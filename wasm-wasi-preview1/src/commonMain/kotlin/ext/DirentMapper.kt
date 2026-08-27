/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber", "NoUnusedImports", "UnusedImports")

package at.released.weh.wasi.preview1.ext

import at.released.weh.wasi.preview1.type.Dirent
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeI64
import kotlinx.io.Sink
import kotlinx.io.writeIntLe
import kotlinx.io.writeLongLe

internal const val DIRENT_PACKED_SIZE = 24

internal fun Dirent.packTo(
    sink: Sink,
) {
    sink.writeLongLe(this.dNext)
    sink.writeLongLe(this.dIno)
    sink.writeIntLe(this.dNamlen)
    sink.writeIntLe(this.dType.code)
}

context(_: MemoryAccess<M>)
internal fun <M> Dirent.writeTo(memory: M, address: WasmPtr) {
    memory.writeI64(address, dNext)
    memory.writeI64(address + 8, dIno)
    memory.writeI32(address + 16, dNamlen)
    memory.writeI32(address + 20, dType.code)
}
