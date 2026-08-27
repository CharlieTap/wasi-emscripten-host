/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import io.github.charlietap.chasm.host.HostMemory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChasmMemoryAccessTest {
    @Test
    fun `scalar access preserves little endian bit patterns`() {
        val memory = ByteArrayHostMemory(ByteArray(32))

        ChasmMemoryAccess.writeI8(memory, 0, 0x80.toByte())
        ChasmMemoryAccess.writeI16(memory, 1, 0xFEDC.toShort())
        ChasmMemoryAccess.writeI32(memory, 3, 0x89ABCDEF.toInt())
        ChasmMemoryAccess.writeI64(memory, 7, 0xFEDCBA9876543210UL.toLong())

        assertEquals(0x80.toByte(), ChasmMemoryAccess.readI8(memory, 0))
        assertEquals(0xFEDC.toShort(), ChasmMemoryAccess.readI16(memory, 1))
        assertEquals(0x89ABCDEF.toInt(), ChasmMemoryAccess.readI32(memory, 3))
        assertEquals(0xFEDCBA9876543210UL.toLong(), ChasmMemoryAccess.readI64(memory, 7))
    }

    @Test
    fun `bulk access honors buffer offsets without intermediate copies`() {
        val backing = ByteArray(16)
        val memory = ByteArrayHostMemory(backing)

        ChasmMemoryAccess.write(memory, 4, byteArrayOf(9, 8, 7, 6, 5), bufferOffset = 1, bytesToWrite = 3)
        val destination = ByteArray(5) { -1 }
        memory.read(destination, memoryPointer = 4, bytesToRead = 3, bufferPointer = 1)

        assertContentEquals(byteArrayOf(0, 8, 7, 6, 0), backing.copyOfRange(3, 8))
        assertContentEquals(byteArrayOf(-1, 8, 7, 6, -1), destination)
    }

    @Test
    fun `null terminated strings use the HostMemory optimized scan`() {
        val backing = ByteArray(32)
        "eight-byte-boundary".encodeToByteArray().copyInto(backing, destinationOffset = 3)
        val memory = ByteArrayHostMemory(backing)

        assertEquals("eight-byte-boundary", ChasmMemoryAccess.readNullTerminatedString(memory, 3))
    }

    @Test
    fun `iovec descriptor arithmetic uses unsigned wasm32 pointers and rejects overflow`() {
        assertEquals(0x8000_0008u.toInt(), iovecDescriptorPointer(0x8000_0000u.toInt(), 1))
        assertEquals(0xFFFF_FFFCu.toInt(), checkedPointerAdd(0xFFFF_FFF8u.toInt(), 4))
        assertFailsWith<IllegalStateException> { checkedPointerAdd(0xFFFF_FFFCu.toInt(), 4) }
        assertFailsWith<IllegalStateException> { iovecDescriptorPointer(0xFFFF_FFF8u.toInt(), 1) }
    }
}

private class ByteArrayHostMemory(
    private val bytes: ByteArray,
) : HostMemory {
    override val byteSize: Int get() = bytes.size

    override fun readI8(memoryPointer: Int): Byte = bytes[memoryPointer]
    override fun readI16(memoryPointer: Int): Short = readInteger(memoryPointer, Short.SIZE_BYTES).toShort()
    override fun readI32(memoryPointer: Int): Int = readInteger(memoryPointer, Int.SIZE_BYTES).toInt()
    override fun readI64(memoryPointer: Int): Long = readInteger(memoryPointer, Long.SIZE_BYTES)
    override fun readF32(memoryPointer: Int): Float = Float.fromBits(readI32(memoryPointer))
    override fun readF64(memoryPointer: Int): Double = Double.fromBits(readI64(memoryPointer))
    override fun read(buffer: ByteArray, memoryPointer: Int, bytesToRead: Int, bufferPointer: Int): ByteArray =
        buffer.also {
            bytes.copyInto(it, bufferPointer, memoryPointer, memoryPointer + bytesToRead)
        }

    override fun writeI8(memoryPointer: Int, value: Byte) {
        bytes[memoryPointer] = value
    }
    override fun writeI16(memoryPointer: Int, value: Short): Unit = writeInteger(memoryPointer, value.toLong(), 2)
    override fun writeI32(memoryPointer: Int, value: Int): Unit = writeInteger(memoryPointer, value.toLong(), 4)
    override fun writeI64(memoryPointer: Int, value: Long): Unit = writeInteger(memoryPointer, value, 8)
    override fun writeF32(memoryPointer: Int, value: Float): Unit = writeI32(memoryPointer, value.toRawBits())
    override fun writeF64(memoryPointer: Int, value: Double): Unit = writeI64(memoryPointer, value.toRawBits())
    override fun write(memoryPointer: Int, buffer: ByteArray, bufferPointer: Int, bytesToWrite: Int) {
        buffer.copyInto(bytes, memoryPointer, bufferPointer, bufferPointer + bytesToWrite)
    }
    override fun fill(memoryPointer: Int, value: Byte, bytesToFill: Int) {
        bytes.fill(value, memoryPointer, memoryPointer + bytesToFill)
    }
    override fun copy(sourcePointer: Int, destinationPointer: Int, bytesToCopy: Int, source: HostMemory) {
        val buffer = ByteArray(bytesToCopy)
        source.read(buffer, sourcePointer, bytesToCopy)
        write(destinationPointer, buffer)
    }
    override fun move(sourcePointer: Int, destinationPointer: Int, bytesToMove: Int, source: HostMemory) =
        copy(sourcePointer, destinationPointer, bytesToMove, source)

    private fun readInteger(pointer: Int, size: Int): Long {
        var result = 0L
        repeat(size) { offset ->
            result = result or ((bytes[pointer + offset].toLong() and 0xFFL) shl (offset * 8))
        }
        return result
    }

    private fun writeInteger(pointer: Int, value: Long, size: Int) {
        repeat(size) { offset -> bytes[pointer + offset] = (value ushr (offset * 8)).toByte() }
    }
}
