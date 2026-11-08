/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.nio

import arrow.core.Either
import at.released.weh.common.api.WasiEmscriptenHostDataModel
import at.released.weh.filesystem.error.SetTimestampError
import at.released.weh.filesystem.error.StatError
import at.released.weh.filesystem.op.stat.StructTimespec
import java.nio.file.Path

/**
 * Reads and sets file timestamps for the NIO file system on Android, in place of `java.nio`, which keeps only
 * microseconds there and cannot update a symbolic link itself.
 *
 * Add the `host-android-timestamps` artifact to provide one. The host discovers it with
 * [java.util.ServiceLoader] when running on Android and uses it for paths on the default file system.
 * At most one implementation may be on the classpath.
 */
public interface NioTimestamps {
    /**
     * Returns the access, modification and status change times of [path].
     * When [followSymlinks] is false and [path] is a symbolic link, returns the times of the link itself.
     */
    public fun read(
        path: Path,
        followSymlinks: Boolean,
    ): Either<StatError, Times>

    /**
     * Sets the access and modification times of [path], in nanoseconds since the epoch.
     * A `null` time is left unchanged.
     * When [followSymlinks] is false and [path] is a symbolic link, the link itself is updated.
     */
    public fun set(
        path: Path,
        followSymlinks: Boolean,
        atimeNanoseconds: Long?,
        mtimeNanoseconds: Long?,
    ): Either<SetTimestampError, Unit>

    @WasiEmscriptenHostDataModel
    public class Times(
        public val accessTime: StructTimespec,
        public val modificationTime: StructTimespec,
        public val changeStatusTime: StructTimespec,
    )
}
