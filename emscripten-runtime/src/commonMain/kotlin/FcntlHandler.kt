/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.emcripten.runtime

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import at.released.weh.emcripten.runtime.ext.WhenceMapper
import at.released.weh.emcripten.runtime.ext.negativeErrnoCode
import at.released.weh.emcripten.runtime.include.Fcntl
import at.released.weh.emcripten.runtime.include.StructFlock
import at.released.weh.filesystem.FileSystem
import at.released.weh.filesystem.error.InvalidArgument
import at.released.weh.filesystem.model.FileDescriptor
import at.released.weh.filesystem.model.FileSystemErrno.Companion.wasiPreview1Code
import at.released.weh.filesystem.model.IntFileDescriptor
import at.released.weh.filesystem.op.lock.AddAdvisoryLockFd
import at.released.weh.filesystem.op.lock.Advisorylock
import at.released.weh.filesystem.op.lock.AdvisorylockLockType
import at.released.weh.filesystem.op.lock.RemoveAdvisoryLockFd
import at.released.weh.wasi.preview1.type.Errno.INVAL
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.readI16
import at.released.weh.wasm.core.memory.readI32
import at.released.weh.wasm.core.memory.readI64
import at.released.weh.wasm.core.memory.readPtr

internal class FcntlHandler(
    private val fileSystem: FileSystem,
) {
    @Suppress("MagicNumber")
    fun <M> invoke(
        memory: M,
        @IntFileDescriptor fd: FileDescriptor,
        operation: UInt,
        thirdArg: Int?,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Int = with(memoryAccess) {
        if (operation != Fcntl.F_SETLK) return -INVAL.code

        @IntWasmPtr(StructFlock::class)
        val structStatPtr: WasmPtr = memory.readPtr(checkNotNull(thirdArg))
        val flock = StructFlock(
            l_type = memory.readI16(structStatPtr),
            l_whence = memory.readI16(structStatPtr + 2),
            l_start = memory.readI64(structStatPtr + 8),
            l_len = memory.readI64(structStatPtr + 16),
            l_pid = memory.readI32(structStatPtr + 24),
        )
        val advisoryLock = flock.toAdvisoryLock().getOrElse {
            return -it.errno.wasiPreview1Code
        }
        return when (flock.l_type) {
            Fcntl.F_RDLCK, Fcntl.F_WRLCK -> fileSystem.execute(
                AddAdvisoryLockFd,
                AddAdvisoryLockFd(fd, advisoryLock),
            ).negativeErrnoCode()

            Fcntl.F_UNLCK -> fileSystem.execute(
                RemoveAdvisoryLockFd,
                RemoveAdvisoryLockFd(fd, advisoryLock),
            ).negativeErrnoCode()

            else -> -INVAL.code
        }
    }

    private companion object {
        fun StructFlock.toAdvisoryLock(): Either<InvalidArgument, Advisorylock> {
            val type = when (this.l_type) {
                Fcntl.F_RDLCK -> AdvisorylockLockType.READ
                Fcntl.F_WRLCK -> AdvisorylockLockType.WRITE
                Fcntl.F_UNLCK -> AdvisorylockLockType.WRITE
                else -> return InvalidArgument("Incorrect l_type").left()
            }
            val whence = WhenceMapper.fromEmscriptenIdOrNull(this.l_whence.toInt())
                ?: return InvalidArgument("Incorrect whence `${this.l_whence}`").left()

            return Advisorylock(
                type = type,
                whence = whence,
                start = this.l_start,
                length = this.l_len,
            ).right()
        }
    }
}
