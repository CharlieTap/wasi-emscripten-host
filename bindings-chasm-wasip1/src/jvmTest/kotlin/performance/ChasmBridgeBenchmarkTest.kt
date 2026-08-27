/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.performance

import arrow.core.Either
import arrow.core.getOrElse
import at.released.weh.bindings.chasm.memory.ChasmMemoryAccess
import at.released.weh.bindings.chasm.memory.ChasmWasiMemoryReader
import at.released.weh.bindings.chasm.memory.ChasmWasiMemoryWriter
import at.released.weh.bindings.chasm.module.wasi.ChasmWasiPreview1Functions
import at.released.weh.common.api.InternalWasiEmscriptenHostApi
import at.released.weh.filesystem.FileSystem
import at.released.weh.filesystem.fdresource.nio.isInAppendMode
import at.released.weh.filesystem.fdresource.nio.readCatching
import at.released.weh.filesystem.fdresource.nio.writeCatching
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.nio.NioFileSystem
import at.released.weh.filesystem.nio.op.RunWithChannelFd
import at.released.weh.filesystem.op.opencreate.Open
import at.released.weh.filesystem.op.opencreate.OpenFileFlag.O_RDWR
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy.CurrentPosition
import at.released.weh.filesystem.path.virtual.VirtualPath
import at.released.weh.host.EmbedderHostBuilder
import at.released.weh.wasi.preview1.function.SchedYieldFunctionHandle
import com.sun.management.ThreadMXBean
import io.github.charlietap.chasm.host.HostExceptions
import io.github.charlietap.chasm.host.HostExterns
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.HostGc
import io.github.charlietap.chasm.host.HostGlobal
import io.github.charlietap.chasm.host.HostMemory
import io.github.charlietap.chasm.host.HostModuleInstance
import io.github.charlietap.chasm.host.HostReference
import io.github.charlietap.chasm.host.HostReferences
import io.github.charlietap.chasm.host.HostResources
import io.github.charlietap.chasm.host.HostTable
import io.github.charlietap.chasm.host.HostTag
import io.github.charlietap.chasm.host.JvmHostMemory
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.host.UnsafeHostApi
import io.github.charlietap.chasm.host.writeI32
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.file.Files
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChasmBridgeBenchmarkTest {
    private val host = EmbedderHostBuilder().build()
    private val semanticHandle = SchedYieldFunctionHandle(host)
    private val raw = HostFunction { _, results ->
        results.writeI32(0, semanticHandle.execute().code)
    }
    private val bridge = ChasmWasiPreview1Functions(host, ModuleIndex.MemoryIndex(0)).schedYield

    @Test
    fun `generated numeric bridge stays allocation free and within raw callback noise`() {
        invokeRepeated(raw, WARMUP_ITERATIONS)
        invokeRepeated(bridge, WARMUP_ITERATIONS)

        val rawAllocated = allocatedBytes { invokeRepeated(raw, ALLOCATION_ITERATIONS) }
        val bridgeAllocated = allocatedBytes { invokeRepeated(bridge, ALLOCATION_ITERATIONS) }
        val rawSamples = LongArray(TIMING_ROUNDS)
        val bridgeSamples = LongArray(TIMING_ROUNDS)
        repeat(TIMING_ROUNDS) { round ->
            if (round % 2 == 0) {
                rawSamples[round] = timed { invokeRepeated(raw, TIMING_ITERATIONS) }
                bridgeSamples[round] = timed { invokeRepeated(bridge, TIMING_ITERATIONS) }
            } else {
                bridgeSamples[round] = timed { invokeRepeated(bridge, TIMING_ITERATIONS) }
                rawSamples[round] = timed { invokeRepeated(raw, TIMING_ITERATIONS) }
            }
        }
        val rawMedian = rawSamples.sorted()[TIMING_ROUNDS / 2]
        val bridgeMedian = bridgeSamples.sorted()[TIMING_ROUNDS / 2]
        val delta = abs(bridgeMedian - rawMedian).toDouble() / rawMedian

        println(
            "CHASM_BRIDGE_BENCHMARK raw_ns=$rawMedian bridge_ns=$bridgeMedian " +
                    "delta_percent=${delta * 100.0} raw_allocated=$rawAllocated bridge_allocated=$bridgeAllocated",
        )
        assertEquals(0, rawAllocated)
        assertEquals(0, bridgeAllocated)
        if (System.getenv(ENFORCE_BENCHMARK_ENV) == "true") {
            assertTrue(delta <= MAX_NOISE_RATIO, "Generated bridge differed from raw callback by ${delta * 100.0}%")
        }
    }

    @Test
    fun `typed memory access delegates without allocation`() {
        repeat(WARMUP_ITERATIONS) { iteration ->
            BenchmarkMemory.writeI32(0, iteration)
            ChasmMemoryAccess.writeI32(BenchmarkMemory, 0, iteration)
        }

        val rawAllocated = allocatedBytes {
            repeat(ALLOCATION_ITERATIONS) { iteration -> BenchmarkMemory.writeI32(0, iteration) }
        }
        val bridgeAllocated = allocatedBytes {
            repeat(ALLOCATION_ITERATIONS) { iteration -> ChasmMemoryAccess.writeI32(BenchmarkMemory, 0, iteration) }
        }
        val rawTime = timed {
            repeat(TIMING_ITERATIONS) { iteration -> BenchmarkMemory.writeI32(0, iteration) }
        }
        val bridgeTime = timed {
            repeat(TIMING_ITERATIONS) { iteration -> ChasmMemoryAccess.writeI32(BenchmarkMemory, 0, iteration) }
        }

        println(
            "CHASM_MEMORY_BENCHMARK raw_ns=$rawTime bridge_ns=$bridgeTime " +
                    "raw_allocated=$rawAllocated bridge_allocated=$bridgeAllocated",
        )
        assertEquals(0, rawAllocated)
        assertEquals(0, bridgeAllocated)
        assertEquals(TIMING_ITERATIONS - 1, BenchmarkMemory.readI32(0))
    }

    @OptIn(InternalWasiEmscriptenHostApi::class, UnsafeHostApi::class)
    @Test
    fun `direct fd IO remains close to the already borrowed NIO path`() {
        val directory = Files.createTempDirectory("weh-chasm-benchmark-")
        val file = Files.createFile(directory.resolve("output.bin"))
        val fileSystem = FileSystem(NioFileSystem) { unrestricted = true }
        try {
            val path = VirtualPath.create(file.toString()).getOrElse { error(it.toString()) }
            val fd = fileSystem.execute(Open, Open(path, openFlags = O_RDWR, fdFlags = 0))
                .getOrElse { error(it.toString()) }
            val memory = BenchmarkJvmMemory(FD_PAYLOAD_POINTER + FD_PAYLOAD_SIZE)
            memory.writeI32(FD_IOVEC_POINTER, FD_PAYLOAD_POINTER)
            memory.writeI32(FD_IOVEC_POINTER + Int.SIZE_BYTES, FD_PAYLOAD_SIZE)
            val rawBuffer = memory.unsafeBorrowByteBuffer().duplicate().apply {
                position(FD_PAYLOAD_POINTER)
                limit(FD_PAYLOAD_POINTER + FD_PAYLOAD_SIZE)
            }.slice()
            val writer = ChasmWasiMemoryWriter(fileSystem)
            val reader = ChasmWasiMemoryReader(fileSystem)

            repeat(FD_WARMUP_ITERATIONS) {
                rawFdWrite(fileSystem, fd, rawBuffer)
                directFdWrite(writer, memory, fd)
            }
            val rawSamples = LongArray(TIMING_ROUNDS)
            val directSamples = LongArray(TIMING_ROUNDS)
            repeat(TIMING_ROUNDS) { round ->
                if (round % 2 == 0) {
                    resetFdPosition(fileSystem, fd)
                    rawSamples[round] = timed { repeat(FD_TIMING_ITERATIONS) { rawFdWrite(fileSystem, fd, rawBuffer) } }
                    resetFdPosition(fileSystem, fd)
                    directSamples[round] = timed { repeat(FD_TIMING_ITERATIONS) { directFdWrite(writer, memory, fd) } }
                } else {
                    resetFdPosition(fileSystem, fd)
                    directSamples[round] = timed { repeat(FD_TIMING_ITERATIONS) { directFdWrite(writer, memory, fd) } }
                    resetFdPosition(fileSystem, fd)
                    rawSamples[round] = timed { repeat(FD_TIMING_ITERATIONS) { rawFdWrite(fileSystem, fd, rawBuffer) } }
                }
            }
            val rawMedian = rawSamples.sorted()[TIMING_ROUNDS / 2]
            val directMedian = directSamples.sorted()[TIMING_ROUNDS / 2]
            val delta = abs(directMedian - rawMedian).toDouble() / rawMedian

            println(
                "CHASM_FD_WRITE_BENCHMARK payload_bytes=$FD_PAYLOAD_SIZE iterations=$FD_TIMING_ITERATIONS " +
                        "raw_ns=$rawMedian direct_ns=$directMedian delta_percent=${delta * 100.0}",
            )
            if (System.getenv(ENFORCE_BENCHMARK_ENV) == "true") {
                assertTrue(delta <= MAX_NOISE_RATIO, "Direct fd_write differed from borrowed NIO by ${delta * 100.0}%")
            }

            resetFdPosition(fileSystem, fd)
            repeat(FD_WARMUP_ITERATIONS) {
                rawFdRead(fileSystem, fd, rawBuffer)
                directFdRead(reader, memory, fd)
            }
            val rawReadSamples = LongArray(TIMING_ROUNDS)
            val directReadSamples = LongArray(TIMING_ROUNDS)
            repeat(TIMING_ROUNDS) { round ->
                if (round % 2 == 0) {
                    resetFdPosition(fileSystem, fd)
                    rawReadSamples[round] = timed {
                        repeat(FD_TIMING_ITERATIONS) { rawFdRead(fileSystem, fd, rawBuffer) }
                    }
                    resetFdPosition(fileSystem, fd)
                    directReadSamples[round] = timed {
                        repeat(FD_TIMING_ITERATIONS) { directFdRead(reader, memory, fd) }
                    }
                } else {
                    resetFdPosition(fileSystem, fd)
                    directReadSamples[round] = timed {
                        repeat(FD_TIMING_ITERATIONS) { directFdRead(reader, memory, fd) }
                    }
                    resetFdPosition(fileSystem, fd)
                    rawReadSamples[round] = timed {
                        repeat(FD_TIMING_ITERATIONS) { rawFdRead(fileSystem, fd, rawBuffer) }
                    }
                }
            }
            val rawReadMedian = rawReadSamples.sorted()[TIMING_ROUNDS / 2]
            val directReadMedian = directReadSamples.sorted()[TIMING_ROUNDS / 2]
            val readDelta = abs(directReadMedian - rawReadMedian).toDouble() / rawReadMedian

            println(
                "CHASM_FD_READ_BENCHMARK payload_bytes=$FD_PAYLOAD_SIZE iterations=$FD_TIMING_ITERATIONS " +
                        "raw_ns=$rawReadMedian direct_ns=$directReadMedian delta_percent=${readDelta * 100.0}",
            )
            if (System.getenv(ENFORCE_BENCHMARK_ENV) == "true") {
                assertTrue(
                    readDelta <= MAX_NOISE_RATIO,
                    "Direct fd_read differed from borrowed NIO by ${readDelta * 100.0}%",
                )
            }
        } finally {
            fileSystem.close()
            Files.deleteIfExists(file)
            Files.deleteIfExists(directory)
        }
    }
}

@OptIn(InternalWasiEmscriptenHostApi::class)
private fun rawFdWrite(fileSystem: FileSystem, fd: FileDescriptor, buffer: ByteBuffer) {
    buffer.clear()
    val request = RunWithChannelFd(
        fd = fd,
        block = { channel ->
            channel.fold(
                ifLeft = { error -> Either.Left(error) },
                ifRight = { nio ->
                    writeCatching {
                        if (nio.isInAppendMode()) nio.channel.position(nio.channel.size())
                        nio.channel.write(buffer).toULong()
                    }
                },
            )
        },
        nonNioResourceFallback = { error("Expected NIO file descriptor") },
    )
    val written = fileSystem.execute(RunWithChannelFd.key(), request).getOrElse { error(it.toString()) }
    check(written == FD_PAYLOAD_SIZE.toULong())
}

@OptIn(InternalWasiEmscriptenHostApi::class)
private fun rawFdRead(fileSystem: FileSystem, fd: FileDescriptor, buffer: ByteBuffer) {
    buffer.clear()
    val request = RunWithChannelFd(
        fd = fd,
        block = { channel ->
            channel.fold(
                ifLeft = { error -> Either.Left(error) },
                ifRight = { nio -> readCatching { nio.channel.read(buffer) }.map(Int::toULong) },
            )
        },
        nonNioResourceFallback = { error("Expected NIO file descriptor") },
    )
    val read = fileSystem.execute(RunWithChannelFd.key(), request).getOrElse { error(it.toString()) }
    check(read == FD_PAYLOAD_SIZE.toULong())
}

@OptIn(InternalWasiEmscriptenHostApi::class)
private fun resetFdPosition(fileSystem: FileSystem, fd: FileDescriptor) {
    val request = RunWithChannelFd(
        fd = fd,
        block = { channel ->
            channel.fold(
                ifLeft = { error -> Either.Left(error) },
                ifRight = { nio -> Either.Right(nio.channel.position(0)).map { Unit } },
            )
        },
        nonNioResourceFallback = { error("Expected NIO file descriptor") },
    )
    fileSystem.execute(RunWithChannelFd.key(), request).getOrElse { error(it.toString()) }
}

private fun directFdWrite(writer: ChasmWasiMemoryWriter, memory: HostMemory, fd: FileDescriptor) {
    val written = writer.write(
        memory = memory,
        fd = fd,
        strategy = CurrentPosition,
        ciovecsPointer = FD_IOVEC_POINTER,
        ciovecCount = 1,
        memoryAccess = ChasmMemoryAccess,
    ).getOrElse { error(it.toString()) }
    check(written == FD_PAYLOAD_SIZE.toULong())
}

private fun directFdRead(reader: ChasmWasiMemoryReader, memory: HostMemory, fd: FileDescriptor) {
    val read = reader.read(
        memory = memory,
        fd = fd,
        strategy = CurrentPosition,
        iovecsPointer = FD_IOVEC_POINTER,
        iovecCount = 1,
        memoryAccess = ChasmMemoryAccess,
    ).getOrElse { error(it.toString()) }
    check(read == FD_PAYLOAD_SIZE.toULong())
}

private fun invokeRepeated(function: HostFunction, iterations: Int) {
    context(benchmarkStack, BenchmarkModule, BenchmarkResources) {
        repeat(iterations) { function.invoke(0, 0) }
    }
    check(benchmarkStack[0] == 0L)
}

private fun timed(block: () -> Unit): Long {
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}

private fun allocatedBytes(block: () -> Unit): Long {
    val bean = ManagementFactory.getThreadMXBean() as ThreadMXBean
    if (!bean.isThreadAllocatedMemoryEnabled) bean.isThreadAllocatedMemoryEnabled = true
    @Suppress("DEPRECATION")
    val thread = Thread.currentThread().id
    val before = bean.getThreadAllocatedBytes(thread)
    block()
    return bean.getThreadAllocatedBytes(thread) - before
}

private object BenchmarkModule : HostModuleInstance
private val benchmarkStack = LongArray(1)

private object BenchmarkMemory : HostMemory {
    private var value: Long = 0
    override val byteSize: Int = Long.SIZE_BYTES
    override fun readI8(memoryPointer: Int): Byte = (value ushr (memoryPointer * 8)).toByte()
    override fun readI16(memoryPointer: Int): Short = readUnsigned(memoryPointer, Short.SIZE_BYTES).toShort()
    override fun readI32(memoryPointer: Int): Int = readUnsigned(memoryPointer, Int.SIZE_BYTES).toInt()
    override fun readI64(memoryPointer: Int): Long = readUnsigned(memoryPointer, Long.SIZE_BYTES)
    override fun readF32(memoryPointer: Int): Float = Float.fromBits(readI32(memoryPointer))
    override fun readF64(memoryPointer: Int): Double = Double.fromBits(readI64(memoryPointer))
    override fun read(buffer: ByteArray, memoryPointer: Int, bytesToRead: Int, bufferPointer: Int): ByteArray =
        buffer.also { target ->
            repeat(bytesToRead) { offset -> target[bufferPointer + offset] = readI8(memoryPointer + offset) }
        }
    override fun writeI8(memoryPointer: Int, value: Byte): Unit = writeUnsigned(memoryPointer, value.toLong(), 1)
    override fun writeI16(memoryPointer: Int, value: Short): Unit = writeUnsigned(memoryPointer, value.toLong(), 2)
    override fun writeI32(memoryPointer: Int, value: Int): Unit = writeUnsigned(memoryPointer, value.toLong(), 4)
    override fun writeI64(memoryPointer: Int, value: Long): Unit = writeUnsigned(memoryPointer, value, 8)
    override fun writeF32(memoryPointer: Int, value: Float): Unit = writeI32(memoryPointer, value.toRawBits())
    override fun writeF64(memoryPointer: Int, value: Double): Unit = writeI64(memoryPointer, value.toRawBits())
    override fun write(memoryPointer: Int, buffer: ByteArray, bufferPointer: Int, bytesToWrite: Int) {
        repeat(bytesToWrite) { offset -> writeI8(memoryPointer + offset, buffer[bufferPointer + offset]) }
    }
    override fun fill(memoryPointer: Int, value: Byte, bytesToFill: Int) {
        repeat(bytesToFill) { offset -> writeI8(memoryPointer + offset, value) }
    }
    override fun copy(sourcePointer: Int, destinationPointer: Int, bytesToCopy: Int, source: HostMemory) {
        repeat(bytesToCopy) { offset -> writeI8(destinationPointer + offset, source.readI8(sourcePointer + offset)) }
    }
    override fun move(sourcePointer: Int, destinationPointer: Int, bytesToMove: Int, source: HostMemory) {
        val snapshot = ByteArray(bytesToMove)
        source.read(snapshot, sourcePointer, bytesToMove)
        write(destinationPointer, snapshot, 0, bytesToMove)
    }

    private fun readUnsigned(pointer: Int, bytes: Int): Long {
        val bits = bytes * 8
        val mask = if (bits == Long.SIZE_BITS) -1L else (1L shl bits) - 1L
        return (value ushr (pointer * 8)) and mask
    }

    private fun writeUnsigned(pointer: Int, newValue: Long, bytes: Int) {
        val bits = bytes * 8
        val unshiftedMask = if (bits == Long.SIZE_BITS) -1L else (1L shl bits) - 1L
        val shift = pointer * 8
        val mask = unshiftedMask shl shift
        value = (value and mask.inv()) or ((newValue and unshiftedMask) shl shift)
    }
}

@Suppress("TooManyFunctions")
private class BenchmarkJvmMemory(size: Int) : JvmHostMemory {
    private val buffer = ByteBuffer.allocateDirect(size).order(LITTLE_ENDIAN)
    override val byteSize: Int get() = buffer.capacity()
    override fun readI8(memoryPointer: Int): Byte = buffer.get(memoryPointer)
    override fun readI16(memoryPointer: Int): Short = buffer.getShort(memoryPointer)
    override fun readI32(memoryPointer: Int): Int = buffer.getInt(memoryPointer)
    override fun readI64(memoryPointer: Int): Long = buffer.getLong(memoryPointer)
    override fun readF32(memoryPointer: Int): Float = buffer.getFloat(memoryPointer)
    override fun readF64(memoryPointer: Int): Double = buffer.getDouble(memoryPointer)
    override fun read(buffer: ByteArray, memoryPointer: Int, bytesToRead: Int, bufferPointer: Int): ByteArray =
        buffer.also { target ->
            this.buffer.duplicate().position(memoryPointer).get(target, bufferPointer, bytesToRead)
        }
    override fun writeI8(memoryPointer: Int, value: Byte) { buffer.put(memoryPointer, value) }
    override fun writeI16(memoryPointer: Int, value: Short) { buffer.putShort(memoryPointer, value) }
    override fun writeI32(memoryPointer: Int, value: Int) { buffer.putInt(memoryPointer, value) }
    override fun writeI64(memoryPointer: Int, value: Long) { buffer.putLong(memoryPointer, value) }
    override fun writeF32(memoryPointer: Int, value: Float) { buffer.putFloat(memoryPointer, value) }
    override fun writeF64(memoryPointer: Int, value: Double) { buffer.putDouble(memoryPointer, value) }
    override fun write(memoryPointer: Int, buffer: ByteArray, bufferPointer: Int, bytesToWrite: Int) {
        this.buffer.duplicate().position(memoryPointer).put(buffer, bufferPointer, bytesToWrite)
    }
    override fun fill(memoryPointer: Int, value: Byte, bytesToFill: Int) {
        repeat(bytesToFill) { offset -> buffer.put(memoryPointer + offset, value) }
    }
    override fun copy(sourcePointer: Int, destinationPointer: Int, bytesToCopy: Int, source: HostMemory) {
        val snapshot = ByteArray(bytesToCopy)
        source.read(snapshot, sourcePointer, bytesToCopy)
        write(destinationPointer, snapshot, 0, bytesToCopy)
    }
    override fun move(sourcePointer: Int, destinationPointer: Int, bytesToMove: Int, source: HostMemory) {
        copy(sourcePointer, destinationPointer, bytesToMove, source)
    }

    @OptIn(UnsafeHostApi::class)
    override fun unsafeBorrowByteBuffer(): ByteBuffer = buffer
}

private object BenchmarkResources : HostResources {
    override val references: HostReferences get() = error("unused")
    override val gc: HostGc get() = error("unused")
    override val externs: HostExterns get() = error("unused")
    override val exceptions: HostExceptions get() = error("unused")
    override fun memory(module: HostModuleInstance, index: ModuleIndex.MemoryIndex): HostMemory = BenchmarkMemory
    override fun growMemory(module: HostModuleInstance, index: ModuleIndex.MemoryIndex, pagesToAdd: Int): Int =
        error("unused")
    override fun table(module: HostModuleInstance, index: ModuleIndex.TableIndex): HostTable = error("unused")
    override fun growTable(
        module: HostModuleInstance,
        index: ModuleIndex.TableIndex,
        elementsToAdd: Int,
        value: HostReference,
    ): Int = error("unused")
    override fun global(module: HostModuleInstance, index: ModuleIndex.GlobalIndex): HostGlobal = error("unused")
    override fun tag(module: HostModuleInstance, index: ModuleIndex.TagIndex): HostTag = error("unused")
}

private const val WARMUP_ITERATIONS = 250_000
private const val ALLOCATION_ITERATIONS = 250_000
private const val TIMING_ITERATIONS = 500_000
private const val TIMING_ROUNDS = 7
private const val MAX_NOISE_RATIO = 0.05
private const val ENFORCE_BENCHMARK_ENV = "WEH_CHASM_BENCHMARK_ENFORCE"
private const val FD_IOVEC_POINTER = 0
private const val FD_PAYLOAD_POINTER = 16
private const val FD_PAYLOAD_SIZE = 1024 * 1024
private const val FD_WARMUP_ITERATIONS = 32
private const val FD_TIMING_ITERATIONS = 256
