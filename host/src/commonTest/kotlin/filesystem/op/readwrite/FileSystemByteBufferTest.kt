/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.op.readwrite

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileSystemByteBufferTest {
    @Test
    fun `accepts an empty range at the end of the backing array`() {
        val buffer = FileSystemByteBuffer(ByteArray(4), offset = 4, length = 0)

        assertEquals(4, buffer.offset)
        assertEquals(0, buffer.length)
    }

    @Test
    fun `rejects negative or out of bounds ranges without integer overflow`() {
        val backing = ByteArray(8)

        assertFailsWith<IllegalArgumentException> { FileSystemByteBuffer(backing, offset = 1, length = -1) }
        assertFailsWith<IllegalArgumentException> { FileSystemByteBuffer(backing, offset = 7, length = 2) }
        assertFailsWith<IllegalArgumentException> { FileSystemByteBuffer(backing, offset = 1, length = Int.MAX_VALUE) }
    }
}
