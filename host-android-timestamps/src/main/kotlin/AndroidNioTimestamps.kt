/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.nio.android

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import at.released.weh.filesystem.error.AccessDenied
import at.released.weh.filesystem.error.BadFileDescriptor
import at.released.weh.filesystem.error.InvalidArgument
import at.released.weh.filesystem.error.IoError
import at.released.weh.filesystem.error.NameTooLong
import at.released.weh.filesystem.error.NoEntry
import at.released.weh.filesystem.error.NotDirectory
import at.released.weh.filesystem.error.NotSupported
import at.released.weh.filesystem.error.Overflow
import at.released.weh.filesystem.error.PermissionDenied
import at.released.weh.filesystem.error.ReadOnlyFileSystem
import at.released.weh.filesystem.error.SetTimestampError
import at.released.weh.filesystem.error.StatError
import at.released.weh.filesystem.error.TooManySymbolicLinks
import at.released.weh.filesystem.nio.NioTimestamps
import at.released.weh.filesystem.op.stat.StructTimespec
import java.nio.file.Path
import android.system.StructTimespec as AndroidTimespec

/**
 * Reads file timestamps with `stat` and sets them with `utimensat`, keeping nanosecond precision and
 * supporting symbolic links themselves.
 *
 * The host picks this up automatically when the artifact is on the classpath.
 */
public class AndroidNioTimestamps : NioTimestamps {
    override fun read(
        path: Path,
        followSymlinks: Boolean,
    ): Either<StatError, NioTimestamps.Times> {
        val absolutePath = path.toAbsolutePath().toString()
        val stat = try {
            if (followSymlinks) Os.stat(absolutePath) else Os.lstat(absolutePath)
        } catch (exception: ErrnoException) {
            return errnoToStatError(exception.errno, absolutePath).left()
        }
        return NioTimestamps.Times(
            accessTime = stat.st_atim.toStructTimespec(),
            modificationTime = stat.st_mtim.toStructTimespec(),
            changeStatusTime = stat.st_ctim.toStructTimespec(),
        ).right()
    }

    override fun set(
        path: Path,
        followSymlinks: Boolean,
        atimeNanoseconds: Long?,
        mtimeNanoseconds: Long?,
    ): Either<SetTimestampError, Unit> {
        if (atimeNanoseconds == null && mtimeNanoseconds == null) {
            return Unit.right()
        }
        val absolutePath = path.toAbsolutePath().toString()
        if ('\u0000' in absolutePath) {
            return InvalidArgument("Path contains a NUL character").left()
        }
        val errno = NativeTimestamps.utimensat(
            path = absolutePath.encodeToByteArray(),
            followSymlinks = followSymlinks,
            hasAtime = atimeNanoseconds != null,
            atime = atimeNanoseconds ?: 0,
            hasMtime = mtimeNanoseconds != null,
            mtime = mtimeNanoseconds ?: 0,
        )
        return if (errno == 0) {
            Unit.right()
        } else {
            errnoToSetTimestampError(errno, absolutePath).left()
        }
    }

    private fun AndroidTimespec.toStructTimespec(): StructTimespec = StructTimespec(
        seconds = tv_sec,
        nanoseconds = tv_nsec,
    )

    private fun errnoToStatError(errno: Int, path: String): StatError = when (errno) {
        OsConstants.EACCES -> AccessDenied("Permission denied `$path`")
        OsConstants.ENOENT -> NoEntry("No such file `$path`")
        OsConstants.ELOOP -> TooManySymbolicLinks("Too many symbolic links `$path`")
        OsConstants.ENAMETOOLONG -> NameTooLong("Name too long `$path`")
        OsConstants.ENOTDIR -> NotDirectory("Not a directory `$path`")
        OsConstants.EBADF -> BadFileDescriptor("Bad file descriptor `$path`")
        OsConstants.EINVAL -> InvalidArgument("Invalid argument `$path`")
        else -> IoError("stat failed with errno $errno `$path`")
    }

    private fun errnoToSetTimestampError(errno: Int, path: String): SetTimestampError = when (errno) {
        OsConstants.EACCES -> AccessDenied("Permission denied `$path`")
        OsConstants.EPERM -> PermissionDenied("Operation not permitted `$path`")
        OsConstants.ENOENT -> NoEntry("No such file `$path`")
        OsConstants.ELOOP -> TooManySymbolicLinks("Too many symbolic links `$path`")
        OsConstants.ENAMETOOLONG -> NameTooLong("Name too long `$path`")
        OsConstants.ENOTDIR -> NotDirectory("Not a directory `$path`")
        OsConstants.EROFS -> ReadOnlyFileSystem("Read-only file system `$path`")
        OsConstants.EINVAL -> InvalidArgument("Invalid timestamp for `$path`")
        OsConstants.EOVERFLOW -> Overflow("Timestamp out of range for `$path`")
        OsConstants.EBADF -> BadFileDescriptor("Bad file descriptor `$path`")
        OsConstants.ENOSYS, OsConstants.EOPNOTSUPP -> NotSupported("Setting timestamps not supported `$path`")
        else -> IoError("utimensat failed with errno $errno `$path`")
    }
}
