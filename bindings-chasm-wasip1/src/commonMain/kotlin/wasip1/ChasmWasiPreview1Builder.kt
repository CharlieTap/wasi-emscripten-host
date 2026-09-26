/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.wasip1

import at.released.weh.bindings.chasm.dsl.ChasmHostFunctionDsl
import at.released.weh.bindings.chasm.module.wasi.createRequiredWasiPreview1HostFunctions
import at.released.weh.bindings.chasm.module.wasi.createWasiPreview1HostFunctions
import at.released.weh.bindings.chasm.module.wasi.validateRequiredWasiPreview1HostFunctions
import at.released.weh.host.EmbedderHost
import at.released.weh.host.EmbedderHostBuilder
import at.released.weh.wasm.core.WasmModules
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.ImportDefinition
import io.github.charlietap.chasm.embedding.shapes.Module
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.runtime.type.ExternalType

/**
 * WASI Preview 1 host function installer.
 *
 * Sets up WebAssembly host imports that provide the Emscripten env and WASI Preview 1 implementations.
 *
 * To create a new instance, use [ChasmWasiPreview1Builder()][Companion.invoke].
 *
 * Usage example:
 *
 * ```kotlin
 * // Prepare WASI host imports
 * val wasiImports: List<Import> = ChasmWasiPreview1Builder(store, module) {
 *     host = embedderHost
 * }.build()
 * ```
 */
public class ChasmWasiPreview1Builder private constructor(
    private val store: Store,
    private val module: Module,
    private val memoryIndex: ModuleIndex.MemoryIndex,
    private val host: EmbedderHost,
) {
    public fun build(
        moduleName: String = WasmModules.WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
    ): List<Import> {
        return createWasiPreview1HostFunctions(
            store = store,
            memoryIndex = memoryIndex,
            host = host,
            moduleName = moduleName,
        ) + createCustomWasiPreview1HostFunctions(store, moduleName)
    }

    /**
     * Builds only the WASI Preview 1 imports required by this builder's module.
     *
     * Imports already present in [providedImports] take precedence and are not
     * allocated again.
     */
    public fun buildRequired(
        moduleName: String = WasmModules.WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
        providedImports: Collection<Import> = emptyList(),
    ): List<Import> {
        val requiredImports = module.requiredImports(moduleName, providedImports)
        val (customImports, generatedImports) = requiredImports.partition { definition ->
            definition.entityName in CUSTOM_WASI_PREVIEW1_FUNCTIONS
        }

        validateRequiredWasiPreview1HostFunctions(generatedImports)
        val customFunctionType = validateRequiredCustomWasiPreview1HostFunctions(customImports)

        val allocatedImports = createRequiredWasiPreview1HostFunctions(
            store = store,
            memoryIndex = memoryIndex,
            host = host,
            imports = generatedImports,
        ) + createRequiredCustomWasiPreview1HostFunctions(store, customImports, customFunctionType)
        val allocatedByName = allocatedImports.associateBy(Import::entityName)

        return requiredImports.map { definition ->
            checkNotNull(allocatedByName[definition.entityName]) {
                "WASI Preview 1 import was validated but not allocated: " +
                    "`${definition.moduleName}.${definition.entityName}`"
            }
        }
    }

    public companion object {
        public operator fun invoke(
            store: Store,
            module: Module,
            block: ChasmHostFunctionDsl.() -> Unit = {},
        ): ChasmWasiPreview1Builder {
            val config = ChasmHostFunctionDsl().apply(block)
            return ChasmWasiPreview1Builder(
                store = store,
                module = module,
                memoryIndex = module.wasiMemoryIndex(),
                host = config.host ?: EmbedderHostBuilder().build(),
            )
        }
    }
}

private fun Module.requiredImports(
    moduleName: String,
    providedImports: Collection<Import>,
): List<ImportDefinition> {
    val providedNames = providedImports.asSequence()
        .filter { import -> import.moduleName == moduleName }
        .mapTo(mutableSetOf(), Import::entityName)
    val requiredByName = linkedMapOf<String, ImportDefinition>()

    imports.forEach { definition ->
        if (definition.moduleName != moduleName) {
            return@forEach
        }

        val existing = requiredByName[definition.entityName]
        check(existing == null || existing.type == definition.type) {
            "Conflicting WASI Preview 1 imports for `${definition.moduleName}.${definition.entityName}`: " +
                "${existing?.type} and ${definition.type}"
        }
        if (existing == null) {
            requiredByName[definition.entityName] = definition
        }
    }

    return requiredByName.values.filterNot { definition -> definition.entityName in providedNames }
}

internal fun Module.wasiMemoryIndex(): ModuleIndex.MemoryIndex {
    val export = exports.singleOrNull { export -> export.name == WASI_MEMORY_EXPORT_NAME }
        ?: return ModuleIndex.MemoryIndex(0)
    check(export.type is ExternalType.Memory) {
        "WASI Preview 1 `$WASI_MEMORY_EXPORT_NAME` export must be a memory"
    }
    return export.index as? ModuleIndex.MemoryIndex
        ?: error("Chasm returned a non-memory index for the `$WASI_MEMORY_EXPORT_NAME` memory export")
}

private const val WASI_MEMORY_EXPORT_NAME: String = "memory"
private val CUSTOM_WASI_PREVIEW1_FUNCTIONS: Set<String> = setOf("proc_exit")
