/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.nio.android

internal object NativeTimestamps {
    init {
        System.loadLibrary("weh_timestamps")
    }

    @JvmStatic
    external fun utimensat(
        path: ByteArray,
        followSymlinks: Boolean,
        hasAtime: Boolean,
        atime: Long,
        hasMtime: Boolean,
        mtime: Long,
    ): Int
}
