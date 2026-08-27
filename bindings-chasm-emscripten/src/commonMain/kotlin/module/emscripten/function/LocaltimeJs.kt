/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.module.emscripten.function

import at.released.weh.bindings.chasm.memory.ChasmMemoryAccess
import at.released.weh.bindings.chasm.module.emscripten.HostFunctionProvider
import at.released.weh.emcripten.runtime.function.LocaltimeJsFunctionHandle
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.host.readI32
import io.github.charlietap.chasm.host.readI64
import io.github.charlietap.chasm.host.withMemory

internal class LocaltimeJs(
    host: EmbedderHost,
    private val memoryIndex: ModuleIndex.MemoryIndex,
) : HostFunctionProvider {
    private val handle = LocaltimeJsFunctionHandle(host)
    override val function: HostFunction = HostFunction { parameters, _ ->
        val timeSeconds = parameters.readI64(0)
        val timePtr = parameters.readI32(1)
        withMemory(memoryIndex) {
            handle.execute(this, timeSeconds, timePtr, ChasmMemoryAccess)
        }
    }
}
