/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.internal.fdresource.stdio

/** Fast path implemented by the built-in stdio sinks to avoid staging data through a [kotlinx.io.Buffer]. */
internal interface ByteArrayStdioSink {
    fun writeFromByteArray(source: ByteArray, startIndex: Int, endIndex: Int)
}

/** Fast path implemented by the built-in stdio sources to avoid staging data through a [kotlinx.io.Buffer]. */
internal interface ByteArrayStdioSource {
    fun readToByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int
}
