/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber", "NoUnusedImports", "UnusedImports")

package at.released.weh.wasi.preview1.ext

import at.released.weh.filesystem.op.poll.FileDescriptorEventType
import at.released.weh.wasi.preview1.type.Event
import at.released.weh.wasi.preview1.type.EventFdReadwrite
import at.released.weh.wasi.preview1.type.EventrwflagsFlag.FD_READWRITE_HANGUP
import at.released.weh.wasi.preview1.type.Eventtype
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.writeI16
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeI64
import at.released.weh.wasm.core.memory.writeI8
import kotlinx.io.Sink
import kotlinx.io.writeIntLe
import kotlinx.io.writeLongLe
import kotlinx.io.writeShortLe
import at.released.weh.filesystem.op.poll.Event as FileSystemEvent

internal object EventMapper {
    internal const val EVENT_PACKED_SIZE = 32
    internal const val EVENT_FD_READWRITE_PACKED_SIZE = 16
    private val DUMMY_EVENT_FD_READWRITE = EventFdReadwrite(0, 0)

    internal fun Event.packTo(
        sink: Sink,
    ) {
        sink.writeLongLe(this.userdata)
        sink.writeShortLe(this.error.code.toShort())
        sink.writeByte(this.type.code.toByte())
        sink.writeByte(0) // alignment
        sink.writeIntLe(0) // alignment

        when (type) {
            Eventtype.CLOCK -> DUMMY_EVENT_FD_READWRITE.packTo(sink)
            Eventtype.FD_READ, Eventtype.FD_WRITE -> fdReadwrite.packTo(sink)
        }
    }

    context(_: MemoryAccess<M>)
    internal fun <M> Event.writeTo(memory: M, address: WasmPtr) {
        memory.writeI64(address, userdata)
        memory.writeI16(address + 8, error.code.toShort())
        memory.writeI8(address + 10, type.code.toByte())
        memory.writeI8(address + 11, 0.toByte())
        memory.writeI32(address + 12, 0)
        val readwrite = if (type == Eventtype.CLOCK) DUMMY_EVENT_FD_READWRITE else fdReadwrite
        memory.writeI64(address + 16, readwrite.nbytes)
        memory.writeI16(address + 24, readwrite.flags)
        memory.writeI16(address + 26, 0.toShort())
        memory.writeI32(address + 28, 0)
    }

    context(_: MemoryAccess<M>)
    internal fun <M> FileSystemEvent.writeTo(memory: M, address: WasmPtr) {
        val eventType: Eventtype
        val bytesAvailable: Long
        val flags: Short
        when (this) {
            is FileSystemEvent.ClockEvent -> {
                eventType = Eventtype.CLOCK
                bytesAvailable = 0
                flags = 0
            }

            is FileSystemEvent.FileDescriptorEvent -> {
                eventType = when (type) {
                    FileDescriptorEventType.READ -> Eventtype.FD_READ
                    FileDescriptorEventType.WRITE -> Eventtype.FD_WRITE
                }
                bytesAvailable = this.bytesAvailable
                flags = if (isHangup) FD_READWRITE_HANGUP else 0
            }
        }

        memory.writeI64(address, userdata)
        memory.writeI16(address + 8, errno.toWasiErrno().code.toShort())
        memory.writeI8(address + 10, eventType.code.toByte())
        memory.writeI8(address + 11, 0)
        memory.writeI32(address + 12, 0)
        memory.writeI64(address + 16, bytesAvailable)
        memory.writeI16(address + 24, flags)
        memory.writeI16(address + 26, 0)
        memory.writeI32(address + 28, 0)
    }

    private fun EventFdReadwrite.packTo(
        sink: Sink,
    ) {
        sink.writeLongLe(nbytes)
        sink.writeShortLe(flags)
        sink.writeShortLe(0) // alignment
        sink.writeIntLe(0) // alignment
    }

    internal fun fromFilesystemEvent(event: FileSystemEvent): Event = when (event) {
        is FileSystemEvent.ClockEvent -> Event(
            userdata = event.userdata,
            error = event.errno.toWasiErrno(),
            type = Eventtype.CLOCK,
            fdReadwrite = DUMMY_EVENT_FD_READWRITE,
        )

        is FileSystemEvent.FileDescriptorEvent -> Event(
            userdata = event.userdata,
            error = event.errno.toWasiErrno(),
            type = when (event.type) {
                FileDescriptorEventType.READ -> Eventtype.FD_READ
                FileDescriptorEventType.WRITE -> Eventtype.FD_WRITE
            },
            fdReadwrite = EventFdReadwrite(
                nbytes = event.bytesAvailable,
                flags = if (event.isHangup) {
                    FD_READWRITE_HANGUP
                } else {
                    0.toShort()
                },
            ),
        )
    }
}
