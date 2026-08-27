/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.function

import arrow.core.getOrElse
import at.released.weh.filesystem.model.BaseDirectory.DirectoryFd
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.model.IntFileDescriptor
import at.released.weh.filesystem.op.stat.Stat
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction.PATH_FILESTAT_GET
import at.released.weh.wasi.preview1.ext.foldToErrno
import at.released.weh.wasi.preview1.ext.readPathString
import at.released.weh.wasi.preview1.ext.writeTo
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasi.preview1.type.Filestat
import at.released.weh.wasi.preview1.type.Lookupflags
import at.released.weh.wasi.preview1.type.LookupflagsFlag.SYMLINK_FOLLOW
import at.released.weh.wasi.preview1.type.LookupflagsType
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess

public class PathFilestatGetFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(PATH_FILESTAT_GET, host) {
    public fun <M> execute(
        memory: M,
        @IntFileDescriptor fd: FileDescriptor,
        @LookupflagsType flags: Lookupflags,
        @IntWasmPtr(Byte::class) path: WasmPtr,
        pathSize: Int,
        @IntWasmPtr(Filestat::class) filestatAddr: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        val pathString = memory.readPathString(path, pathSize).getOrElse {
            return it
        }

        val statRequest = Stat(
            path = pathString,
            baseDirectory = DirectoryFd(fd),
            followSymlinks = flags and SYMLINK_FOLLOW == SYMLINK_FOLLOW,
        )

        return host.fileSystem.execute(Stat, statRequest)
            .onRight { stat ->
                stat.writeTo(memory, filestatAddr)
            }.foldToErrno()
    }
}
