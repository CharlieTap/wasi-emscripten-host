/*
 * Copyright 2024-2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber")

package at.released.weh.wasm.core.memory

import at.released.weh.common.api.InternalWasiEmscriptenHostApi
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.write

/**
 * Allocation-free bridge between runtime-specific linear memories and shared WebAssembly helpers.
 *
 * Runtime bindings should keep one stateless implementation and pass the callback-scoped memory as [M].
 */
@InternalWasiEmscriptenHostApi
public interface MemoryAccess<M> {
    public fun readI8(memory: M, @IntWasmPtr addr: WasmPtr): Byte
    public fun readI16(memory: M, @IntWasmPtr addr: WasmPtr): Short
    public fun readI32(memory: M, @IntWasmPtr addr: WasmPtr): Int
    public fun readI64(memory: M, @IntWasmPtr addr: WasmPtr): Long
    public fun read(memory: M, buffer: ByteArray, @IntWasmPtr addr: WasmPtr, bytesToRead: Int)
    public fun readNullTerminatedString(memory: M, @IntWasmPtr addr: WasmPtr): String {
        require(addr != 0)
        var length = 0
        while (true) {
            val address = addr.toUInt().toULong() + length.toUInt().toULong()
            check(address <= UInt.MAX_VALUE.toULong()) { "Null-terminated string address overflow" }
            if (readI8(memory, address.toUInt().toInt()) == 0.toByte()) break
            check(length != Int.MAX_VALUE) { "Null-terminated string length overflow" }
            length += 1
        }
        if (length == 0) return ""
        val bytes = ByteArray(length)
        read(memory, bytes, addr, length)
        return bytes.decodeToString()
    }
    public fun source(memory: M, @IntWasmPtr fromAddr: WasmPtr, @IntWasmPtr toAddrExclusive: WasmPtr): RawSource
    public fun writeI8(memory: M, @IntWasmPtr addr: WasmPtr, data: Byte)
    public fun writeI16(memory: M, @IntWasmPtr addr: WasmPtr, data: Short)
    public fun writeI32(memory: M, @IntWasmPtr addr: WasmPtr, data: Int)
    public fun writeI64(memory: M, @IntWasmPtr addr: WasmPtr, data: Long)
    public fun write(
        memory: M,
        @IntWasmPtr addr: WasmPtr,
        buffer: ByteArray,
        bufferOffset: Int = 0,
        bytesToWrite: Int = buffer.size - bufferOffset,
    )
    public fun sink(memory: M, @IntWasmPtr fromAddr: WasmPtr, @IntWasmPtr toAddrExclusive: WasmPtr): RawSink
}

@InternalWasiEmscriptenHostApi
public object DefaultMemoryAccess : MemoryAccess<Memory> {
    override fun readI8(memory: Memory, addr: WasmPtr): Byte = memory.readI8(addr)
    override fun readI16(memory: Memory, addr: WasmPtr): Short =
        ((memory.readI8(addr).toInt() and 0xff) or
                ((memory.readI8(addr + 1).toInt() and 0xff) shl 8)).toShort()
    override fun readI32(memory: Memory, addr: WasmPtr): Int = memory.readI32(addr)
    override fun readI64(memory: Memory, addr: WasmPtr): Long = memory.readI64(addr)
    override fun read(memory: Memory, buffer: ByteArray, addr: WasmPtr, bytesToRead: Int) {
        val bytes = memory.source(addr, addr + bytesToRead).buffered().use { it.readByteArray(bytesToRead) }
        bytes.copyInto(buffer, endIndex = bytesToRead)
    }
    override fun source(memory: Memory, fromAddr: WasmPtr, toAddrExclusive: WasmPtr): RawSource =
        memory.source(fromAddr, toAddrExclusive)
    override fun writeI8(memory: Memory, addr: WasmPtr, data: Byte): Unit = memory.writeI8(addr, data)
    override fun writeI16(memory: Memory, addr: WasmPtr, data: Short) {
        memory.writeI8(addr, data.toByte())
        memory.writeI8(addr + 1, (data.toInt() ushr 8).toByte())
    }
    override fun writeI32(memory: Memory, addr: WasmPtr, data: Int): Unit = memory.writeI32(addr, data)
    override fun writeI64(memory: Memory, addr: WasmPtr, data: Long): Unit = memory.writeI64(addr, data)
    override fun write(
        memory: Memory,
        addr: WasmPtr,
        buffer: ByteArray,
        bufferOffset: Int,
        bytesToWrite: Int,
    ) {
        memory.sink(addr, addr + bytesToWrite).buffered().use { sink ->
            sink.write(buffer, bufferOffset, bufferOffset + bytesToWrite)
        }
    }
    override fun sink(memory: Memory, fromAddr: WasmPtr, toAddrExclusive: WasmPtr): RawSink =
        memory.sink(fromAddr, toAddrExclusive)
}

@InternalWasiEmscriptenHostApi
@Suppress("UNCHECKED_CAST")
public fun <M> M.defaultMemoryAccess(): MemoryAccess<M> {
    require(this is Memory) { "A runtime-specific MemoryAccess must be supplied for non-WEH memory types" }
    return DefaultMemoryAccess as MemoryAccess<M>
}

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.readI8(@IntWasmPtr addr: WasmPtr): Byte = access.readI8(this, addr)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.readI16(@IntWasmPtr addr: WasmPtr): Short = access.readI16(this, addr)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.readI32(@IntWasmPtr addr: WasmPtr): Int = access.readI32(this, addr)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.readI64(@IntWasmPtr addr: WasmPtr): Long = access.readI64(this, addr)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.read(buffer: ByteArray, @IntWasmPtr addr: WasmPtr, bytesToRead: Int): Unit =
    access.read(this, buffer, addr, bytesToRead)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.source(@IntWasmPtr fromAddr: WasmPtr, @IntWasmPtr toAddrExclusive: WasmPtr): RawSource =
    access.source(this, fromAddr, toAddrExclusive)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.writeI8(@IntWasmPtr addr: WasmPtr, data: Byte): Unit = access.writeI8(this, addr, data)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.writeI16(@IntWasmPtr addr: WasmPtr, data: Short): Unit = access.writeI16(this, addr, data)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.writeI32(@IntWasmPtr addr: WasmPtr, data: Int): Unit = access.writeI32(this, addr, data)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.writeI64(@IntWasmPtr addr: WasmPtr, data: Long): Unit = access.writeI64(this, addr, data)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.write(
    @IntWasmPtr addr: WasmPtr,
    buffer: ByteArray,
    bufferOffset: Int = 0,
    bytesToWrite: Int = buffer.size - bufferOffset,
): Unit = access.write(this, addr, buffer, bufferOffset, bytesToWrite)

context(access: MemoryAccess<M>)
@InternalWasiEmscriptenHostApi
public fun <M> M.sink(@IntWasmPtr fromAddr: WasmPtr, @IntWasmPtr toAddrExclusive: WasmPtr): RawSink =
    access.sink(this, fromAddr, toAddrExclusive)
