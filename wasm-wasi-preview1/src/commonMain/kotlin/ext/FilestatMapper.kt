/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber", "NoUnusedImports", "UnusedImports")

package at.released.weh.wasi.preview1.ext

import at.released.weh.filesystem.op.stat.StructStat
import at.released.weh.filesystem.op.stat.timeNanos
import at.released.weh.wasi.preview1.type.Filestat
import at.released.weh.wasi.preview1.type.Filetype
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeI64
import kotlinx.io.Sink
import kotlinx.io.writeIntLe
import kotlinx.io.writeLongLe

internal const val FILESTAT_PACKED_SIZE = 64

internal fun Filestat.packTo(
    sink: Sink,
) {
    sink.writeLongLe(this.dev)
    sink.writeLongLe(this.ino)
    sink.writeIntLe(this.filetype.code)
    sink.writeIntLe(0) // Alignment
    sink.writeLongLe(this.nlink)
    sink.writeLongLe(this.size)
    sink.writeLongLe(this.atim)
    sink.writeLongLe(this.mtim)
    sink.writeLongLe(this.ctim)
}

context(_: MemoryAccess<M>)
internal fun <M> Filestat.writeTo(memory: M, address: WasmPtr) {
    memory.writeI64(address, dev)
    memory.writeI64(address + 8, ino)
    memory.writeI32(address + 16, filetype.code)
    memory.writeI32(address + 20, 0)
    memory.writeI64(address + 24, nlink)
    memory.writeI64(address + 32, size)
    memory.writeI64(address + 40, atim)
    memory.writeI64(address + 48, mtim)
    memory.writeI64(address + 56, ctim)
}

context(_: MemoryAccess<M>)
internal fun <M> StructStat.writeTo(memory: M, address: WasmPtr) {
    val wasiFiletype = checkNotNull(Filetype.fromCode(type.id)) { "Unexpected type ${type.id}" }
    memory.writeI64(address, deviceId)
    memory.writeI64(address + 8, inode)
    memory.writeI32(address + 16, wasiFiletype.code)
    memory.writeI32(address + 20, 0)
    memory.writeI64(address + 24, links)
    memory.writeI64(address + 32, size)
    memory.writeI64(address + 40, accessTime.timeNanos)
    memory.writeI64(address + 48, modificationTime.timeNanos)
    memory.writeI64(address + 56, changeStatusTime.timeNanos)
}

internal fun StructStat.toFilestat(): Filestat = Filestat(
    dev = this.deviceId,
    ino = this.inode,
    filetype = checkNotNull(Filetype.fromCode(this.type.id)) {
        "Unexpected type ${this.type.id}"
    },
    nlink = this.links,
    size = this.size,
    atim = this.accessTime.timeNanos,
    mtim = this.modificationTime.timeNanos,
    ctim = this.changeStatusTime.timeNanos,
)
