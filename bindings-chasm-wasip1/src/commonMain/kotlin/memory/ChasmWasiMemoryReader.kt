/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import arrow.core.Either
import at.released.weh.filesystem.FileSystem
import at.released.weh.filesystem.error.ReadError
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy
import at.released.weh.wasi.preview1.memory.DirectWasiMemoryReader
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import io.github.charlietap.chasm.host.HostMemory

internal expect class ChasmWasiMemoryReader(fileSystem: FileSystem) : DirectWasiMemoryReader<HostMemory> {
    override fun read(
        memory: HostMemory,
        fd: FileDescriptor,
        strategy: ReadWriteStrategy,
        iovecsPointer: WasmPtr,
        iovecCount: Int,
        memoryAccess: MemoryAccess<HostMemory>,
    ): Either<ReadError, ULong>
}
