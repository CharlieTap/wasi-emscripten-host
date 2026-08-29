/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm

import at.released.weh.bindings.chasm.dsl.ChasmHostFunctionDsl
import at.released.weh.bindings.chasm.exports.ChasmEmscriptenMainExports
import at.released.weh.bindings.chasm.exports.ChasmEmscriptenStackExports
import at.released.weh.bindings.chasm.memory.ChasmEmbeddingMemoryAdapter
import at.released.weh.bindings.chasm.module.emscripten.createEmscriptenHostFunctions
import at.released.weh.bindings.chasm.wasip1.ChasmWasiPreview1Builder
import at.released.weh.common.api.Logger
import at.released.weh.emcripten.runtime.export.DefaultEmscriptenRuntime
import at.released.weh.emcripten.runtime.export.EmscriptenRuntime
import at.released.weh.emcripten.runtime.export.stack.EmscriptenStack
import at.released.weh.host.EmbedderHost
import at.released.weh.host.EmbedderHostBuilder
import at.released.weh.wasm.core.WasmModules.ENV_MODULE_NAME
import at.released.weh.wasm.core.WasmModules.WASI_SNAPSHOT_PREVIEW1_MODULE_NAME
import io.github.charlietap.chasm.embedding.exports
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Module
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.runtime.type.ExternalType
import io.github.charlietap.chasm.embedding.shapes.Instance as ChasmInstance
import io.github.charlietap.chasm.embedding.shapes.Memory as ChasmMemory

/**
 * Emscripten / WASI Preview 1 host function installer.
 *
 * Sets up WebAssembly host imports that provide the Emscripten env and WASI Preview 1 implementations.
 *
 * To create a new instance, use [ChasmEmscriptenHostBuilder(store, module)][Companion.invoke].
 *
 * Usage example:
 *
 * ```kotlin
 * val store: Store = store()
 * val module = module(helloWorldBytes).fold(
 *     onSuccess = { it },
 *     onError = { error("Cannot decode WebAssembly binary: $it") },
 * )
 *
 * val chasmHostBuilder = ChasmEmscriptenHostBuilder(store, module) {
 *     this.host = embedderHost
 * }
 * val wasiHostFunctions = chasmHostBuilder.setupWasiPreview1HostFunctions()
 * val emscriptenFinalizer = chasmHostBuilder.setupEmscriptenFunctions()
 *
 * val hostImports: List<Import> = buildList {
 *     addAll(emscriptenInstaller.emscriptenFunctions)
 *     addAll(wasiHostFunctions)
 * }
 *
 * // Instantiate the WebAssembly module
 * val instance = instance(store, module, hostImports).fold(
 *     onSuccess = { it },
 *     onError = { error("Cannot instantiate WebAssembly binary: $it") },
 * )
 *
 * // Finalize initialization after module instantiation
 * val emscriptenRuntime = emscriptenInstaller.finalize(instance)
 *
 * // Initialize Emscripten runtime environment
 * emscriptenRuntime.initMainThread()
 *
 * // Execute code
 * ```
 */
public class ChasmEmscriptenHostBuilder private constructor(
    private val store: Store,
    private val module: Module,
    private val memoryIndex: ModuleIndex.MemoryIndex,
    private val host: EmbedderHost,
) {
    public fun setupWasiPreview1HostFunctions(
        moduleName: String = WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
    ): List<Import> = ChasmWasiPreview1Builder(store, module) {
        this.host = this@ChasmEmscriptenHostBuilder.host
    }.build(moduleName)

    public fun setupEmscriptenFunctions(
        moduleName: String = ENV_MODULE_NAME,
    ): ChasmEmscriptenSetupFinalizer {
        return ChasmEmscriptenSetupFinalizer(store, memoryIndex, host.rootLogger).apply {
            setupEmscriptenFunctions(host, moduleName)
        }
    }

    public class ChasmEmscriptenSetupFinalizer internal constructor(
        private val store: Store,
        private val memoryIndex: ModuleIndex.MemoryIndex,
        private val rootLogger: Logger,
    ) {
        public var emscriptenFunctions: List<Import> = emptyList()
            private set

        private var _emscriptenStack: EmscriptenStack? = null
        private val emscriptenStack: EmscriptenStack
            get() = _emscriptenStack ?: error("Emscripten instantiation is not finalized")

        internal fun setupEmscriptenFunctions(
            host: EmbedderHost,
            moduleName: String,
        ) {
            emscriptenFunctions = createEmscriptenHostFunctions(
                store = store,
                memoryIndex = memoryIndex,
                host = host,
                emscriptenStackRef = ::emscriptenStack,
                moduleName = moduleName,
            )
        }

        public fun finalize(instance: ChasmInstance): EmscriptenRuntime {
            val memory = exports(instance).singleOrNull { export -> export.name == EMSCRIPTEN_MEMORY_EXPORT_NAME }
                ?.value as? ChasmMemory
                ?: error("Emscripten module must export exactly one `$EMSCRIPTEN_MEMORY_EXPORT_NAME` memory")
            return finalize(instance, memory)
        }

        public fun finalize(
            instance: ChasmInstance,
            memory: ChasmMemory,
        ): EmscriptenRuntime {
            val emscriptenRuntime = DefaultEmscriptenRuntime.emscriptenSingleThreadedRuntime(
                mainExports = ChasmEmscriptenMainExports(store, instance),
                stackExports = ChasmEmscriptenStackExports(store, instance),
                memory = ChasmEmbeddingMemoryAdapter(store, memory),
                logger = rootLogger,
            )
            _emscriptenStack = emscriptenRuntime.stack
            return emscriptenRuntime
        }
    }

    public companion object {
        public operator fun invoke(
            store: Store,
            module: Module,
            block: ChasmHostFunctionDsl.() -> Unit = {},
        ): ChasmEmscriptenHostBuilder {
            val config = ChasmHostFunctionDsl().apply(block)
            return ChasmEmscriptenHostBuilder(
                store = store,
                module = module,
                memoryIndex = module.emscriptenMemoryIndex(),
                host = config.host ?: EmbedderHostBuilder().build(),
            )
        }
    }
}

private fun Module.emscriptenMemoryIndex(): ModuleIndex.MemoryIndex {
    val export = exports.singleOrNull { export -> export.name == EMSCRIPTEN_MEMORY_EXPORT_NAME }
        ?: return ModuleIndex.MemoryIndex(0)
    check(export.type is ExternalType.Memory) {
        "Emscripten `$EMSCRIPTEN_MEMORY_EXPORT_NAME` export must be a memory"
    }
    return export.index as? ModuleIndex.MemoryIndex
        ?: error("Chasm returned a non-memory index for the `$EMSCRIPTEN_MEMORY_EXPORT_NAME` memory export")
}

private const val EMSCRIPTEN_MEMORY_EXPORT_NAME: String = "memory"
