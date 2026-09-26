/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.wasip1

import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.embedding.function
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.expect
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.runtime.value.NumberValue
import io.github.charlietap.chasm.type.FunctionType
import io.github.charlietap.chasm.type.NumberType.I32
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChasmWasiPreview1AllocationTest {
    @Test
    fun `selecting one import allocates one store function`() {
        val store = store()
        val module = module(moduleImporting("args_get", argsGetTypeBytes())).expect("test module must decode")

        EmbedderHost().use { host ->
            ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired()
        }

        assertEquals(1, store.functionCount())
    }

    @Test
    fun `provided import does not allocate an automatic duplicate`() {
        val store = store()
        val module = module(moduleImporting("args_get", argsGetTypeBytes())).expect("test module must decode")
        val provided = Import(
            WASI_MODULE_NAME,
            "args_get",
            function(store, argsGetType(), HostFunction { _, _ -> }),
        )
        val functionCount = store.functionCount()

        EmbedderHost().use { host ->
            val automaticImports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired(providedImports = listOf(provided))

            assertEquals(emptyList(), automaticImports)
        }

        assertEquals(functionCount, store.functionCount())
    }

    @Test
    fun `later validation failure does not partially allocate store functions`() {
        val store = store()
        val module = module(
            moduleImporting(
                "args_get" to argsGetTypeBytes(),
                "proc_exit" to emptyFunctionTypeBytes(),
            ),
        ).expect("test module must decode")

        EmbedderHost().use { host ->
            assertFailsWith<IllegalArgumentException> {
                ChasmWasiPreview1Builder(store, module) {
                    this.host = host
                }.buildRequired()
            }
        }

        assertEquals(0, store.functionCount())
    }

    @Test
    fun `selected memory function can be instantiated and invoked`() {
        val store = store()
        val module = module(argsGetInvocationModule()).expect("test module must decode")

        EmbedderHost().use { host ->
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired()
            val instance = instance(store, module, imports).expect("test module must instantiate")
            val result = invoke(store, instance, "run").expect("test function must run")

            assertEquals(0, (result.single() as NumberValue.I32).value)
        }
    }
}

private fun Store.functionCount(): Int {
    val internalStoreField = Store::class.java.declaredFields.single { field ->
        field.type.name == "io.github.charlietap.chasm.runtime.store.Store"
    }
    internalStoreField.isAccessible = true
    val internalStore = internalStoreField.get(this)
    val functions = internalStore.javaClass.getMethod("getFunctions").invoke(internalStore) as List<*>
    return functions.size
}

private fun moduleImporting(entityName: String, functionType: ByteArray): ByteArray =
    moduleImporting(entityName to functionType)

private fun moduleImporting(vararg imports: Pair<String, ByteArray>): ByteArray =
    WASM_HEADER + typeSection(*imports.map { it.second }.toTypedArray()) +
        importSection(*imports.map { it.first }.toTypedArray())

private fun typeSection(vararg functionTypes: ByteArray): ByteArray = section(
    TYPE_SECTION,
    byteArrayOf(functionTypes.size.toByte()) + functionTypes.fold(ByteArray(0), ByteArray::plus),
)

private fun importSection(vararg entityNames: String): ByteArray = section(
    IMPORT_SECTION,
    byteArrayOf(entityNames.size.toByte()) + entityNames.foldIndexed(ByteArray(0)) { index, bytes, entityName ->
        bytes + name(WASI_MODULE_NAME) + name(entityName) + byteArrayOf(FUNCTION_KIND, index.toByte())
    },
)

private fun argsGetInvocationModule(): ByteArray {
    val functionBody = byteArrayOf(
        0,
        I32_CONST,
        0,
        I32_CONST,
        4,
        CALL,
        0,
        END,
    )
    return WASM_HEADER +
        typeSection(argsGetTypeBytes(), emptyToI32FunctionTypeBytes()) +
        importSection("args_get") +
        section(FUNCTION_SECTION, byteArrayOf(1, 1)) +
        section(MEMORY_SECTION, byteArrayOf(1, 0, 1)) +
        section(
            EXPORT_SECTION,
            byteArrayOf(2) +
                name("memory") + byteArrayOf(MEMORY_KIND, 0) +
                name("run") + byteArrayOf(FUNCTION_KIND, 1),
        ) +
        section(CODE_SECTION, byteArrayOf(1, functionBody.size.toByte()) + functionBody)
}

private fun argsGetTypeBytes(): ByteArray =
    byteArrayOf(FUNCTION_TYPE, 2, I32_TYPE, I32_TYPE, 1, I32_TYPE)

private fun emptyFunctionTypeBytes(): ByteArray = byteArrayOf(FUNCTION_TYPE, 0, 0)

private fun emptyToI32FunctionTypeBytes(): ByteArray = byteArrayOf(FUNCTION_TYPE, 0, 1, I32_TYPE)

private fun argsGetType(): FunctionType = FunctionType(
    params = ResultType(listOf(ValueType.Number(I32), ValueType.Number(I32))),
    results = ResultType(listOf(ValueType.Number(I32))),
)

private fun name(value: String): ByteArray {
    val bytes = value.encodeToByteArray()
    return byteArrayOf(bytes.size.toByte()) + bytes
}

private fun section(id: Byte, payload: ByteArray): ByteArray = byteArrayOf(id, payload.size.toByte()) + payload

private const val WASI_MODULE_NAME: String = "wasi_snapshot_preview1"
private const val TYPE_SECTION: Byte = 1
private const val IMPORT_SECTION: Byte = 2
private const val FUNCTION_SECTION: Byte = 3
private const val MEMORY_SECTION: Byte = 5
private const val EXPORT_SECTION: Byte = 7
private const val CODE_SECTION: Byte = 10
private const val FUNCTION_TYPE: Byte = 0x60
private const val FUNCTION_KIND: Byte = 0
private const val MEMORY_KIND: Byte = 2
private const val I32_TYPE: Byte = 0x7f
private const val I32_CONST: Byte = 0x41
private const val CALL: Byte = 0x10
private const val END: Byte = 0x0b
private val WASM_HEADER: ByteArray = byteArrayOf(0, 0x61, 0x73, 0x6d, 1, 0, 0, 0)
