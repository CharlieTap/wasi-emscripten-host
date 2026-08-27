/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber")

package at.released.weh.bindings.chasm.memory

import at.released.weh.bindings.chasm.ext.orThrow
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.Memory
import at.released.weh.wasm.core.memory.MemoryRawSink
import at.released.weh.wasm.core.memory.MemoryRawSource
import io.github.charlietap.chasm.embedding.memory.readByte
import io.github.charlietap.chasm.embedding.memory.readBytes
import io.github.charlietap.chasm.embedding.memory.readInt
import io.github.charlietap.chasm.embedding.memory.readLong
import io.github.charlietap.chasm.embedding.memory.writeByte
import io.github.charlietap.chasm.embedding.memory.writeBytes
import io.github.charlietap.chasm.embedding.memory.writeInt
import io.github.charlietap.chasm.embedding.memory.writeLong
import io.github.charlietap.chasm.embedding.shapes.Store
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import io.github.charlietap.chasm.embedding.shapes.Memory as ChasmMemory

/** Embedding-side memory access used only after instantiation, outside guest-to-host callbacks. */
internal class ChasmEmbeddingMemoryAdapter(
    private val store: Store,
    private val memory: ChasmMemory,
) : Memory {
    override fun readI8(@IntWasmPtr addr: WasmPtr): Byte = readByte(store, memory, addr).orThrow()

    override fun readI32(@IntWasmPtr addr: WasmPtr): Int = readInt(store, memory, addr).orThrow()

    override fun readI64(@IntWasmPtr addr: WasmPtr): Long = readLong(store, memory, addr).orThrow()

    override fun source(@IntWasmPtr fromAddr: WasmPtr, @IntWasmPtr toAddrExclusive: WasmPtr): RawSource =
        EmbeddingMemoryRawSource(store, memory, fromAddr, toAddrExclusive)

    override fun writeI8(@IntWasmPtr addr: WasmPtr, data: Byte) {
        writeByte(store, memory, addr, data).orThrow()
    }

    override fun writeI32(@IntWasmPtr addr: WasmPtr, data: Int) {
        writeInt(store, memory, addr, data).orThrow()
    }

    override fun writeI64(@IntWasmPtr addr: WasmPtr, data: Long) {
        writeLong(store, memory, addr, data).orThrow()
    }

    override fun sink(@IntWasmPtr fromAddr: WasmPtr, @IntWasmPtr toAddrExclusive: WasmPtr): RawSink =
        EmbeddingMemoryRawSink(store, memory, fromAddr, toAddrExclusive)
}

private class EmbeddingMemoryRawSource(
    private val store: Store,
    private val memory: ChasmMemory,
    @IntWasmPtr baseAddr: WasmPtr,
    @IntWasmPtr toAddrExclusive: WasmPtr,
) : MemoryRawSource(baseAddr, toAddrExclusive) {
    override fun readBytesFromMemory(@IntWasmPtr srcAddr: WasmPtr, sink: Buffer, readBytes: Int) {
        val buffer = ByteArray(readBytes)
        readBytes(store, memory, buffer, srcAddr, readBytes).orThrow()
        sink.write(buffer)
        sink.emit()
    }
}

private class EmbeddingMemoryRawSink(
    private val store: Store,
    private val memory: ChasmMemory,
    @IntWasmPtr baseAddr: WasmPtr,
    @IntWasmPtr toAddrExclusive: WasmPtr,
) : MemoryRawSink(baseAddr, toAddrExclusive) {
    override fun writeBytesToMemory(source: Buffer, toAddr: WasmPtr, byteCount: Long) {
        val bytes = source.readByteArray(byteCount.toInt())
        writeBytes(store, memory, toAddr, bytes).orThrow()
    }
}
