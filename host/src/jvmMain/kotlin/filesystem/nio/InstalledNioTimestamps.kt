/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.nio

import at.released.weh.filesystem.ext.Os
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.ServiceLoader

private val installedNioTimestamps: NioTimestamps? by lazy {
    val implementations = ServiceLoader.load(NioTimestamps::class.java, NioTimestamps::class.java.classLoader)
        .iterator()
        .asSequence()
        .toList()
    check(implementations.size <= 1) {
        "Expected at most one NioTimestamps, found ${implementations.map { it.javaClass.name }}"
    }
    implementations.firstOrNull()
}

internal fun nioTimestampsFor(path: Path): NioTimestamps? = if (
    Os.isAndroid && path.fileSystem === FileSystems.getDefault()
) {
    installedNioTimestamps
} else {
    null
}
