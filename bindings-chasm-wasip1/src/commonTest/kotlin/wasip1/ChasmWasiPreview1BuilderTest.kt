/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.wasip1

import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.embedding.function
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.expect
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.type.FunctionType
import io.github.charlietap.chasm.type.NumberType.I32
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChasmWasiPreview1BuilderTest {
    @Test
    fun `build creates the complete host`() {
        val module = module(wasmModule(memorySection())).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.build()

            assertEquals(46, imports.size)
            assertTrue(imports.any { import -> import.entityName == "proc_exit" })
        } finally {
            host.close()
        }
    }

    @Test
    fun `build required creates only module imports in module order`() {
        val module = module(
            wasmModule(
                typeSection(
                    functionType(listOf(I32_TYPE), emptyList()),
                    functionType(listOf(I32_TYPE, I32_TYPE), listOf(I32_TYPE)),
                ),
                importFunctionSection(
                    functionImport("proc_exit", typeIndex = 0),
                    functionImport("args_get", typeIndex = 1),
                ),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired()

            assertEquals(listOf("proc_exit", "args_get"), imports.map(Import::entityName))
        } finally {
            host.close()
        }
    }

    @Test
    fun `build required returns empty when module has no wasi imports`() {
        val module = module(wasmModule(memorySection())).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired()

            assertTrue(imports.isEmpty())
        } finally {
            host.close()
        }
    }

    @Test
    fun `provided import suppresses matching automatic import`() {
        val module = module(
            wasmModule(
                typeSection(
                    functionType(listOf(I32_TYPE, I32_TYPE), listOf(I32_TYPE)),
                    functionType(listOf(I32_TYPE), emptyList()),
                ),
                importFunctionSection(
                    functionImport("args_get", typeIndex = 0),
                    functionImport("proc_exit", typeIndex = 1),
                ),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()
        val provided = Import(
            WASI_MODULE_NAME,
            "args_get",
            function(store, argsGetType(), HostFunction { _, _ -> }),
        )

        try {
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired(providedImports = listOf(provided))

            assertEquals(listOf("proc_exit"), imports.map(Import::entityName))
        } finally {
            host.close()
        }
    }

    @Test
    fun `provided import from another namespace does not suppress wasi import`() {
        val module = module(
            wasmModule(
                typeSection(functionType(listOf(I32_TYPE, I32_TYPE), listOf(I32_TYPE))),
                importFunctionSection(functionImport("args_get", typeIndex = 0)),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()
        val provided = Import(
            "env",
            "args_get",
            function(store, argsGetType(), HostFunction { _, _ -> }),
        )

        try {
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired(providedImports = listOf(provided))

            assertEquals(listOf("args_get"), imports.map(Import::entityName))
        } finally {
            host.close()
        }
    }

    @Test
    fun `build required can select proc exit independently`() {
        val module = module(
            wasmModule(
                typeSection(functionType(listOf(I32_TYPE), emptyList())),
                importFunctionSection(functionImport("proc_exit", typeIndex = 0)),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired()

            assertEquals(listOf("proc_exit"), imports.map(Import::entityName))
        } finally {
            host.close()
        }
    }

    @Test
    fun `compatible duplicate imports allocate one host function`() {
        val module = module(
            wasmModule(
                typeSection(functionType(listOf(I32_TYPE, I32_TYPE), listOf(I32_TYPE))),
                importFunctionSection(
                    functionImport("args_get", typeIndex = 0),
                    functionImport("args_get", typeIndex = 0),
                ),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired()

            assertEquals(listOf("args_get"), imports.map(Import::entityName))
        } finally {
            host.close()
        }
    }

    @Test
    fun `conflicting duplicate imports are rejected`() {
        val module = module(
            wasmModule(
                typeSection(
                    functionType(listOf(I32_TYPE, I32_TYPE), listOf(I32_TYPE)),
                    functionType(emptyList(), emptyList()),
                ),
                importFunctionSection(
                    functionImport("args_get", typeIndex = 0),
                    functionImport("args_get", typeIndex = 1),
                ),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val failure = assertFailsWith<IllegalStateException> {
                ChasmWasiPreview1Builder(store, module) {
                    this.host = host
                }.buildRequired()
            }

            assertTrue(failure.message.orEmpty().contains("Conflicting WASI Preview 1 imports"))
        } finally {
            host.close()
        }
    }

    @Test
    fun `provided import does not hide conflicting module definitions`() {
        val module = module(
            wasmModule(
                typeSection(
                    functionType(listOf(I32_TYPE, I32_TYPE), listOf(I32_TYPE)),
                    functionType(emptyList(), emptyList()),
                ),
                importFunctionSection(
                    functionImport("args_get", typeIndex = 0),
                    functionImport("args_get", typeIndex = 1),
                ),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()
        val provided = Import(
            WASI_MODULE_NAME,
            "args_get",
            function(store, argsGetType(), HostFunction { _, _ -> }),
        )

        try {
            assertFailsWith<IllegalStateException> {
                ChasmWasiPreview1Builder(store, module) {
                    this.host = host
                }.buildRequired(providedImports = listOf(provided))
            }
        } finally {
            host.close()
        }
    }

    @Test
    fun `unsupported wasi function is rejected`() {
        val module = module(
            wasmModule(
                typeSection(functionType(emptyList(), emptyList())),
                importFunctionSection(functionImport("unsupported", typeIndex = 0)),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val failure = assertFailsWith<IllegalStateException> {
                ChasmWasiPreview1Builder(store, module) {
                    this.host = host
                }.buildRequired()
            }

            assertTrue(failure.message.orEmpty().contains("wasi_snapshot_preview1.unsupported"))
        } finally {
            host.close()
        }
    }

    @Test
    fun `wasi function with wrong signature is rejected`() {
        val module = module(
            wasmModule(
                typeSection(functionType(emptyList(), emptyList())),
                importFunctionSection(functionImport("args_get", typeIndex = 0)),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val failure = assertFailsWith<IllegalArgumentException> {
                ChasmWasiPreview1Builder(store, module) {
                    this.host = host
                }.buildRequired()
            }

            assertTrue(failure.message.orEmpty().contains("expected"))
        } finally {
            host.close()
        }
    }

    @Test
    fun `proc exit with wrong signature is rejected`() {
        val module = module(
            wasmModule(
                typeSection(functionType(emptyList(), emptyList())),
                importFunctionSection(functionImport("proc_exit", typeIndex = 0)),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val failure = assertFailsWith<IllegalArgumentException> {
                ChasmWasiPreview1Builder(store, module) {
                    this.host = host
                }.buildRequired()
            }

            assertTrue(failure.message.orEmpty().contains("expected"))
        } finally {
            host.close()
        }
    }

    @Test
    fun `non function wasi import is rejected`() {
        val module = module(
            wasmModule(
                importGlobalSection("args_get"),
                memorySection(),
            ),
        ).expect("test module must decode")
        val store = store()
        val host = EmbedderHost()

        try {
            val failure = assertFailsWith<IllegalArgumentException> {
                ChasmWasiPreview1Builder(store, module) {
                    this.host = host
                }.buildRequired()
            }

            assertTrue(failure.message.orEmpty().contains("expected"))
        } finally {
            host.close()
        }
    }

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

private data class TestFunctionType(
    val params: List<Byte>,
    val results: List<Byte>,
)

private data class TestFunctionImport(
    val name: String,
    val typeIndex: Int,
)

private fun functionType(
    params: List<Byte>,
    results: List<Byte>,
): TestFunctionType = TestFunctionType(params, results)

private fun functionImport(
    name: String,
    typeIndex: Int,
): TestFunctionImport = TestFunctionImport(name, typeIndex)

private fun typeSection(vararg types: TestFunctionType): ByteArray {
    val payload = byteArrayOf(types.size.toByte()) + types.fold(ByteArray(0)) { bytes, type ->
        bytes + byteArrayOf(FUNCTION_TYPE, type.params.size.toByte()) + type.params.toByteArray() +
            byteArrayOf(type.results.size.toByte()) + type.results.toByteArray()
    }
    return section(TYPE_SECTION, payload)
}

private fun importFunctionSection(vararg imports: TestFunctionImport): ByteArray {
    val payload = byteArrayOf(imports.size.toByte()) + imports.fold(ByteArray(0)) { bytes, import ->
        bytes + name(WASI_MODULE_NAME) + name(import.name) + byteArrayOf(FUNCTION_KIND, import.typeIndex.toByte())
    }
    return section(IMPORT_SECTION, payload)
}

private fun importGlobalSection(entityName: String): ByteArray {
    val payload = byteArrayOf(1) +
        name(WASI_MODULE_NAME) +
        name(entityName) +
        byteArrayOf(GLOBAL_KIND, I32_TYPE, IMMUTABLE)
    return section(IMPORT_SECTION, payload)
}

private fun argsGetType(): FunctionType = FunctionType(
    params = ResultType(listOf(ValueType.Number(I32), ValueType.Number(I32))),
    results = ResultType(listOf(ValueType.Number(I32))),
)

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
private const val TYPE_SECTION: Byte = 1
private const val MEMORY_SECTION: Byte = 5
private const val GLOBAL_SECTION: Byte = 6
private const val EXPORT_SECTION: Byte = 7
private const val FUNCTION_KIND: Byte = 0
private const val MEMORY_KIND: Byte = 2
private const val GLOBAL_KIND: Byte = 3
private const val IMMUTABLE: Byte = 0
private const val FUNCTION_TYPE: Byte = 0x60
private const val I32_TYPE: Byte = 0x7F
private const val I32_CONST: Byte = 0x41
private const val END: Byte = 0x0B
private const val WASI_MODULE_NAME: String = "wasi_snapshot_preview1"
