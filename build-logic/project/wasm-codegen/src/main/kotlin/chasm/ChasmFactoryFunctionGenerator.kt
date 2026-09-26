/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.wasm.codegen.chasm

import at.released.weh.gradle.wasm.codegen.chasm.classname.ChasmShapesClassname
import at.released.weh.gradle.wasm.codegen.chasm.classname.ChasmShapesClassname.AstType
import at.released.weh.gradle.wasm.codegen.util.WasiFunctionHandlersExt.WASI_MEMORY_READER_FUNCTIONS
import at.released.weh.gradle.wasm.codegen.util.WasiFunctionHandlersExt.WASI_MEMORY_WRITER_FUNCTIONS
import at.released.weh.gradle.wasm.codegen.util.classname.WehHostClassname
import at.released.weh.gradle.wasm.codegen.util.toCamelCasePropertyName
import at.released.weh.gradle.wasm.codegen.witx.helper.BaseFunctionType
import at.released.weh.gradle.wasm.codegen.witx.helper.BaseFunctionType.BaseWebAssemblyType
import at.released.weh.gradle.wasm.codegen.witx.helper.BaseFunctionType.BaseWebAssemblyType.I32
import at.released.weh.gradle.wasm.codegen.witx.helper.BaseFunctionType.BaseWebAssemblyType.I64
import at.released.weh.gradle.wasm.codegen.witx.helper.BaseFunctionType.Companion.getBaseFunctionTypes
import at.released.weh.gradle.wasm.codegen.witx.helper.BaseFunctionType.Companion.wasmType
import at.released.weh.gradle.wasm.codegen.witx.helper.BaseFunctionType.ListOfBaseWebAssemblyTypes.listOfTypesComparator
import at.released.weh.gradle.wasm.codegen.witx.helper.BaseFunctionType.ListOfBaseWebAssemblyTypes.listPropertyName
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver
import at.released.weh.gradle.wasm.codegen.witx.helper.WasiBaseTypeResolver.NamedParamType
import at.released.weh.gradle.wasm.codegen.witx.parser.model.Identifier
import at.released.weh.gradle.wasm.codegen.witx.parser.model.WasiFunc
import at.released.weh.gradle.wasm.codegen.witx.parser.model.WasiType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING

internal class ChasmFactoryFunctionGenerator(
    wasiTypenames: Map<Identifier, WasiType>,
    private val wasiFunctions: List<WasiFunc>,
    private val functionsClassName: ClassName,
    private val factoryFunctionName: String = "createWasiPreview1HostFunctions",
) {
    private val baseTypeResolver = WasiBaseTypeResolver(wasiTypenames)

    fun generateFunctions(): List<FunSpec> = listOf(
        generateCompleteFactory(),
        generateRequiredValidator(),
        generateRequiredFactory(),
        generateMemoryRequirement("requiresMemoryReader", WASI_MEMORY_READER_FUNCTIONS),
        generateMemoryRequirement("requiresMemoryWriter", WASI_MEMORY_WRITER_FUNCTIONS),
    )

    private fun generateCompleteFactory(): FunSpec = FunSpec.builder(factoryFunctionName).apply {
        addModifiers(INTERNAL)
        addParameter("store", ChasmShapesClassname.STORE)
        addParameter("memoryIndex", ChasmShapesClassname.MEMORY_INDEX)
        addParameter("host", WehHostClassname.EMBEDDER_HOST)
        addParameter(
            ParameterSpec.builder("moduleName", STRING).defaultValue("%S", "wasi_snapshot_preview1").build(),
        )
        returns(LIST.parameterizedBy(ChasmShapesClassname.IMPORT))

        addCode("val %N = %T(%M)\n", "i", AstType.VALUE_TYPE_NUMBER, AstType.AST_NUMBER_TYPE_I32)
        addCode("val %N = %T(%M)\n", "j", AstType.VALUE_TYPE_NUMBER, AstType.AST_NUMBER_TYPE_I64)

        getResultTypes().forEach { listOfArgs: List<BaseWebAssemblyType> ->
            val propertyName = listOfArgs.listPropertyName()
            val args: List<Any> = buildList {
                add(propertyName)
                add(AstType.AST_RESULT_TYPE)
                listOfArgs.map {
                    when (it) {
                        I32 -> "i"
                        I64 -> "j"
                    }
                }.let(::addAll)
            }
            addCode(
                format = listOfArgs.joinToString(
                    prefix = "val %N = %T(listOf(",
                    postfix = "))\n",
                    separator = ",",
                ) { "%N" },
                args = args.toTypedArray(),
            )
        }

        getBaseFunctionTypes(wasiFunctions, baseTypeResolver).forEach { functionType ->
            addCode(
                "val %N = %T(%N, %N)\n",
                functionType.propertyName,
                AstType.AST_FUNCTION_TYPE,
                functionType.input.listPropertyName(),
                functionType.results.listPropertyName(),
            )
        }

        addCode(
            "val functions = %T(host, memoryIndex, requiresMemoryReader = true, requiresMemoryWriter = true)\n",
            functionsClassName,
        )
        addCode("return listOf(⇥⇥\n")
        wasiFunctions.forEach { wasiFunction ->
            val baseType = BaseFunctionType.fromWasiFunc(wasiFunction, baseTypeResolver)
            addCode(
                "%T(moduleName, %S, %M(store, %N, functions.%N())),\n",
                ChasmShapesClassname.IMPORT,
                wasiFunction.export,
                ChasmShapesClassname.CHASM_EMBEDDING_FUNCTION,
                baseType.propertyName,
                wasiFunction.export.toCamelCasePropertyName(),
            )
        }
        addCode("⇤⇤)")
    }.build()

    private fun generateRequiredValidator(): FunSpec =
        FunSpec.builder("validateRequiredWasiPreview1HostFunctions").apply {
            addModifiers(INTERNAL)
            addParameter(
                "imports",
                LIST.parameterizedBy(ChasmShapesClassname.IMPORT_DEFINITION),
            )

            addStatement("if (imports.isEmpty()) return")
            getBaseFunctionTypes(wasiFunctions, baseTypeResolver).forEach { functionType ->
                addStatement("var %N: %T? = null", functionType.propertyName, AstType.AST_FUNCTION_TYPE)
            }
            addCodeBlock {
                add("for (definition in imports) {\n")
                indent()
                add("val expectedType = when (definition.entityName) {\n")
                indent()
                wasiFunctions.forEach { wasiFunction ->
                    val baseType = BaseFunctionType.fromWasiFunc(wasiFunction, baseTypeResolver)
                    add("%S -> %N ?: ", wasiFunction.export, baseType.propertyName)
                    add("%L.also { %N = it }\n", functionType(baseType), baseType.propertyName)
                }
                add("else -> error(\n")
                indent()
                add(
                    "%S + definition.moduleName + %S + definition.entityName + %S,\n",
                    "Unsupported WASI Preview 1 import `",
                    ".",
                    "`",
                )
                unindent()
                add(")\n")
                unindent()
                add("}\n")
                addStatement(
                    "val actualType = definition.type as? %T",
                    ChasmShapesClassname.EXTERNAL_FUNCTION_TYPE,
                )
                add("require(actualType?.functionType == expectedType) {\n")
                indent()
                add(
                    "%S + definition.moduleName + %S + definition.entityName + %S + " +
                        "definition.type + %S + %T(expectedType)\n",
                    "WASI Preview 1 import `",
                    ".",
                    "` has type ",
                    "; expected ",
                    ChasmShapesClassname.EXTERNAL_FUNCTION_TYPE,
                )
                unindent()
                add("}\n")
                unindent()
                add("}\n")
            }
        }.build()

    private fun generateRequiredFactory(): FunSpec = FunSpec.builder("createRequiredWasiPreview1HostFunctions").apply {
        addModifiers(INTERNAL)
        addParameter("store", ChasmShapesClassname.STORE)
        addParameter("memoryIndex", ChasmShapesClassname.MEMORY_INDEX)
        addParameter("host", WehHostClassname.EMBEDDER_HOST)
        addParameter("imports", LIST.parameterizedBy(ChasmShapesClassname.IMPORT_DEFINITION))
        returns(LIST.parameterizedBy(ChasmShapesClassname.IMPORT))

        addStatement("if (imports.isEmpty()) return emptyList()")
        addCodeBlock {
            add("val functions = %T(\n", functionsClassName)
            indent()
            add("host = host,\n")
            add("memoryIndex = memoryIndex,\n")
            add("requiresMemoryReader = imports.any { requiresMemoryReader(it.entityName) },\n")
            add("requiresMemoryWriter = imports.any { requiresMemoryWriter(it.entityName) },\n")
            unindent()
            add(")\n")
            add("return imports.map { definition ->\n")
            indent()
            add("val hostFunction = when (definition.entityName) {\n")
            indent()
            wasiFunctions.forEach { wasiFunction ->
                addStatement(
                    "%S -> functions.%N()",
                    wasiFunction.export,
                    wasiFunction.export.toCamelCasePropertyName(),
                )
            }
            addStatement("else -> error(%S + definition.entityName)", "Unsupported WASI Preview 1 import: ")
            unindent()
            add("}\n")
            addStatement(
                "val functionType = (definition.type as %T).functionType",
                ChasmShapesClassname.EXTERNAL_FUNCTION_TYPE,
            )
            add(
                "%T(\n⇥definition.moduleName,\ndefinition.entityName,\n" +
                    "%M(store, functionType, hostFunction),\n⇤)\n",
                ChasmShapesClassname.IMPORT,
                ChasmShapesClassname.CHASM_EMBEDDING_FUNCTION,
            )
            unindent()
            add("}\n")
        }
    }.build()

    private fun generateMemoryRequirement(
        functionName: String,
        requiredFunctions: Set<String>,
    ): FunSpec = FunSpec.builder(functionName).apply {
        addModifiers(PRIVATE)
        addParameter("entityName", STRING)
        returns(BOOLEAN)
        addCodeBlock {
            add("return when (entityName) {\n")
            indent()
            requiredFunctions.sorted().forEach { function ->
                add("%S,\n", function)
            }
            add("-> true\n")
            add("else -> false\n")
            unindent()
            add("}\n")
        }
    }.build()

    private fun functionType(functionType: BaseFunctionType): CodeBlock = CodeBlock.builder().apply {
        add("%T(\n", AstType.AST_FUNCTION_TYPE)
        indent()
        add("params = %L,\n", resultType(functionType.input))
        add("results = %L,\n", resultType(functionType.results))
        unindent()
        add(")")
    }.build()

    private fun resultType(types: List<BaseWebAssemblyType>): CodeBlock = CodeBlock.builder().apply {
        add("%T(listOf(", AstType.AST_RESULT_TYPE)
        types.forEachIndexed { index, type ->
            if (index > 0) add(", ")
            when (type) {
                I32 -> add("%T(%M)", AstType.VALUE_TYPE_NUMBER, AstType.AST_NUMBER_TYPE_I32)
                I64 -> add("%T(%M)", AstType.VALUE_TYPE_NUMBER, AstType.AST_NUMBER_TYPE_I64)
            }
        }
        add("))")
    }.build()

    private fun getResultTypes(): Set<List<BaseWebAssemblyType>> {
        return wasiFunctions.flatMap { wasiFunction ->
            listOf(
                baseTypeResolver.getFuncInputArgs(wasiFunction),
                baseTypeResolver.getFuncReturnTypes(wasiFunction),
            )
        }
            .map { args: List<NamedParamType> -> args.map { it.baseType.wasmType } }
            .toSortedSet(listOfTypesComparator)
    }

    private fun FunSpec.Builder.addCodeBlock(block: CodeBlock.Builder.() -> Unit) {
        addCode(CodeBlock.builder().apply(block).build())
    }
}
