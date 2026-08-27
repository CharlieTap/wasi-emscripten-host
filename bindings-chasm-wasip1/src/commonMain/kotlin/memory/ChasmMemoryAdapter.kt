/*
 * Copyright 2024-2025, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber")

package at.released.weh.bindings.chasm.memory

import at.released.weh.common.api.InternalWasiEmscriptenHostApi
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.Memory
import io.github.charlietap.chasm.host.HostMemory
import kotlinx.io.RawSink
import kotlinx.io.RawSource

@InternalWasiEmscriptenHostApi
public class ChasmMemoryAdapter(
    private val memory: HostMemory,
) : Memory {
    override fun readI8(@IntWasmPtr addr: WasmPtr): Byte {
        return memory.readI8(addr)
    }

    override fun readI32(@IntWasmPtr addr: WasmPtr): Int {
        return memory.readI32(addr)
    }

    override fun readI64(@IntWasmPtr addr: WasmPtr): Long {
        return memory.readI64(addr)
    }

    override fun source(@IntWasmPtr fromAddr: WasmPtr, @IntWasmPtr toAddrExclusive: WasmPtr): RawSource {
        return ChasmMemoryRawSource(memory, fromAddr, toAddrExclusive)
    }

    override fun writeI8(@IntWasmPtr addr: WasmPtr, data: Byte) {
        memory.writeI8(addr, data)
    }

    override fun writeI32(@IntWasmPtr addr: WasmPtr, data: Int) {
        memory.writeI32(addr, data)
    }

    override fun writeI64(addr: WasmPtr, data: Long) {
        memory.writeI64(addr, data)
    }

    override fun sink(@IntWasmPtr fromAddr: WasmPtr, @IntWasmPtr toAddrExclusive: WasmPtr): RawSink {
        return ChasmMemoryRawSink(memory, fromAddr, toAddrExclusive)
    }
}
