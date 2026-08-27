/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.function

import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import at.released.weh.filesystem.error.PrestatError
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.model.IntFileDescriptor
import at.released.weh.filesystem.op.prestat.PrestatFd
import at.released.weh.filesystem.op.prestat.PrestatResult
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction
import at.released.weh.wasi.preview1.ext.wasiErrno
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.write
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations

public class FdPrestatDirNameFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(WasiPreview1HostFunction.FD_PRESTAT_DIR_NAME, host) {
    @OptIn(UnsafeByteStringApi::class)
    public fun <M> execute(
        memory: M,
        @IntFileDescriptor fd: FileDescriptor,
        @IntWasmPtr(Byte::class) dstPath: WasmPtr,
        dstPathLen: Int,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        return host.fileSystem.execute(PrestatFd, PrestatFd(fd))
            .mapLeft(PrestatError::wasiErrno)
            .flatMap { prestatResult: PrestatResult ->
                val path = prestatResult.path
                if (path.utf8SizeBytes > dstPathLen) {
                    return@flatMap Errno.NAMETOOLONG.left()
                }
                UnsafeByteStringOperations.withByteArrayUnsafe(path.utf8Bytes) { bytes ->
                    memory.write(dstPath, bytes)
                }
                Errno.SUCCESS.right()
            }.getOrElse { it }
    }
}
