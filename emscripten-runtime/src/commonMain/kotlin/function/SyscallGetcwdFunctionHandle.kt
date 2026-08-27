/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.emcripten.runtime.function

import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_GETCWD
import at.released.weh.filesystem.model.FileSystemErrno.Companion.wasiPreview1Code
import at.released.weh.filesystem.op.cwd.GetCurrentWorkingDirectory
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.write
import at.released.weh.wasm.core.memory.writeI8

public class SyscallGetcwdFunctionHandle(
    host: EmbedderHost,
) : EmscriptenHostFunctionHandle(SYSCALL_GETCWD, host) {
    public fun <M> execute(
        memory: M,
        @IntWasmPtr(Byte::class) dst: WasmPtr,
        size: Int,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Int = with(memoryAccess) {
        if (size == 0) {
            return -Errno.INVAL.code
        }
        return host.fileSystem.execute(GetCurrentWorkingDirectory, Unit)
            .fold(
                ifLeft = { -it.errno.wasiPreview1Code },
            ) { currentWorkingDirectory ->
                val encodedPath = currentWorkingDirectory.toString().encodeToByteArray()
                val pathSize = encodedPath.size + 1
                if (size < pathSize) {
                    return@fold -Errno.RANGE.code
                }
                memory.write(dst, encodedPath)
                memory.writeI8(dst + encodedPath.size, 0)
                pathSize
            }
    }
}
