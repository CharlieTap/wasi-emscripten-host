/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.ext

import at.released.weh.filesystem.model.FdFlag
import at.released.weh.wasi.preview1.type.FdflagsFlag
import kotlin.test.Test
import kotlin.test.assertEquals

class WasiFdFlagsMapperTest {
    @Test
    fun `maps common flags and swaps the differing sync bits`() {
        val wasiFlags = (
                FdflagsFlag.APPEND.toInt() or
                        FdflagsFlag.DSYNC.toInt() or
                        FdflagsFlag.NONBLOCK.toInt() or
                        FdflagsFlag.RSYNC.toInt() or
                        FdflagsFlag.SYNC.toInt()
                ).toShort()

        assertEquals(
            FdFlag.FD_APPEND or FdFlag.FD_DSYNC or FdFlag.FD_NONBLOCK or FdFlag.FD_RSYNC or FdFlag.FD_SYNC,
            WasiFdFlagsMapper.getFsFdlags(wasiFlags),
        )
        assertEquals(FdFlag.FD_RSYNC, WasiFdFlagsMapper.getFsFdlags(FdflagsFlag.RSYNC))
        assertEquals(FdFlag.FD_SYNC, WasiFdFlagsMapper.getFsFdlags(FdflagsFlag.SYNC))
    }
}
