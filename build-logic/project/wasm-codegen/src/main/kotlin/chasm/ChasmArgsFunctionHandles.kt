/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.wasm.codegen.chasm

import at.released.weh.gradle.wasm.codegen.chasm.classname.ChasmBindingsClassname
import at.released.weh.gradle.wasm.codegen.chasm.classname.ChasmShapesClassname
import at.released.weh.gradle.wasm.codegen.util.WasiFunctionHandlerProperty
import at.released.weh.gradle.wasm.codegen.util.WasiFunctionHandlersExt.NO_MEMORY_FUNCTIONS
import at.released.weh.gradle.wasm.codegen.util.WasiFunctionHandlersExt.WASI_MEMORY_READER_FUNCTIONS
import at.released.weh.gradle.wasm.codegen.util.WasiFunctionHandlersExt.WASI_MEMORY_WRITER_FUNCTIONS
import at.released.weh.gradle.wasm.codegen.util.toCamelCasePropertyName
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.HANDLE
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.POINTER
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.S16
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.S32
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.S64
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.S8
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.U16
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.U32
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.U64
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.WasiBaseWasmType.U8
import at.released.weh.gradle.wasm.codegen.witx.parser.model.Identifier
import at.released.weh.gradle.wasm.codegen.witx.parser.model.WasiFunc
import at.released.weh.gradle.wasm.codegen.witx.parser.model.WasiType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

internal class ChasmArgsFunctionHandles(
    wasiTypes: Map<Identifier, WasiType>,
    private val wasiFunctions: List<WasiFunc>,
) {
    private val baseTypeResolver = WasiBaseTypeResolver(wasiTypes)

    fun getFunctionHandles(): List<WasiFunctionHandle> {
        return wasiFunctions.map { wasiFunc: WasiFunc ->
            WasiFunctionHandle(
                func = wasiFunc,
                chasmHostFunctionName = wasiFunc.export.toCamelCasePropertyName(),
            )
        }.sortedBy { it.func.export }
    }

    inner class WasiFunctionHandle(
        val func: WasiFunc,
        val chasmHostFunctionName: String,
    ) {
        val handleProperty = WasiFunctionHandlerProperty(func)

        fun chasmHostFunctionDeclaration(): FunSpec =
            FunSpec.builder(chasmHostFunctionName).apply {
                returns(ChasmShapesClassname.HOST_FUNCTION)
                addStatement("val %N = %T(host)", handleProperty.propertyName, handleProperty.className)
                if (func.export in WASI_MEMORY_READER_FUNCTIONS) {
                    addStatement("val wasiMemoryReader = checkNotNull(this.wasiMemoryReader)")
                }
                if (func.export in WASI_MEMORY_WRITER_FUNCTIONS) {
                    addStatement("val wasiMemoryWriter = checkNotNull(this.wasiMemoryWriter)")
                }
                addCode("return %L\n", buildCallback())
            }.build()

        @Suppress("CyclomaticComplexMethod")
        private fun buildCallback(): CodeBlock = CodeBlock.builder().apply {
            add("%T { parameters, results ->\n", ChasmShapesClassname.HOST_FUNCTION)
            indent()

            val args = baseTypeResolver.getFuncInputArgs(func)
            args.forEachIndexed { index, (baseType: WasiBaseWasmType, _, comment) ->
                val read = when (baseType) {
                    S64, U64 -> ChasmShapesClassname.READ_I64
                    else -> ChasmShapesClassname.READ_I32
                }
                val conversion = when (baseType) {
                    S8, U8 -> ".toByte()"
                    S16, U16 -> ".toShort()"
                    POINTER, S32, U32, HANDLE, S64, U64 -> ""
                }
                add("val arg%L = parameters.%M(%L)%L // %L\n", index, read, index, conversion, comment)
            }

            val hasMemory = func.export !in NO_MEMORY_FUNCTIONS
            if (hasMemory) {
                add("val errno = %M(memoryIndex) {\n", ChasmShapesClassname.WITH_MEMORY)
                indent()
            } else {
                add("val errno = ")
            }

            val allArgs = buildList<String> {
                if (func.export in WASI_MEMORY_READER_FUNCTIONS) add("wasiMemoryReader")
                if (func.export in WASI_MEMORY_WRITER_FUNCTIONS) add("wasiMemoryWriter")
                args.indices.forEach { add("arg$it") }
            }
            val executeFunction = if (
                func.export in WASI_MEMORY_READER_FUNCTIONS || func.export in WASI_MEMORY_WRITER_FUNCTIONS
            ) {
                "executeDirect"
            } else {
                "execute"
            }
            add("%N.%L(\n", handleProperty.propertyName, executeFunction)
            indent()
            if (hasMemory) {
                add("this,\n")
            }
            allArgs.forEach { add("%N,\n", it) }
            if (hasMemory) {
                add("memoryAccess = %T,\n", ChasmBindingsClassname.CHASM_MEMORY_ACCESS)
            }
            unindent()
            add(")\n")

            if (hasMemory) {
                unindent()
                add("}\n")
            }
            if (func.result != null) {
                add("results.%M(0, errno.code)\n", ChasmShapesClassname.WRITE_I32)
            }
            unindent()
            add("}")
        }.build()
    }
}
