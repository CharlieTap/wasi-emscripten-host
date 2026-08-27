/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.function

import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.model.IntFileDescriptor
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasi.preview1.type.Fdflags
import at.released.weh.wasi.preview1.type.FdflagsType
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess

public class SockAcceptFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(WasiPreview1HostFunction.SOCK_ACCEPT, host) {
    @Suppress("UNUSED_PARAMETER")
    public fun <M> execute(
        memory: M,
        @IntFileDescriptor fd: FileDescriptor,
        @FdflagsType fdFlags: Fdflags,
        @IntWasmPtr(FileDescriptor::class) expectedFdAddr: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        // TODO
        return Errno.NOTSUP
    }
}
