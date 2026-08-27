/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.ext

import at.released.weh.wasi.preview1.type.Rights
import at.released.weh.wasi.preview1.type.RightsType
import at.released.weh.filesystem.fdrights.FdRightsType as FsRightsType

internal object WasiRightsMapper {
    // WASI and the filesystem deliberately share the same contiguous bit layout for all Preview 1 rights.
    private const val SUPPORTED_RIGHTS_MASK = 0x3fff_ffffL

    @FsRightsType
    fun getFsRights(
        @RightsType wasiRights: Rights,
    ): Long = wasiRights and SUPPORTED_RIGHTS_MASK
}
