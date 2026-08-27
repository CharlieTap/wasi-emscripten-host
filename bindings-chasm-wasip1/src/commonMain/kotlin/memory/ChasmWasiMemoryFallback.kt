/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import arrow.core.Either
import at.released.weh.filesystem.FileSystem
import at.released.weh.filesystem.error.ReadError
import at.released.weh.filesystem.error.WriteError
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.op.readwrite.FileSystemByteBuffer
import at.released.weh.filesystem.op.readwrite.ReadFd
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy
import at.released.weh.filesystem.op.readwrite.WriteFd
import at.released.weh.wasi.preview1.type.Ciovec
import at.released.weh.wasi.preview1.type.Iovec
import at.released.weh.wasm.core.WasmPtr
import io.github.charlietap.chasm.host.HostMemory

internal fun readWithCopyFallback(
    memory: HostMemory,
    fileSystem: FileSystem,
    fd: FileDescriptor,
    strategy: ReadWriteStrategy,
    iovecsPointer: WasmPtr,
    iovecCount: Int,
): Either<ReadError, ULong> {
    val iovecs = List(iovecCount) { index ->
        val descriptor = iovecDescriptorPointer(iovecsPointer, index)
        Iovec(memory.readI32(descriptor), memory.readI32(checkedPointerAdd(descriptor, 4)))
    }
    val buffers = iovecs.map { iovec -> FileSystemByteBuffer(ByteArray(iovec.bufLen)) }
    return fileSystem.execute(ReadFd, ReadFd(fd, buffers, strategy)).onRight { readBytes ->
        var bytesLeft = readBytes.toLong()
        for (index in iovecs.indices) {
            if (bytesLeft == 0L) break
            val buffer = buffers[index]
            val size = minOf(buffer.length, bytesLeft.toInt())
            memory.write(iovecs[index].buf, buffer.array, buffer.offset, size)
            bytesLeft -= size
        }
    }
}

internal fun writeWithCopyFallback(
    memory: HostMemory,
    fileSystem: FileSystem,
    fd: FileDescriptor,
    strategy: ReadWriteStrategy,
    ciovecsPointer: WasmPtr,
    ciovecCount: Int,
): Either<WriteError, ULong> {
    val cioVecs = List(ciovecCount) { index ->
        val descriptor = iovecDescriptorPointer(ciovecsPointer, index)
        Ciovec(memory.readI32(descriptor), memory.readI32(checkedPointerAdd(descriptor, 4)))
    }
    val buffers = cioVecs.map { ciovec ->
        val bytes = ByteArray(ciovec.bufLen)
        memory.read(bytes, ciovec.buf, ciovec.bufLen)
        FileSystemByteBuffer(bytes)
    }
    return fileSystem.execute(WriteFd, WriteFd(fd, buffers, strategy))
}

internal fun iovecDescriptorPointer(base: WasmPtr, index: Int): WasmPtr {
    require(index >= 0)
    return checkedPointerAdd(base, index.toULong() * 8uL)
}

internal fun checkedPointerAdd(base: WasmPtr, offset: Int): WasmPtr {
    require(offset >= 0)
    return checkedPointerAdd(base, offset.toULong())
}

private fun checkedPointerAdd(base: WasmPtr, offset: ULong): WasmPtr {
    val address = base.toUInt().toULong() + offset
    check(address <= UInt.MAX_VALUE.toULong()) { "WebAssembly memory pointer overflow" }
    return address.toUInt().toInt()
}
