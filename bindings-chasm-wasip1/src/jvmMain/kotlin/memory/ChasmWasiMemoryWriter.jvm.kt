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
import at.released.weh.filesystem.error.WriteError
import at.released.weh.filesystem.fdresource.nio.NioFileChannel
import at.released.weh.filesystem.fdresource.nio.isInAppendMode
import at.released.weh.filesystem.fdresource.nio.writeCatching
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.nio.op.RunWithChannelFd
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy.CurrentPosition
import at.released.weh.filesystem.op.readwrite.ReadWriteStrategy.Position
import at.released.weh.wasi.preview1.memory.DirectWasiMemoryWriter
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import io.github.charlietap.chasm.host.HostMemory
import io.github.charlietap.chasm.host.JvmHostMemory
import io.github.charlietap.chasm.host.UnsafeHostApi
import java.nio.ByteBuffer

internal actual class ChasmWasiMemoryWriter actual constructor(
    private val fileSystem: FileSystem,
) : DirectWasiMemoryWriter<HostMemory> {
    private val supportsDirectChannel: Boolean = fileSystem.isOperationSupported(RunWithChannelFd)

    @OptIn(UnsafeHostApi::class)
    actual override fun write(
        memory: HostMemory,
        fd: FileDescriptor,
        strategy: ReadWriteStrategy,
        ciovecsPointer: WasmPtr,
        ciovecCount: Int,
        memoryAccess: MemoryAccess<HostMemory>,
    ): Either<WriteError, ULong> {
        if (memory !is JvmHostMemory || !supportsDirectChannel) {
            return writeWithCopyFallback(memory, fileSystem, fd, strategy, ciovecsPointer, ciovecCount)
        }
        val backing = memory.unsafeBorrowByteBuffer()
        if (ciovecCount == 1) {
            val request = RunWithChannelFd(
                fd = fd,
                block = { channel ->
                    channel.fold(
                        ifLeft = { error -> Either.Left(error) },
                        ifRight = {
                            val lengthPointer = checkedPointerAdd(ciovecsPointer, Int.SIZE_BYTES)
                            val buffer = backing.view(memory.readI32(ciovecsPointer), memory.readI32(lengthPointer))
                            directWrite(it, buffer, strategy)
                        },
                    )
                },
                nonNioResourceFallback = {
                    writeWithCopyFallback(memory, fileSystem, fd, strategy, ciovecsPointer, ciovecCount)
                },
            )
            return fileSystem.execute(RunWithChannelFd.key(), request).mapLeft { error ->
                error as? WriteError ?: IoError(error.toString())
            }
        }
        val request = RunWithChannelFd(
            fd = fd,
            block = { channel ->
                channel.fold(
                    ifLeft = { error -> Either.Left(error) },
                    ifRight = {
                        val buffers = Array(ciovecCount) { index ->
                            val descriptor = iovecDescriptorPointer(ciovecsPointer, index)
                            backing.view(memory.readI32(descriptor), memory.readI32(checkedPointerAdd(descriptor, 4)))
                        }
                        directWrite(it, buffers, strategy)
                    },
                )
            },
            nonNioResourceFallback = {
                writeWithCopyFallback(memory, fileSystem, fd, strategy, ciovecsPointer, ciovecCount)
            },
        )
        return fileSystem.execute(RunWithChannelFd.key(), request).mapLeft { error ->
            error as? WriteError ?: IoError(error.toString())
        }
    }

    private fun directWrite(
        channel: NioFileChannel,
        buffer: ByteBuffer,
        strategy: ReadWriteStrategy,
    ): Either<FileSystemOperationError, ULong> = when (strategy) {
        CurrentPosition -> writeCatching {
            if (channel.isInAppendMode()) channel.channel.position(channel.channel.size())
            channel.channel.write(buffer).toULong()
        }
        is Position -> writeCatching { channel.channel.write(buffer, strategy.position).toULong() }
    }

    private fun directWrite(
        channel: NioFileChannel,
        buffers: Array<ByteBuffer>,
        strategy: ReadWriteStrategy,
    ): Either<FileSystemOperationError, ULong> = when (strategy) {
        CurrentPosition -> writeCatching {
            if (channel.isInAppendMode()) channel.channel.position(channel.channel.size())
            channel.channel.write(buffers).toULong()
        }
        is Position -> either {
            var offset = strategy.position
            var total = 0UL
            for (buffer in buffers) {
                val requested = buffer.remaining()
                val written = writeCatching { channel.channel.write(buffer, offset) }.bind()
                if (written > 0) {
                    offset += written
                    total += written.toULong()
                }
                if (written < requested) break
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
