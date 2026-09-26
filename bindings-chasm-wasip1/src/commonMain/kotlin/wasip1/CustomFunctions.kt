/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.wasip1

import at.released.weh.wasm.core.WasmModules
import io.github.charlietap.chasm.embedding.function
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.ImportDefinition
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.host.HostFunctionException
import io.github.charlietap.chasm.host.readI32
import io.github.charlietap.chasm.runtime.type.ExternalType
import io.github.charlietap.chasm.type.FunctionType
import io.github.charlietap.chasm.type.NumberType.I32
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.host.HostFunction as ChasmHostFunction

internal fun createCustomWasiPreview1HostFunctions(
    store: Store,
    moduleName: String = WasmModules.WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
): List<Import> {
    return listOf(
        Import(
            moduleName,
            PROC_EXIT_FUNCTION_NAME,
            function(
                store,
                procExitFunctionType(),
                procExitHostFunction,
            ),
        ),
    )
}

internal fun validateRequiredCustomWasiPreview1HostFunctions(
    imports: List<ImportDefinition>,
): FunctionType? {
    if (imports.isEmpty()) return null

    val expectedType = ExternalType.Function(procExitFunctionType())
    imports.forEach { definition ->
        require(definition.entityName == PROC_EXIT_FUNCTION_NAME) {
            "Unsupported custom WASI Preview 1 import `${definition.moduleName}.${definition.entityName}`"
        }
        require(definition.type == expectedType) {
            "WASI Preview 1 import `${definition.moduleName}.${definition.entityName}` has type " +
                "${definition.type}; expected $expectedType"
        }
    }
    return expectedType.functionType
}

internal fun createRequiredCustomWasiPreview1HostFunctions(
    store: Store,
    imports: List<ImportDefinition>,
    functionType: FunctionType?,
): List<Import> {
    if (imports.isEmpty()) return emptyList()

    val type = checkNotNull(functionType)
    return imports.map { definition ->
        Import(
            definition.moduleName,
            definition.entityName,
            function(store, type, procExitHostFunction),
        )
    }
}

private fun procExitFunctionType(): FunctionType = FunctionType(
    ResultType(listOf(ValueType.Number(I32))),
    ResultType(listOf()),
)

private val procExitHostFunction: ChasmHostFunction = ChasmHostFunction { parameters, _ ->
    val exitCode = parameters.readI32(0)
    throw HostFunctionException(exitCode.toString())
}

private const val PROC_EXIT_FUNCTION_NAME: String = "proc_exit"
