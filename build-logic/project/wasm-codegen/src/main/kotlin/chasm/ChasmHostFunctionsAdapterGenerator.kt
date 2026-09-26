/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.wasm.codegen.chasm

import at.released.weh.gradle.wasm.codegen.chasm.ChasmArgsFunctionHandles.WasiFunctionHandle
import at.released.weh.gradle.wasm.codegen.chasm.classname.ChasmBindingsClassname.CHASM_FUNCTIONS_CLASS_NAME
import at.released.weh.gradle.wasm.codegen.chasm.classname.ChasmBindingsClassname.CHASM_WASI_MEMORY_READER
import at.released.weh.gradle.wasm.codegen.chasm.classname.ChasmBindingsClassname.CHASM_WASI_MEMORY_WRITER
import at.released.weh.gradle.wasm.codegen.chasm.classname.ChasmShapesClassname
import at.released.weh.gradle.wasm.codegen.util.classname.SUPPRESS_CLASS_NAME
import at.released.weh.gradle.wasm.codegen.util.classname.WehHostClassname
import at.released.weh.gradle.wasm.codegen.witx.parser.model.WasiFunc
import at.released.weh.gradle.wasm.codegen.witx.parser.model.WasiType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal class ChasmHostFunctionsAdapterGenerator(
    private val wasiTypenames: Map<String, WasiType>,
    private val wasiFunctions: List<WasiFunc>,
    private val outputDirectory: File,
    private val factoryFunctionName: String = "createWasiPreview1HostFunctions",
) {
    fun generate() {
        val factoryGenerator = ChasmFactoryFunctionGenerator(
            wasiTypenames = wasiTypenames,
            wasiFunctions = wasiFunctions,
            functionsClassName = CHASM_FUNCTIONS_CLASS_NAME,
            factoryFunctionName = factoryFunctionName,
        )
        val spec = FileSpec.builder(CHASM_FUNCTIONS_CLASS_NAME).apply {
            factoryGenerator.generateFunctions().forEach(::addFunction)
            addType(generateFunctionsClass())
        }.build()
        spec.writeTo(outputDirectory)
    }

    private fun generateFunctionsClass(): TypeSpec = TypeSpec.classBuilder(CHASM_FUNCTIONS_CLASS_NAME).apply {
        addModifiers(INTERNAL)
        addAnnotation(AnnotationSpec.builder(SUPPRESS_CLASS_NAME).addMember("%S", "UNUSED_PARAMETER").build())
        primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("host", WehHostClassname.EMBEDDER_HOST)
                .addParameter("memoryIndex", ChasmShapesClassname.MEMORY_INDEX)
                .addParameter("requiresMemoryReader", BOOLEAN)
                .addParameter("requiresMemoryWriter", BOOLEAN)
                .build(),
        )
        addProperty(
            PropertySpec.builder("host", WehHostClassname.EMBEDDER_HOST, PRIVATE)
                .initializer("host")
                .build(),
        )
        addProperty(
            PropertySpec.builder("memoryIndex", ChasmShapesClassname.MEMORY_INDEX, PRIVATE)
                .initializer("memoryIndex")
                .build(),
        )
        addProperty(
            PropertySpec.builder("wasiMemoryReader", CHASM_WASI_MEMORY_READER.copy(nullable = true), PRIVATE)
                .initializer("if (requiresMemoryReader) %T(host.fileSystem) else null", CHASM_WASI_MEMORY_READER)
                .build(),
        )
        addProperty(
            PropertySpec.builder("wasiMemoryWriter", CHASM_WASI_MEMORY_WRITER.copy(nullable = true), PRIVATE)
                .initializer("if (requiresMemoryWriter) %T(host.fileSystem) else null", CHASM_WASI_MEMORY_WRITER)
                .build(),
        )

        val functionHandles = ChasmArgsFunctionHandles(wasiTypenames, wasiFunctions).getFunctionHandles()

        functionHandles.forEach { funcHandleSpec: WasiFunctionHandle ->
            addFunction(funcHandleSpec.chasmHostFunctionDeclaration())
        }
    }.build()
}
