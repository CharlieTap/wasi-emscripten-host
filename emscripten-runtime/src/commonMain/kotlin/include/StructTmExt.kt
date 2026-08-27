/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber", "NoUnusedImports", "UnusedImports")

package at.released.weh.emcripten.runtime.include

import at.released.weh.host.LocalTimeFormatter.StructTm
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.writeI32
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.writeIntLe

internal const val STRUCT_TM_PACKED_SIZE: Int = 40

internal fun StructTm.packTo(sink: Sink): Unit = sink.run {
    writeIntLe(tm_sec) // 0
    writeIntLe(tm_min) // 4
    writeIntLe(tm_hour) // 8
    writeIntLe(tm_mday) // 12
    writeIntLe(tm_mon) // 16
    writeIntLe(tm_year) // 20
    writeIntLe(tm_wday) // 24
    writeIntLe(tm_yday) // 28
    writeIntLe(tm_isdst) // 32
    writeIntLe(tm_gmtoff.toInt()) // 36
}

context(_: MemoryAccess<M>)
internal fun <M> StructTm.writeTo(memory: M, address: WasmPtr) {
    memory.writeI32(address, tm_sec)
    memory.writeI32(address + 4, tm_min)
    memory.writeI32(address + 8, tm_hour)
    memory.writeI32(address + 12, tm_mday)
    memory.writeI32(address + 16, tm_mon)
    memory.writeI32(address + 20, tm_year)
    memory.writeI32(address + 24, tm_wday)
    memory.writeI32(address + 28, tm_yday)
    memory.writeI32(address + 32, tm_isdst)
    memory.writeI32(address + 36, tm_gmtoff.toInt())
}

internal fun StructTm.pack(): Buffer = Buffer().also {
    packTo(it)
    check(it.size == STRUCT_TM_PACKED_SIZE.toLong())
}
