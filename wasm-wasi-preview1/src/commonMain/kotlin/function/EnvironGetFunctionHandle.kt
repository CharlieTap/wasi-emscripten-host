/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.function

import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction
import at.released.weh.wasi.preview1.ext.WasiArgsEnvironmentFunc.encodeEnvToWasi
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeNullTerminatedString

public class EnvironGetFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(WasiPreview1HostFunction.ENVIRON_GET, host) {
    public fun <M> execute(
        memory: M,
        @IntWasmPtr(Int::class) environPAddr: WasmPtr,
        @IntWasmPtr(Int::class) environBufAddr: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        var pp = environPAddr
        var bufP = environBufAddr
        for (environmentEntry in host.systemEnvProvider.getSystemEnv().entries) {
            val envString = environmentEntry.encodeEnvToWasi()
            memory.writeI32(pp, bufP)
            pp += 4
            bufP += memory.writeNullTerminatedString(bufP, envString)
        }
        return Errno.SUCCESS
    }
}
