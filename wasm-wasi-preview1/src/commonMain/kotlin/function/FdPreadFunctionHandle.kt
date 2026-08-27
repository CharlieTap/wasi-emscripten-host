/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.function

import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.model.IntFileDescriptor
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction
import at.released.weh.wasi.preview1.ext.foldToErrno
import at.released.weh.wasi.preview1.ext.readIovecs
import at.released.weh.wasi.preview1.memory.DirectWasiMemoryReader
import at.released.weh.wasi.preview1.memory.WasiMemoryReader
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasi.preview1.type.Iovec
import at.released.weh.wasi.preview1.type.IovecArray
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.writeI32

public class FdPreadFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(WasiPreview1HostFunction.FD_PREAD, host) {
    public fun <M> execute(
        memory: M,
        bulkReader: WasiMemoryReader,
        @IntFileDescriptor fd: FileDescriptor,
        @IntWasmPtr(Iovec::class) pIov: WasmPtr,
        iovCnt: Int,
        offset: Long,
        @IntWasmPtr(Int::class) pNum: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        val ioVecs: IovecArray = readIovecs(memory, pIov, iovCnt)
        return bulkReader.read(fd, ReadWriteStrategy.Position(offset), ioVecs)
            .onRight { readBytes -> memory.writeI32(pNum, readBytes.toInt()) }
            .foldToErrno()
    }

    public fun <M> executeDirect(
        memory: M,
        bulkReader: DirectWasiMemoryReader<M>,
        @IntFileDescriptor fd: FileDescriptor,
        @IntWasmPtr(Iovec::class) pIov: WasmPtr,
        iovCnt: Int,
        offset: Long,
        @IntWasmPtr(Int::class) pNum: WasmPtr,
        memoryAccess: MemoryAccess<M>,
    ): Errno = with(memoryAccess) {
        return bulkReader.read(memory, fd, ReadWriteStrategy.Position(offset), pIov, iovCnt, memoryAccess)
            .onRight { readBytes -> memory.writeI32(pNum, readBytes.toInt()) }
            .foldToErrno()
    }
}
