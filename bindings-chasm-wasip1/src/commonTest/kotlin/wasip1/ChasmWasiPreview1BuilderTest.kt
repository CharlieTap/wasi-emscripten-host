/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.wasip1

import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.expect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChasmWasiPreview1BuilderTest {
    @Test
    fun `resolves exported memory at nonzero module memory index`() {
        val module = module(
            wasmModule(
                memorySection(memoryCount = 2),
                exportSection(name = "memory", kind = MEMORY_KIND, index = 1),
            ),
        ).expect("test module must decode")

        assertEquals(1, module.wasiMemoryIndex().index)
    }

    @Test
    fun `resolves imported and re-exported memory`() {
        val module = module(
            wasmModule(
                importMemorySection(moduleName = "env", fieldName = "linear"),
                exportSection(name = "memory", kind = MEMORY_KIND, index = 0),
            ),
        ).expect("test module must decode")

        assertEquals(0, module.wasiMemoryIndex().index)
    }

    @Test
    fun `falls back to memory index zero when memory export is missing`() {
        val module = module(wasmModule(memorySection())).expect("test module must decode")

        assertEquals(0, module.wasiMemoryIndex().index)
    }

    @Test
    fun `rejects memory name exported as another kind`() {
        val module = module(
            wasmModule(
                globalSection(),
                exportSection(name = "memory", kind = GLOBAL_KIND, index = 0),
            ),
        ).expect("test module must decode")

        val failure = assertFailsWith<IllegalStateException> { module.wasiMemoryIndex() }

        assertTrue(failure.message.orEmpty().contains("must be a memory"))
    }
}

private fun wasmModule(vararg sections: ByteArray): ByteArray =
    WASM_HEADER + sections.fold(ByteArray(0), ByteArray::plus)

private fun memorySection(memoryCount: Int = 1): ByteArray {
    val payload = byteArrayOf(memoryCount.toByte()) + ByteArray(memoryCount * 2) { index ->
        if (index % 2 == 0) 0 else 1
    }
    return section(MEMORY_SECTION, payload)
}

private fun importMemorySection(moduleName: String, fieldName: String): ByteArray {
    val payload = byteArrayOf(1) +
            name(moduleName) +
            name(fieldName) +
            byteArrayOf(MEMORY_KIND, 0, 1)
    return section(IMPORT_SECTION, payload)
}

private fun globalSection(): ByteArray = section(
    GLOBAL_SECTION,
    byteArrayOf(1, I32_TYPE, 0, I32_CONST, 0, END),
)

private fun exportSection(name: String, kind: Byte, index: Int): ByteArray = section(
    EXPORT_SECTION,
    byteArrayOf(1) + name(name) + byteArrayOf(kind, index.toByte()),
)

private fun name(value: String): ByteArray {
    val bytes = value.encodeToByteArray()
    return byteArrayOf(bytes.size.toByte()) + bytes
}

private fun section(id: Byte, payload: ByteArray): ByteArray =
    byteArrayOf(id, payload.size.toByte()) + payload

private val WASM_HEADER = byteArrayOf(0, 0x61, 0x73, 0x6D, 1, 0, 0, 0)
private const val IMPORT_SECTION: Byte = 2
private const val MEMORY_SECTION: Byte = 5
private const val GLOBAL_SECTION: Byte = 6
private const val EXPORT_SECTION: Byte = 7
private const val MEMORY_KIND: Byte = 2
private const val GLOBAL_KIND: Byte = 3
private const val I32_TYPE: Byte = 0x7F
private const val I32_CONST: Byte = 0x41
private const val END: Byte = 0x0B
