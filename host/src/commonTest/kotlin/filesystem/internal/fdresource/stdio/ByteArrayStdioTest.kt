/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.internal.fdresource.stdio

import at.released.weh.filesystem.op.readwrite.FileSystemByteBuffer
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ByteArrayStdioTest {
    @Test
    fun `byte array sink bypasses RawSink staging`() {
        val sink = RecordingByteArraySink()

        val result = sink.transferFrom(
            listOf(
                FileSystemByteBuffer(byteArrayOf(0, 1, 2, 3), offset = 1, length = 2),
                FileSystemByteBuffer(byteArrayOf(4, 5, 6), offset = 0, length = 3),
            ),
        )

        assertEquals(5UL, result.getOrNull())
        assertContentEquals(byteArrayOf(1, 2, 4, 5, 6), sink.received.toByteArray())
        assertEquals(0, sink.rawWrites)
    }

    @Test
    fun `byte array source bypasses RawSource staging`() {
        val source = RecordingByteArraySource(byteArrayOf(1, 2, 3, 4, 5))
        val first = ByteArray(4)
        val second = ByteArray(6)

        val result = source.transferTo(
            listOf(
                FileSystemByteBuffer(first, offset = 1, length = 2),
                FileSystemByteBuffer(second, offset = 2, length = 4),
            ),
        )

        assertEquals(5UL, result.getOrNull())
        assertContentEquals(byteArrayOf(0, 1, 2, 0), first)
        assertContentEquals(byteArrayOf(0, 0, 3, 4, 5, 0), second)
        assertEquals(0, source.rawReads)
    }

    private class RecordingByteArraySink : RawSink, ByteArrayStdioSink {
        val received = mutableListOf<Byte>()
        var rawWrites = 0

        override fun writeFromByteArray(source: ByteArray, startIndex: Int, endIndex: Int) {
            for (index in startIndex until endIndex) received += source[index]
        }

        override fun write(source: Buffer, byteCount: Long) {
            rawWrites += 1
        }

        override fun flush() = Unit
        override fun close() = Unit
    }

    private class RecordingByteArraySource(
        private val content: ByteArray,
    ) : RawSource, ByteArrayStdioSource {
        var rawReads = 0
        private var position = 0

        override fun readToByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
            if (position == content.size) return -1
            val count = minOf(endIndex - startIndex, content.size - position)
            content.copyInto(sink, startIndex, position, position + count)
            position += count
            return count
        }

        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
            rawReads += 1
            return -1
        }

        override fun close() = Unit
    }
}
