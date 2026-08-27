/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import arrow.core.Either
import at.released.weh.filesystem.FileSystem
import at.released.weh.filesystem.error.WriteError
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.op.readwrite.FileSystemByteBuffer
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy
import at.released.weh.filesystem.op.readwrite.WriteFd
import at.released.weh.wasi.preview1.memory.DirectWasiMemoryWriter
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import io.github.charlietap.chasm.host.HostMemory
import io.github.charlietap.chasm.host.NativeHostMemory
import io.github.charlietap.chasm.host.UnsafeHostApi

internal actual class ChasmWasiMemoryWriter actual constructor(
    private val fileSystem: FileSystem,
) : DirectWasiMemoryWriter<HostMemory> {
    @OptIn(UnsafeHostApi::class)
    override fun write(
        memory: HostMemory,
        fd: FileDescriptor,
        strategy: ReadWriteStrategy,
        ciovecsPointer: WasmPtr,
        ciovecCount: Int,
        memoryAccess: MemoryAccess<HostMemory>,
    ): Either<WriteError, ULong> {
        if (memory !is NativeHostMemory) {
            return writeWithCopyFallback(memory, fileSystem, fd, strategy, ciovecsPointer, ciovecCount)
        }
        val backing = memory.unsafeBorrowByteArray()
        val buffers = List(ciovecCount) { index ->
            val descriptor = iovecDescriptorPointer(ciovecsPointer, index)
            FileSystemByteBuffer(backing, memory.readI32(descriptor), memory.readI32(checkedPointerAdd(descriptor, 4)))
        }
        return fileSystem.execute(WriteFd, WriteFd(fd, buffers, strategy))
    }
}
