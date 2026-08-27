/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.function

import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.model.Filetype
import at.released.weh.filesystem.model.Filetype.BLOCK_DEVICE
import at.released.weh.filesystem.model.Filetype.CHARACTER_DEVICE
import at.released.weh.filesystem.model.Filetype.DIRECTORY
import at.released.weh.filesystem.model.Filetype.REGULAR_FILE
import at.released.weh.filesystem.model.Filetype.SOCKET_DGRAM
import at.released.weh.filesystem.model.Filetype.SOCKET_STREAM
import at.released.weh.filesystem.model.Filetype.SYMBOLIC_LINK
import at.released.weh.filesystem.model.Filetype.UNKNOWN
import at.released.weh.filesystem.model.IntFileDescriptor
import at.released.weh.filesystem.op.fdattributes.FdAttributes
import at.released.weh.filesystem.op.fdattributes.FdAttributesResult
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction.FD_FDSTAT_GET
import at.released.weh.wasi.preview1.ext.foldToErrno
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasi.preview1.type.FdflagsFlag
import at.released.weh.wasi.preview1.type.Fdstat
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.writeI16
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeI64
import at.released.weh.wasm.core.memory.writeI8
import at.released.weh.filesystem.model.FdFlag as FileSystemFdFlag
import at.released.weh.wasi.preview1.type.Filetype as WasiFiletype

public class FdFdstatGetFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(FD_FDSTAT_GET, host) {
    public fun <M> execute(
        memory: M,
        @IntFileDescriptor fd: FileDescriptor,
        @IntWasmPtr(Fdstat::class) dstAddr: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        return host.fileSystem.execute(FdAttributes, FdAttributes(fd))
            .onRight { prestatResult: FdAttributesResult ->
                memory.writeI8(dstAddr, prestatResult.type.toWasiType().code.toByte())
                memory.writeI8(dstAddr + 1, 0)
                memory.writeI16(dstAddr + 2, prestatResult.flags.toWasiFdFlags())
                memory.writeI32(dstAddr + 4, 0)
                memory.writeI64(dstAddr + 8, prestatResult.rights and SUPPORTED_RIGHTS_MASK)
                memory.writeI64(dstAddr + 16, prestatResult.inheritingRights and SUPPORTED_RIGHTS_MASK)
            }.foldToErrno()
    }

    private companion object {
        private const val SUPPORTED_RIGHTS_MASK = 0x3fff_ffffL
        private const val COMMON_FD_FLAGS_MASK =
            FileSystemFdFlag.FD_APPEND or FileSystemFdFlag.FD_DSYNC or FileSystemFdFlag.FD_NONBLOCK

        private fun Filetype.toWasiType(): WasiFiletype = when (this) {
            UNKNOWN -> WasiFiletype.UNKNOWN
            BLOCK_DEVICE -> WasiFiletype.BLOCK_DEVICE
            CHARACTER_DEVICE -> WasiFiletype.CHARACTER_DEVICE
            DIRECTORY -> WasiFiletype.DIRECTORY
            REGULAR_FILE -> WasiFiletype.REGULAR_FILE
            SOCKET_DGRAM -> WasiFiletype.SOCKET_DGRAM
            SOCKET_STREAM -> WasiFiletype.SOCKET_STREAM
            SYMBOLIC_LINK -> WasiFiletype.SYMBOLIC_LINK
        }

        private fun Int.toWasiFdFlags(): Short = ((this and COMMON_FD_FLAGS_MASK) or
                (if (this and FileSystemFdFlag.FD_RSYNC != 0) FdflagsFlag.RSYNC.toInt() else 0) or
                (if (this and FileSystemFdFlag.FD_SYNC != 0) FdflagsFlag.SYNC.toInt() else 0)).toShort()
    }
}
