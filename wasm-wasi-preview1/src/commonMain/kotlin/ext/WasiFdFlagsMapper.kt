/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.ext

import at.released.weh.wasi.preview1.type.Fdflags
import at.released.weh.wasi.preview1.type.FdflagsFlag
import at.released.weh.filesystem.model.FdFlag as FsFdFlag
import at.released.weh.filesystem.model.FdflagsType as FsFdflagsType
import at.released.weh.wasi.preview1.type.FdflagsType as WasiFdflagsType

internal object WasiFdFlagsMapper {
    @FsFdflagsType
    fun getFsFdlags(
        @WasiFdflagsType fdflags: Fdflags,
    ): Int {
        val flags = fdflags.toInt()
        return (flags and COMMON_FLAGS_MASK) or
                (if (flags and FdflagsFlag.RSYNC.toInt() != 0) FsFdFlag.FD_RSYNC else 0) or
                (if (flags and FdflagsFlag.SYNC.toInt() != 0) FsFdFlag.FD_SYNC else 0)
    }

    private const val COMMON_FLAGS_MASK = FsFdFlag.FD_APPEND or FsFdFlag.FD_DSYNC or FsFdFlag.FD_NONBLOCK
}
