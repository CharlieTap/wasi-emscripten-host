/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.emcripten.runtime.function

import at.released.weh.emcripten.runtime.EmscriptenHostFunction.TZSET_JS
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.write
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeI8

public class TzsetJsFunctionHandle(
    host: EmbedderHost,
) : EmscriptenHostFunctionHandle(TZSET_JS, host) {
    public fun <M> execute(
        memory: M,
        @IntWasmPtr(Int::class) timezone: WasmPtr,
        @IntWasmPtr(Int::class) daylight: WasmPtr,
        @IntWasmPtr(Byte::class) stdName: WasmPtr,
        @IntWasmPtr(Byte::class) dstName: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Unit = with(memoryAccess) {
        val tzInfo = host.timeZoneInfoProvider.getTimeZoneInfo()
        memory.writeI32(timezone, tzInfo.timeZone.toInt())
        memory.writeI32(daylight, tzInfo.daylight)

        memory.writeTruncatedString(stdName, tzInfo.stdName)
        memory.writeTruncatedString(dstName, tzInfo.dstName)
    }

    context(_: MemoryAccess<M>)
    private fun <M> M.writeTruncatedString(address: WasmPtr, value: String) {
        val encoded = value.encodeToByteArray()
        val length = minOf(encoded.size, TZ_NAME_MAX_SIZE - 1)
        write(address, encoded, bytesToWrite = length)
        writeI8(address + length, 0)
    }

    private companion object {
        private const val TZ_NAME_MAX_SIZE = 17
    }
}
