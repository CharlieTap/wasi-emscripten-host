/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.nio.android

import android.system.Os
import android.system.StructTimespec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import at.released.weh.filesystem.error.NoEntry
import at.released.weh.filesystem.nio.NioTimestamps
import at.released.weh.filesystem.op.stat.timeNanos
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AndroidNioTimestampsTest {
    private val timestamps = AndroidNioTimestamps()
    private lateinit var root: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(context.cacheDir, "timestamps").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun sets_modification_time_with_nanosecond_precision_and_keeps_access_time() {
        val file = File(root, "file").apply { writeText("x") }
        val atimeBefore = Os.stat(file.path).st_atim.nanos()

        val result = timestamps.set(file.toPath(), true, null, MTIME)

        assertThat(result).isEqualTo(Either.Right(Unit))
        assertThat(Os.stat(file.path).st_mtim.nanos()).isEqualTo(MTIME)
        assertThat(Os.stat(file.path).st_atim.nanos()).isEqualTo(atimeBefore)
    }

    @Test
    fun sets_access_time_with_nanosecond_precision() {
        val file = File(root, "file").apply { writeText("x") }

        timestamps.set(file.toPath(), true, ATIME, null)

        assertThat(Os.stat(file.path).st_atim.nanos()).isEqualTo(ATIME)
    }

    @Test
    fun reads_times_with_nanosecond_precision() {
        val file = File(root, "file").apply { writeText("x") }
        timestamps.set(file.toPath(), true, ATIME, MTIME)

        val times = (timestamps.read(file.toPath(), true) as Either.Right<NioTimestamps.Times>).value

        assertThat(times.accessTime.timeNanos).isEqualTo(ATIME)
        assertThat(times.modificationTime.timeNanos).isEqualTo(MTIME)
        assertThat(times.changeStatusTime.timeNanos).isEqualTo(Os.stat(file.path).st_ctim.nanos())
    }

    @Test
    fun updates_and_reads_the_symbolic_link_itself_when_not_following() {
        val file = File(root, "file").apply { writeText("x") }
        val link = File(root, "link")
        Os.symlink("file", link.path)
        val targetMtime = Os.stat(file.path).st_mtim.nanos()

        val result = timestamps.set(link.toPath(), false, null, MTIME)

        assertThat(result).isEqualTo(Either.Right(Unit))
        val linkTimes = (timestamps.read(link.toPath(), false) as Either.Right<NioTimestamps.Times>).value
        assertThat(linkTimes.modificationTime.timeNanos).isEqualTo(MTIME)
        assertThat(Os.stat(file.path).st_mtim.nanos()).isEqualTo(targetMtime)
    }

    @Test
    fun updates_the_target_when_following() {
        val file = File(root, "file").apply { writeText("x") }
        val link = File(root, "link")
        Os.symlink("file", link.path)

        timestamps.set(link.toPath(), true, null, MTIME)

        assertThat(Os.stat(file.path).st_mtim.nanos()).isEqualTo(MTIME)
    }

    @Test
    fun updates_a_dangling_symbolic_link() {
        val link = File(root, "dangling")
        Os.symlink("missing", link.path)

        val result = timestamps.set(link.toPath(), false, null, MTIME)

        assertThat(result).isEqualTo(Either.Right(Unit))
        assertThat(Os.lstat(link.path).st_mtim.nanos()).isEqualTo(MTIME)
    }

    @Test
    fun handles_names_outside_the_basic_multilingual_plane() {
        val file = File(root, "file-😀").apply { writeText("x") }

        val result = timestamps.set(file.toPath(), true, null, MTIME)

        assertThat(result).isEqualTo(Either.Right(Unit))
        assertThat(Os.stat(file.path).st_mtim.nanos()).isEqualTo(MTIME)
    }

    @Test
    fun reports_a_missing_file() {
        val missing = File(root, "missing").toPath()

        assertThat((timestamps.set(missing, true, null, MTIME) as Either.Left).value).isInstanceOf(NoEntry::class)
        assertThat((timestamps.read(missing, true) as Either.Left).value).isInstanceOf(NoEntry::class)
    }

    private fun StructTimespec.nanos(): Long = tv_sec * 1_000_000_000L + tv_nsec

    private companion object {
        const val MTIME = 1_700_000_000_123_456_789L
        const val ATIME = 1_600_000_000_987_654_321L
    }
}
