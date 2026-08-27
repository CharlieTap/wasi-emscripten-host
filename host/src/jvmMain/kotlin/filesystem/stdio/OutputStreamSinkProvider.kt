/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.stdio

import at.released.weh.filesystem.internal.fdresource.stdio.ByteArrayStdioSink
import kotlinx.io.RawSink
import kotlinx.io.asSink
import java.io.OutputStream

internal class OutputStreamSinkProvider(
    private val streamProvider: () -> OutputStream,
) : StdioSink.Provider {
    override fun open(): StdioSink = OutputStreamStdioSink(streamProvider())
}

private class OutputStreamStdioSink(
    private val outputStream: OutputStream,
    sink: RawSink = outputStream.asSink(),
) : StdioSink, ByteArrayStdioSink, RawSink by sink {
    override fun writeFromByteArray(source: ByteArray, startIndex: Int, endIndex: Int) {
        outputStream.write(source, startIndex, endIndex - startIndex)
    }
}
