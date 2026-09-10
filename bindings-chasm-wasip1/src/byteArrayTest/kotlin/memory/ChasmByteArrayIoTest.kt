/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import arrow.core.Either
import at.released.weh.filesystem.FileSystem
import at.released.weh.filesystem.error.FileSystemOperationError
import at.released.weh.filesystem.op.FileSystemOperation
import at.released.weh.filesystem.op.readwrite.ReadFd
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy.CurrentPosition
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy.Position
import at.released.weh.filesystem.op.readwrite.WriteFd
import io.github.charlietap.chasm.host.HostMemory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@Suppress("MagicNumber")
class ChasmByteArrayIoTest {
    @Test
    fun `reader uses the borrowed array for each iovec`() {
        val memory = memoryWithIovecs()
        val fileSystem = RecordingFileSystem(onRead = { request ->
            assertEquals(3, request.fd)
            assertEquals(CurrentPosition, request.strategy)
            assertEquals(listOf(32, 40), request.iovecs.map { it.offset })
            assertEquals(listOf(3, 2), request.iovecs.map { it.length })
            request.iovecs.forEachIndexed { index, buffer ->
                assertSame(memory.bytes, buffer.array)
                buffer.array.fill((index + 1).toByte(), buffer.offset, buffer.offset + buffer.length)
            }
            5UL
        })

        val result = ChasmWasiMemoryReader(fileSystem).read(memory, 3, CurrentPosition, 0, 2, ChasmMemoryAccess)

        assertEquals(Either.Right(5UL), result)
        assertContentEquals(byteArrayOf(1, 1, 1), memory.bytes.copyOfRange(32, 35))
        assertContentEquals(byteArrayOf(2, 2), memory.bytes.copyOfRange(40, 42))
    }

    @Test
    fun `writer uses the borrowed array and preserves positioned IO`() {
        val memory = memoryWithIovecs()
        memory.write(32, byteArrayOf(7, 8, 9))
        memory.write(40, byteArrayOf(10, 11))
        val strategy = Position(17)
        val fileSystem = RecordingFileSystem(onWrite = { request ->
            assertEquals(strategy, request.strategy)
            request.cIovecs.forEach { assertSame(memory.bytes, it.array) }
            assertContentEquals(byteArrayOf(7, 8, 9, 10, 11), request.cIovecs.flatMap {
                it.array.slice(it.offset until it.offset + it.length)
            }.toByteArray())
            5UL
        })

        val result = ChasmWasiMemoryWriter(fileSystem).write(memory, 3, strategy, 0, 2, ChasmMemoryAccess)

        assertEquals(Either.Right(5UL), result)
    }

    @Test
    fun `rejects iovecs outside logical memory when the backing array has spare capacity`() {
        for ((offset, length) in listOf(63 to 2, 64 to 1, -1 to 1, 32 to -1, 32 to Int.MAX_VALUE)) {
            val memory = memoryWithIovecs()
            // The first iovec is valid; the second must prevent the entire operation.
            memory.writeI32(8, offset)
            memory.writeI32(12, length)
            assertRejectedBeforeIo(memory)
        }
    }

    @Test
    fun `accepts an empty iovec at the end of logical memory`() {
        val memory = memoryWithIovecs()
        memory.writeI32(0, memory.byteSize)
        memory.writeI32(4, 0)
        val fileSystem = RecordingFileSystem(onRead = { request ->
            request.iovecs.forEach {
                assertSame(memory.bytes, it.array)
                assertEquals(memory.byteSize, it.offset)
                assertEquals(0, it.length)
            }
            0UL
        })
        val reader = ChasmWasiMemoryReader(fileSystem)

        assertEquals(Either.Right(0UL), reader.read(memory, 3, CurrentPosition, 0, 1, ChasmMemoryAccess))
        assertEquals(Either.Right(0UL), reader.read(memory, 3, CurrentPosition, memory.byteSize, 0, ChasmMemoryAccess))
    }

    @Test
    fun `reacquires the backing array for each operation`() {
        val memory = memoryWithIovecs()
        val fileSystem = RecordingFileSystem(onWrite = { request ->
            request.cIovecs.forEach { assertSame(memory.bytes, it.array) }
            5UL
        })
        val writer = ChasmWasiMemoryWriter(fileSystem)
        writer.write(memory, 3, CurrentPosition, 0, 2, ChasmMemoryAccess)

        memory.bytes = memory.bytes.copyOf(256)
        memory.byteSize = 128
        memory.writeI32(8, 100)
        writer.write(memory, 3, CurrentPosition, 0, 2, ChasmMemoryAccess)

        assertEquals(2, fileSystem.calls)
    }

    @Test
    fun `uses the copy fallback when array access is unavailable`() {
        val backing = memoryWithIovecs()
        val memory = object : HostMemory by backing {}
        val fileSystem = RecordingFileSystem(
            onRead = { request ->
                request.iovecs.forEach {
                    assertNotSame(backing.bytes, it.array)
                    it.array.fill(42)
                }
                5UL
            },
            onWrite = { request ->
                request.cIovecs.forEach {
                    assertNotSame(backing.bytes, it.array)
                    assertContentEquals(ByteArray(it.length) { 42 }, it.array)
                }
                5UL
            },
        )

        assertEquals(
            Either.Right(5UL),
            ChasmWasiMemoryReader(fileSystem).read(memory, 3, CurrentPosition, 0, 2, ChasmMemoryAccess),
        )
        assertEquals(
            Either.Right(5UL),
            ChasmWasiMemoryWriter(fileSystem).write(memory, 3, CurrentPosition, 0, 2, ChasmMemoryAccess),
        )
        assertContentEquals(byteArrayOf(42, 42, 42), backing.bytes.copyOfRange(32, 35))
    }

    private fun assertRejectedBeforeIo(memory: ByteArrayHostMemory) {
        val before = memory.bytes.copyOf()
        val fileSystem = RecordingFileSystem()
        assertFailsWith<IllegalArgumentException> {
            ChasmWasiMemoryReader(fileSystem).read(memory, 3, CurrentPosition, 0, 2, ChasmMemoryAccess)
        }
        assertFailsWith<IllegalArgumentException> {
            ChasmWasiMemoryWriter(fileSystem).write(memory, 3, CurrentPosition, 0, 2, ChasmMemoryAccess)
        }
        assertEquals(0, fileSystem.calls)
        assertContentEquals(before, memory.bytes)
    }

    private fun memoryWithIovecs(): ByteArrayHostMemory = ByteArrayHostMemory(ByteArray(128), 64).apply {
        writeI32(0, 32)
        writeI32(4, 3)
        writeI32(8, 40)
        writeI32(12, 2)
    }
}

private class RecordingFileSystem(
    private val onRead: (ReadFd) -> ULong = { error("Unexpected read") },
    private val onWrite: (WriteFd) -> ULong = { error("Unexpected write") },
) : FileSystem {
    var calls: Int = 0
        private set

    @Suppress("UNCHECKED_CAST")
    override fun <I : Any, E : FileSystemOperationError, R : Any> execute(
        operation: FileSystemOperation<I, E, R>,
        input: I,
    ): Either<E, R> {
        calls++
        val result = when (input) {
            is ReadFd -> onRead(input)
            is WriteFd -> onWrite(input)
            else -> error("Unexpected operation: $operation")
        }
        return Either.Right(result) as Either<E, R>
    }

    override fun isOperationSupported(operation: FileSystemOperation<*, *, *>): Boolean =
        operation == ReadFd || operation == WriteFd

    override fun close() = Unit
}
