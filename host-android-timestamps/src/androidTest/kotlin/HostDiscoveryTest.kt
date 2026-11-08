/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.nio.android

import android.system.Os
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import at.released.weh.filesystem.FileSystem
import at.released.weh.filesystem.nio.NioFileSystem
import at.released.weh.filesystem.op.settimestamp.SetTimestamp
import at.released.weh.filesystem.op.stat.Stat
import at.released.weh.filesystem.op.stat.timeNanos
import at.released.weh.filesystem.path.virtual.VirtualPath
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HostDiscoveryTest {
    @Test
    fun host_file_system_sets_and_reads_nanosecond_times_through_the_add_on() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "discovery").apply {
            deleteRecursively()
            mkdirs()
        }
        val file = File(root, "file").apply { writeText("x") }
        val link = File(root, "link")
        Os.symlink("file", link.path)

        FileSystem(NioFileSystem) { unrestricted = true }.use { fileSystem ->
            val filePath = VirtualPath.create(file.absolutePath).getOrNull()!!
            val linkPath = VirtualPath.create(link.absolutePath).getOrNull()!!

            val setFile = fileSystem.execute(
                SetTimestamp,
                SetTimestamp(
                    path = filePath,
                    atimeNanoseconds = null,
                    mtimeNanoseconds = MTIME,
                    followSymlinks = true,
                ),
            )
            val setLink = fileSystem.execute(
                SetTimestamp,
                SetTimestamp(
                    path = linkPath,
                    atimeNanoseconds = null,
                    mtimeNanoseconds = LINK_MTIME,
                    followSymlinks = false,
                ),
            )
            val fileStat = fileSystem.execute(Stat, Stat(path = filePath, followSymlinks = true))
            val linkStat = fileSystem.execute(Stat, Stat(path = linkPath, followSymlinks = false))

            assertThat(setFile).isEqualTo(Either.Right(Unit))
            assertThat(setLink).isEqualTo(Either.Right(Unit))
            assertThat(fileStat.getOrNull()!!.modificationTime.timeNanos).isEqualTo(MTIME)
            assertThat(linkStat.getOrNull()!!.modificationTime.timeNanos).isEqualTo(LINK_MTIME)
        }
        root.deleteRecursively()
    }

    private companion object {
        const val MTIME = 1_700_000_000_123_456_789L
        const val LINK_MTIME = 1_700_000_000_987_654_321L
    }
}
