/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import io.github.charlietap.chasm.host.HostMemory
import io.github.charlietap.chasm.host.UnsafeHostApi
import io.github.charlietap.chasm.host.ByteArrayHostMemory as ChasmByteArrayHostMemory

@OptIn(UnsafeHostApi::class)
internal class ByteArrayHostMemory(
    var bytes: ByteArray,
    override var byteSize: Int = bytes.size,
) : ChasmByteArrayHostMemory {
    @UnsafeHostApi
    override fun unsafeBorrowByteArray(): ByteArray = bytes

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
