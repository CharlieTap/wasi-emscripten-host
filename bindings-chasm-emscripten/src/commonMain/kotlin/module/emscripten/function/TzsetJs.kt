/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.module.emscripten.function

import at.released.weh.bindings.chasm.memory.ChasmMemoryAccess
import at.released.weh.bindings.chasm.module.emscripten.HostFunctionProvider
import at.released.weh.emcripten.runtime.function.TzsetJsFunctionHandle
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.host.readI32
import io.github.charlietap.chasm.host.withMemory

internal class TzsetJs(
    host: EmbedderHost,
    private val memoryIndex: ModuleIndex.MemoryIndex,
) : HostFunctionProvider {
    private val handle = TzsetJsFunctionHandle(host)
    override val function: HostFunction = HostFunction { parameters, _ ->
        val timezone = parameters.readI32(0)
        val daylight = parameters.readI32(1)
        val stdName = parameters.readI32(2)
        val dstName = parameters.readI32(3)
        withMemory(memoryIndex) {
            handle.execute(this, timezone, daylight, stdName, dstName, ChasmMemoryAccess)
        }
    }
}
