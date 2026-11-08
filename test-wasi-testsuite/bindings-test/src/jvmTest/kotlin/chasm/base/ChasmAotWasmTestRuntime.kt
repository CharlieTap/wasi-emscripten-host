/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalChasmApi::class)

package at.released.weh.wasi.bindings.test.chasm.base

import at.released.weh.bindings.chasm.wasip1.ChasmWasiPreview1Builder
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.bindings.test.runner.WasiTestsuiteArguments
import at.released.weh.wasi.bindings.test.runner.WasmTestRuntime
import io.github.charlietap.chasm.ExperimentalChasmApi
import io.github.charlietap.chasm.aot.CompileConfig
import io.github.charlietap.chasm.aot.CompileOutput
import io.github.charlietap.chasm.aot.CompiledModule
import io.github.charlietap.chasm.aot.compile
import io.github.charlietap.chasm.aot.expect
import io.github.charlietap.chasm.aot.instance
import io.github.charlietap.chasm.aot.moduleInfo
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.shapes.Instance
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.embedding.store
import kotlinx.io.files.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs the WASI testsuite through Chasm's ahead-of-time compiler: each module is compiled to JVM classes in memory,
 * loaded, and instantiated by runtime-aot with the same WASI Preview 1 imports the interpreter uses.
 */
object ChasmAotWasmTestRuntime : WasmTestRuntime {
    private val HOST_FUNCTION_ERROR_PATTERN = """HostFunctionError\(error=(\d+)\)""".toRegex()
    private val modules = AtomicInteger()
    override val hasOwnStdioTests: Boolean = false

    override fun runTest(
        wasmFile: ByteArray,
        host: EmbedderHost,
        arguments: WasiTestsuiteArguments,
        rootDir: Path,
    ): Int {
        val compiled = compileModule(wasmFile)
        val store: Store = store(compiled.metadata.storeConfig)
        val instance = setupInstance(store, compiled, host)
        return invoke(store, instance, "_start").fold(
            onSuccess = { 0 },
            onError = { executionError ->
                HOST_FUNCTION_ERROR_PATTERN.matchEntire(executionError.error)?.let { match ->
                    match.groups[1]!!.value.toIntOrNull()
                } ?: -1
            },
        )
    }

    private fun compileModule(wasmFile: ByteArray): CompiledModule {
        val output = CompileOutput.Memory()
        val config = CompileConfig("at.released.weh.wasi.aot.module${modules.incrementAndGet()}")
        val artifact = compile(wasmFile, output, config).expect("Can not compile WebAssembly binary ahead of time")
        return output.load(artifact)
    }

    private fun setupInstance(
        store: Store,
        compiled: CompiledModule,
        host: EmbedderHost,
    ): Instance {
        val info = moduleInfo(compiled).fold(
            onSuccess = { it },
            onError = { throw WasmException("Can not read compiled module info: $it") },
        )
        val hostImports = ChasmWasiPreview1Builder(store, info) {
            this.host = host
        }.build()
        return instance(store, compiled, hostImports).fold(
            onSuccess = { it },
            onError = { throw WasmException("Can not instantiate compiled WebAssembly module: $it") },
        )
    }

    class WasmException(message: String) : RuntimeException(message)

    class Factory : WasmTestRuntime.Factory {
        override fun invoke(): WasmTestRuntime = ChasmAotWasmTestRuntime
        override fun close() = Unit
    }
}
