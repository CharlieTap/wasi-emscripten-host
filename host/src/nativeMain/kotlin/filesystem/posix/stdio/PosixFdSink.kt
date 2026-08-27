/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.posix.stdio

import arrow.core.Either
import arrow.core.right
import at.released.weh.filesystem.error.NonblockingPollError
import at.released.weh.filesystem.internal.fdresource.stdio.ByteArrayStdioSink
import at.released.weh.filesystem.posix.NativeFileFd
import at.released.weh.filesystem.stdio.StdioPollEvent
import at.released.weh.filesystem.stdio.StdioPollEvent.Companion.STDIO_POLL_EVENT_SUCCESS
import at.released.weh.filesystem.stdio.StdioSink
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import platform.posix.STDOUT_FILENO
import platform.posix.dup
import platform.posix.errno

internal expect fun syncNative(
    fd: NativeFileFd,
): Either<Int, Unit>

internal expect fun writeNative(
    fd: NativeFileFd,
    buf: CValuesRef<*>,
    bytes: Int,
): Either<Int, Int>

internal class PosixFdSink private constructor(
    private val fd: NativeFileFd,
) : StdioSink, ByteArrayStdioSink, StdioWithPollableFileDescriptor {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private var isClosed = atomic<Boolean>(false)
    override val pollableFileDescriptor: Int get() = fd.fd

    override fun flush() {
        checkSinkNotClosed()
        syncNative(fd)
            .onLeft { errno ->
                throw IOException("Can not flush $fd: $errno")
            }
    }

    override fun write(source: Buffer, byteCount: Long) {
        val bytes = source.readByteArray(byteCount.toInt())
        writeFromByteArray(bytes, 0, bytes.size)
    }

    override fun writeFromByteArray(source: ByteArray, startIndex: Int, endIndex: Int) {
        checkSinkNotClosed()
        require(startIndex in 0..endIndex && endIndex <= source.size)
        if (startIndex == endIndex) {
            return
        }
        val errnoOrNull = source.usePinned { buf ->
            var offset = startIndex
            var writeError: Int? = null
            while (offset != endIndex) {
                val writtenBytes = writeNative(fd, buf.addressOf(offset), endIndex - offset).fold(
                    ifLeft = {
                        writeError = it
                        0
                    },
                    ifRight = { it },
                )
                if (writeError != null) {
                    break
                }
                offset += writtenBytes
            }
            writeError
        }
        if (errnoOrNull != null) {
            throw IOException("Can not write to ${fd.fd}: $errnoOrNull")
        }
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            // Do not close the same file descriptor twice even in case of an error
            return
        }

        val result = platform.posix.close(fd.fd)
        if (result == -1) {
            throw IOException("Can not close $fd. Error `$errno`")
        }
    }

    override fun pollNonblocking(): Either<NonblockingPollError, StdioPollEvent> {
        // XXX: use real poll?
        return STDIO_POLL_EVENT_SUCCESS.right()
    }

    private fun checkSinkNotClosed(): Unit = check(!isClosed.value) { "Sink is closed" }

    internal companion object {
        fun create(
            fd: NativeFileFd = NativeFileFd(STDOUT_FILENO),
        ): PosixFdSink {
            val newfd = dup(fd.fd)
            if (newfd == -1) {
                throw IOException("Can not duplicate $fd. Error `$errno`")
            }
            return PosixFdSink(NativeFileFd(newfd))
        }
    }
}
