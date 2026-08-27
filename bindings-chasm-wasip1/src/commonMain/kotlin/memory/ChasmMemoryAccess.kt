/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import at.released.weh.common.api.InternalWasiEmscriptenHostApi
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import io.github.charlietap.chasm.host.HostMemory
import io.github.charlietap.chasm.host.readNullTerminatedUtf8String
import kotlinx.io.RawSink
import kotlinx.io.RawSource

/** Stateless direct-memory bridge used by generated Chasm callbacks. */
@InternalWasiEmscriptenHostApi
public object ChasmMemoryAccess : MemoryAccess<HostMemory> {
    override fun readI8(memory: HostMemory, addr: WasmPtr): Byte = memory.readI8(addr)
    override fun readI16(memory: HostMemory, addr: WasmPtr): Short = memory.readI16(addr)
    override fun readI32(memory: HostMemory, addr: WasmPtr): Int = memory.readI32(addr)
    override fun readI64(memory: HostMemory, addr: WasmPtr): Long = memory.readI64(addr)
    override fun read(memory: HostMemory, buffer: ByteArray, addr: WasmPtr, bytesToRead: Int) {
        memory.read(buffer, addr, bytesToRead)
    }
    override fun readNullTerminatedString(memory: HostMemory, addr: WasmPtr): String =
        memory.readNullTerminatedUtf8String(addr)
    override fun source(memory: HostMemory, fromAddr: WasmPtr, toAddrExclusive: WasmPtr): RawSource =
        ChasmMemoryRawSource(memory, fromAddr, toAddrExclusive)
    override fun writeI8(memory: HostMemory, addr: WasmPtr, data: Byte): Unit = memory.writeI8(addr, data)
    override fun writeI16(memory: HostMemory, addr: WasmPtr, data: Short): Unit = memory.writeI16(addr, data)
    override fun writeI32(memory: HostMemory, addr: WasmPtr, data: Int): Unit = memory.writeI32(addr, data)
    override fun writeI64(memory: HostMemory, addr: WasmPtr, data: Long): Unit = memory.writeI64(addr, data)
    override fun write(
        memory: HostMemory,
        addr: WasmPtr,
        buffer: ByteArray,
        bufferOffset: Int,
        bytesToWrite: Int,
    ) {
        memory.write(addr, buffer, bufferOffset, bytesToWrite)
    }
    override fun sink(memory: HostMemory, fromAddr: WasmPtr, toAddrExclusive: WasmPtr): RawSink =
        ChasmMemoryRawSink(memory, fromAddr, toAddrExclusive)
}
