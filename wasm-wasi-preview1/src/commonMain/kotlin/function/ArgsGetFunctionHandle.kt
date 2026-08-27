/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.function

import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction
import at.released.weh.wasi.preview1.ext.WasiArgsEnvironmentFunc
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeNullTerminatedString

public class ArgsGetFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(WasiPreview1HostFunction.ARGS_GET, host) {
    public fun <M> execute(
        memory: M,
        @IntWasmPtr(Int::class) argvAddr: WasmPtr,
        @IntWasmPtr(Int::class) argvSizesAddr: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        var argvPointer = argvAddr
        var argBufPointer = argvSizesAddr
        for (argument in host.commandArgsProvider.getCommandArgs()) {
            val argString = WasiArgsEnvironmentFunc.cleanupProgramArgument(argument)
            memory.writeI32(addr = argvPointer, data = argBufPointer)
            argvPointer += 4
            argBufPointer += memory.writeNullTerminatedString(argBufPointer, argString)
        }
        return Errno.SUCCESS
    }
}
