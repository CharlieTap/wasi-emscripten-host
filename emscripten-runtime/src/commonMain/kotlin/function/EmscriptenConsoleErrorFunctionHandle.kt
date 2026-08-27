/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.emcripten.runtime.function

import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_CONSOLE_ERROR
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.readNullTerminatedString

public class EmscriptenConsoleErrorFunctionHandle(
    host: EmbedderHost,
) : EmscriptenHostFunctionHandle(EMSCRIPTEN_CONSOLE_ERROR, host) {
    public fun <M> execute(
        memory: M,
        @IntWasmPtr(Byte::class) messagePtr: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Unit = with(memoryAccess) {
        val message = memory.readNullTerminatedString(messagePtr)
        logger.e { message }
    }
}
