/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.module.emscripten.function

import at.released.weh.bindings.chasm.module.emscripten.HostFunctionProvider
import at.released.weh.emcripten.runtime.function.MmapJsFunctionHandle
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.readI32
import io.github.charlietap.chasm.host.readI64
import io.github.charlietap.chasm.host.writeI32

internal class MmapJs(
    host: EmbedderHost,
) : HostFunctionProvider {
    private val handle = MmapJsFunctionHandle(host)
    override val function: HostFunction = HostFunction { parameters, results ->
        @Suppress("MagicNumber")
        val result: Int = handle.execute(
            parameters.readI32(0),
            parameters.readI32(1),
            parameters.readI32(2),
            parameters.readI32(3),
            parameters.readI64(4),
            parameters.readI32(5),
            parameters.readI32(6),
        )
        results.writeI32(0, result)
    }
}
