/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.module.emscripten.function

import at.released.weh.bindings.chasm.module.emscripten.HostFunctionProvider
import at.released.weh.emcripten.runtime.function.EmscriptenAsmConstAsyncOnMainThreadFunctionHandle
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.readI32

internal class EmscriptenAsmConstAsyncOnMainThread(host: EmbedderHost) : HostFunctionProvider {
    private val handle = EmscriptenAsmConstAsyncOnMainThreadFunctionHandle(host)
    override val function: HostFunction = HostFunction { parameters, results ->
        handle.execute(
            emAsmAddr = parameters.readI32(0),
            sigPtr = parameters.readI32(1),
            argbuf = parameters.readI32(2),
        )
    }
}
