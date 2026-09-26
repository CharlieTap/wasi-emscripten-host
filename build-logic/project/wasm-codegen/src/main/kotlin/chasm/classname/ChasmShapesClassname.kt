/*
 * Copyright 2024-2025, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.wasm.codegen.chasm.classname

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member

internal object ChasmShapesClassname {
    const val EMBEDDING_SHAPES_PACKAGE = "io.github.charlietap.chasm.embedding.shapes"
    const val HOST_PACKAGE = "io.github.charlietap.chasm.host"
    val HOST_FUNCTION = ClassName(HOST_PACKAGE, "HostFunction")
    val HOST_MEMORY = ClassName(HOST_PACKAGE, "HostMemory")
    val MEMORY_INDEX = ClassName(HOST_PACKAGE, "ModuleIndex").nestedClass("MemoryIndex")
    val IMPORT = ClassName(EMBEDDING_SHAPES_PACKAGE, "Import")
    val IMPORT_DEFINITION = ClassName(EMBEDDING_SHAPES_PACKAGE, "ImportDefinition")
    val STORE = ClassName(EMBEDDING_SHAPES_PACKAGE, "Store")
    val EXTERNAL_TYPE = ClassName("io.github.charlietap.chasm.runtime.type", "ExternalType")
    val EXTERNAL_FUNCTION_TYPE = EXTERNAL_TYPE.nestedClass("Function")
    val CHASM_EMBEDDING_FUNCTION = MemberName("io.github.charlietap.chasm.embedding", "function")
    val READ_I32 = MemberName(HOST_PACKAGE, "readI32")
    val READ_I64 = MemberName(HOST_PACKAGE, "readI64")
    val WRITE_I32 = MemberName(HOST_PACKAGE, "writeI32")
    val WITH_MEMORY = MemberName(HOST_PACKAGE, "withMemory")

    internal object AstType {
        const val PACKAGE = "io.github.charlietap.chasm.type"
        val AST_FUNCTION_TYPE = ClassName(PACKAGE, "FunctionType")
        val AST_NUMBER_TYPE = ClassName(PACKAGE, "NumberType")
        val AST_NUMBER_TYPE_I32 = AST_NUMBER_TYPE.member("I32")
        val AST_NUMBER_TYPE_I64 = AST_NUMBER_TYPE.member("I64")
        val AST_RESULT_TYPE = ClassName(PACKAGE, "ResultType")
        val AST_VALUE_TYPE = ClassName(PACKAGE, "ValueType")
        val VALUE_TYPE_NUMBER = AST_VALUE_TYPE.nestedClass("Number")
    }
}
