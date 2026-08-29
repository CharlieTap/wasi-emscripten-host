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
import at.released.weh.filesystem.op.readwrite.FileSystemByteBuffer
import at.released.weh.filesystem.op.readwrite.ReadFd
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy
import at.released.weh.wasi.preview1.memory.DirectWasiMemoryReader
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import io.github.charlietap.chasm.host.HostMemory
import io.github.charlietap.chasm.host.NativeHostMemory
import io.github.charlietap.chasm.host.UnsafeHostApi

internal actual class ChasmWasiMemoryReader actual constructor(
    private val fileSystem: FileSystem,
) : DirectWasiMemoryReader<HostMemory> {
    @OptIn(UnsafeHostApi::class)
    actual override fun read(
        memory: HostMemory,
        fd: FileDescriptor,
        strategy: ReadWriteStrategy,
        iovecsPointer: WasmPtr,
        iovecCount: Int,
        memoryAccess: MemoryAccess<HostMemory>,
    ): Either<ReadError, ULong> {
        if (memory !is NativeHostMemory) {
            return readWithCopyFallback(memory, fileSystem, fd, strategy, iovecsPointer, iovecCount)
        }
        val backing = memory.unsafeBorrowByteArray()
        val buffers = List(iovecCount) { index ->
            val descriptor = iovecDescriptorPointer(iovecsPointer, index)
            FileSystemByteBuffer(backing, memory.readI32(descriptor), memory.readI32(checkedPointerAdd(descriptor, 4)))
        }
        return fileSystem.execute(ReadFd, ReadFd(fd, buffers, strategy))
    }
}
