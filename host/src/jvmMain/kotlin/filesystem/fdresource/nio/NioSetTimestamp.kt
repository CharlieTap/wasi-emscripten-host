/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.fdresource.nio

import arrow.core.Either
import at.released.weh.filesystem.error.AccessDenied
import at.released.weh.filesystem.error.IoError
import at.released.weh.filesystem.error.NoEntry
import at.released.weh.filesystem.error.NotSupported
import at.released.weh.filesystem.error.SetTimestampError
import at.released.weh.filesystem.error.TooManySymbolicLinks
import at.released.weh.filesystem.ext.Os
import at.released.weh.filesystem.ext.asLinkOptions
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.FileSystemException
import java.nio.file.FileSystemLoopException
import java.nio.file.FileSystems
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.io.path.fileAttributesView
import kotlin.io.path.isSymbolicLink

internal fun nioSetTimestamp(
    path: Path,
    followSymlinks: Boolean,
    atimeNanoseconds: Long?,
    mtimeNanoseconds: Long?,
): Either<SetTimestampError, Unit> {
    val isDefaultFileSystem = path.fileSystem === FileSystems.getDefault()
    val options = asLinkOptions(followSymlinks = followSymlinks)
    val newAtime = atimeNanoseconds?.let { FileTime.from(it, NANOSECONDS) }
    val newMtime = mtimeNanoseconds?.let { FileTime.from(it, NANOSECONDS) }
    return Either.catch {
        path.fileAttributesView<BasicFileAttributeView>(options = options).setTimes(newMtime, newAtime, null)
    }.mapLeft {
        when (it) {
            is NoSuchFileException -> NoEntry("No such file `$path`")
            is AccessDeniedException -> AccessDenied("Permission denied")
            is FileSystemLoopException -> TooManySymbolicLinks("Too many symbolic links")
            is FileSystemException if !followSymlinks && Os.isAndroid && isDefaultFileSystem && path.isSymbolicLink() ->
                NotSupported("Setting timestamps on a symbolic link is not supported on Android")

            is IOException -> IoError("I/O error: ${it.message}")
            else -> throw IllegalStateException("Unexpected error", it)
        }
    }
}
