/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryRawSink
import io.github.charlietap.chasm.host.HostMemory
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

internal class ChasmMemoryRawSink(
    private val memory: HostMemory,
    @IntWasmPtr baseAddr: WasmPtr,
    @IntWasmPtr toAddrExclusive: WasmPtr,
) : MemoryRawSink(baseAddr, toAddrExclusive) {
    override fun writeBytesToMemory(source: Buffer, toAddr: WasmPtr, byteCount: Long) {
        val bytes = source.readByteArray(byteCount.toInt())
        memory.write(toAddr, bytes)
    }
}
