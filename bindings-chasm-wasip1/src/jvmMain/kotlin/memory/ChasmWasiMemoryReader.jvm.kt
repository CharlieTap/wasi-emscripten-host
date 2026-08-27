/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import arrow.core.Either
import arrow.core.raise.either
import at.released.weh.filesystem.FileSystem
import at.released.weh.filesystem.error.FileSystemOperationError
import at.released.weh.filesystem.error.IoError
import at.released.weh.filesystem.error.ReadError
import at.released.weh.filesystem.fdresource.nio.NioFileChannel
import at.released.weh.filesystem.fdresource.nio.readCatching
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.nio.op.RunWithChannelFd
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy.CurrentPosition
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy.Position
import at.released.weh.wasi.preview1.memory.DirectWasiMemoryReader
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import io.github.charlietap.chasm.host.HostMemory
import io.github.charlietap.chasm.host.JvmHostMemory
import io.github.charlietap.chasm.host.UnsafeHostApi
import java.nio.ByteBuffer

internal actual class ChasmWasiMemoryReader actual constructor(
    private val fileSystem: FileSystem,
) : DirectWasiMemoryReader<HostMemory> {
    private val supportsDirectChannel: Boolean = fileSystem.isOperationSupported(RunWithChannelFd)

    @OptIn(UnsafeHostApi::class)
    override fun read(
        memory: HostMemory,
        fd: FileDescriptor,
        strategy: ReadWriteStrategy,
        iovecsPointer: WasmPtr,
        iovecCount: Int,
        memoryAccess: MemoryAccess<HostMemory>,
    ): Either<ReadError, ULong> {
        if (memory !is JvmHostMemory || !supportsDirectChannel) {
            return readWithCopyFallback(memory, fileSystem, fd, strategy, iovecsPointer, iovecCount)
        }
        val backing = memory.unsafeBorrowByteBuffer()
        if (iovecCount == 1) {
            val request = RunWithChannelFd(
                fd = fd,
                block = { channel ->
                    channel.fold(
                        ifLeft = { error -> Either.Left(error) },
                        ifRight = {
                            val lengthPointer = checkedPointerAdd(iovecsPointer, Int.SIZE_BYTES)
                            val buffer = backing.view(memory.readI32(iovecsPointer), memory.readI32(lengthPointer))
                            directRead(it, buffer, strategy)
                        },
                    )
                },
                nonNioResourceFallback = {
                    readWithCopyFallback(memory, fileSystem, fd, strategy, iovecsPointer, iovecCount)
                },
            )
            return fileSystem.execute(RunWithChannelFd.key(), request).mapLeft { error ->
                error as? ReadError ?: IoError(error.toString())
            }
        }
        val request = RunWithChannelFd(
            fd = fd,
            block = { channel ->
                channel.fold(
                    ifLeft = { error -> Either.Left(error) },
                    ifRight = {
                        val buffers = Array(iovecCount) { index ->
                            val descriptor = iovecDescriptorPointer(iovecsPointer, index)
                            backing.view(memory.readI32(descriptor), memory.readI32(checkedPointerAdd(descriptor, 4)))
                        }
                        directRead(it, buffers, strategy)
                    },
                )
            },
            nonNioResourceFallback = {
                readWithCopyFallback(memory, fileSystem, fd, strategy, iovecsPointer, iovecCount)
            },
        )
        return fileSystem.execute(RunWithChannelFd.key(), request).mapLeft { error ->
            error as? ReadError ?: IoError(error.toString())
        }
    }

    private fun directRead(
        channel: NioFileChannel,
        buffer: ByteBuffer,
        strategy: ReadWriteStrategy,
    ): Either<FileSystemOperationError, ULong> = when (strategy) {
        CurrentPosition -> readCatching { channel.channel.read(buffer) }
            .map { read -> if (read == -1) 0UL else read.toULong() }
        is Position -> readCatching { channel.channel.read(buffer, strategy.position) }
            .map { read -> if (read == -1) 0UL else read.toULong() }
    }

    private fun directRead(
        channel: NioFileChannel,
        buffers: Array<ByteBuffer>,
        strategy: ReadWriteStrategy,
    ): Either<FileSystemOperationError, ULong> = when (strategy) {
        CurrentPosition -> readCatching { channel.channel.read(buffers) }
            .map { read -> if (read == -1L) 0UL else read.toULong() }
        is Position -> either {
            var offset = strategy.position
            var total = 0UL
            for (buffer in buffers) {
                val requested = buffer.remaining()
                val read = readCatching { channel.channel.read(buffer, offset) }.bind()
                if (read > 0) {
                    offset += read
                    total += read.toULong()
                }
                if (read < requested) break
            }
            total
        }
    }
}

private fun ByteBuffer.view(offset: Int, length: Int): ByteBuffer {
    val end = Math.addExact(offset, length)
    return duplicate().apply {
        position(offset)
        limit(end)
    }
}
