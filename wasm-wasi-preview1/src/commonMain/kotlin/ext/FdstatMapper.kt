/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("NoUnusedImports", "UnusedImports")

package at.released.weh.wasi.preview1.ext

import at.released.weh.wasi.preview1.type.Fdstat
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.writeI16
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeI64
import at.released.weh.wasm.core.memory.writeI8
import kotlinx.io.Sink
import kotlinx.io.writeLongLe
import kotlinx.io.writeShortLe

internal const val FDSTAT_PACKED_SIZE = 24

internal fun Fdstat.packTo(
    sink: Sink,
) {
    sink.writeByte(this.fsFiletype.code.toByte())
    sink.writeByte(0) // Alignment
    sink.writeShortLe(this.fsFlags)
    sink.writeShortLe(0) // Alignment
    sink.writeShortLe(0) // Alignment
    sink.writeLongLe(this.fsRightsBase)
    sink.writeLongLe(this.fsRightsInheriting)
}

context(_: MemoryAccess<M>)
internal fun <M> Fdstat.writeTo(memory: M, address: WasmPtr) {
    memory.writeI8(address, fsFiletype.code.toByte())
    memory.writeI8(address + 1, 0.toByte())
    memory.writeI16(address + 2, fsFlags)
    memory.writeI32(address + 4, 0)
    memory.writeI64(address + 8, fsRightsBase)
    memory.writeI64(address + 16, fsRightsInheriting)
}
