/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.function

import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.model.IntFileDescriptor
import at.released.weh.filesystem.op.prestat.PrestatFd
import at.released.weh.filesystem.op.prestat.PrestatResult
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction
import at.released.weh.wasi.preview1.ext.foldToErrno
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasi.preview1.type.Prestat
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.writeI32

public class FdPrestatGetFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(WasiPreview1HostFunction.FD_PRESTAT_GET, host) {
    public fun <M> execute(
        memory: M,
        @IntFileDescriptor fd: FileDescriptor,
        @IntWasmPtr(Prestat::class) dstAddr: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        return host.fileSystem.execute(PrestatFd, PrestatFd(fd))
            .onRight { prestatResult: PrestatResult ->
                memory.writeI32(dstAddr, 0)
                memory.writeI32(dstAddr + 4, prestatResult.path.utf8SizeBytes)
            }.foldToErrno()
    }
}
