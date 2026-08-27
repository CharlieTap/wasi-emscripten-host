/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress(
    "MagicNumber",
    "ConstructorParameterNaming",
    "TYPEALIAS_NAME_INCORRECT_CASE",
    "NoUnusedImports",
    "UnusedImports",
)

package at.released.weh.emcripten.runtime.include.sys

import at.released.weh.emcripten.runtime.include.sys.FileTypeFlag.fileTypeToFileMode
import at.released.weh.filesystem.op.stat.StructStat
import at.released.weh.filesystem.op.stat.timeMillis
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.writeI32
import at.released.weh.wasm.core.memory.writeI64
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.writeIntLe
import kotlinx.io.writeLongLe

internal fun StructStat.packTo(sink: Sink): Unit = sink.run {
    writeIntLe(deviceId.toInt()) // 0
    writeIntLe(modeType) // 4
    writeIntLe(links.toInt()) // 8
    writeIntLe(usedId.toInt()) // 12
    writeIntLe(groupId.toInt()) // 16
    writeIntLe(specialFileDeviceId.toInt()) // 20
    writeLongLe(size) // 24
    writeIntLe(4096) // 32
    writeIntLe(blocks.toInt()) // 36

    accessTime.timeMillis.let {
        writeLongLe((it / 1000)) // 40
        writeIntLe((1000 * (it % 1000)).toInt()) // 48
        writeIntLe(0) // 52, padding
    }
    modificationTime.timeMillis.let {
        writeLongLe((it / 1000)) // 56
        writeIntLe((1000 * (it % 1000)).toInt()) // 64
        writeIntLe(0) // 68, padding
    }
    changeStatusTime.timeMillis.let {
        writeLongLe((it / 1000)) // 72
        writeIntLe((1000 * (it % 1000)).toInt()) // 80
        writeIntLe(0) // 84, padding
    }
    writeLongLe(inode) // 88
}

context(memoryAccess: MemoryAccess<M>)
internal fun <M> StructStat.writeTo(memory: M, address: WasmPtr) {
    memory.writeI32(address, deviceId.toInt())
    memory.writeI32(address + 4, modeType)
    memory.writeI32(address + 8, links.toInt())
    memory.writeI32(address + 12, usedId.toInt())
    memory.writeI32(address + 16, groupId.toInt())
    memory.writeI32(address + 20, specialFileDeviceId.toInt())
    memory.writeI64(address + 24, size)
    memory.writeI32(address + 32, 4096)
    memory.writeI32(address + 36, blocks.toInt())
    writeTimespec(accessTime.timeMillis, memory, address + 40)
    writeTimespec(modificationTime.timeMillis, memory, address + 56)
    writeTimespec(changeStatusTime.timeMillis, memory, address + 72)
    memory.writeI64(address + 88, inode)
}

context(memoryAccess: MemoryAccess<M>)
private fun <M> writeTimespec(timeMillis: Long, memory: M, address: WasmPtr) {
    memory.writeI64(address, timeMillis / 1000)
    memory.writeI32(address + 8, (1000 * (timeMillis % 1000)).toInt())
    memory.writeI32(address + 12, 0)
}

internal const val STRUCT_SIZE_PACKED_SIZE: Int = 96

internal fun StructStat.pack(): Buffer = Buffer().also {
    packTo(it)
    check(it.size == STRUCT_SIZE_PACKED_SIZE.toLong())
}

internal val StructStat.modeType: Int get() = mode or fileTypeToFileMode(type)
