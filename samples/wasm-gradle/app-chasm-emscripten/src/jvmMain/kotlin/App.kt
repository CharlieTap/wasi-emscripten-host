/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.sample.chasm.gradle.app

import at.released.weh.bindings.chasm.ChasmEmscriptenHostBuilder
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.runtime.value.NumberValue.I32
import java.io.InputStream

fun main() {
    // Create Host and run code
    EmbedderHost {
        fileSystem {
            unrestricted = true
        }
    }.use(::executeCode)
}

@Suppress("ThrowsCount")
private fun executeCode(embedderHost: EmbedderHost) {
    val store: Store = store()

    // Decode first so the host builder can resolve the exported `memory` index once.
    val helloWorldBytes = checkNotNull(Thread.currentThread().contextClassLoader.getResource("helloworld.wasm"))
        .openStream()
        .use(InputStream::readAllBytes)
    val module = module(bytes = helloWorldBytes).fold(
        onSuccess = { it },
        onError = { throw WasmException("Cannot decode WebAssembly binary: $it") },
    )

    // Prepare WASI and Emscripten host imports
    val chasmBuilder = ChasmEmscriptenHostBuilder(store, module) {
        this.host = embedderHost
    }
    val wasiHostFunctions = chasmBuilder.setupWasiPreview1HostFunctions()
    val emscriptenInstaller = chasmBuilder.setupEmscriptenFunctions()

    val hostImports: List<Import> = buildList {
        addAll(emscriptenInstaller.emscriptenFunctions)
        addAll(wasiHostFunctions)
    }

    // Instantiate the WebAssembly module
    val instance = instance(store, module, hostImports).fold(
        onSuccess = { it },
        onError = { throw WasmException("Can node instantiate WebAssembly binary: $it") },
    )

    // Finalize initialization after module instantiation
    val emscriptenRuntime = emscriptenInstaller.finalize(instance)

    // Initialize Emscripten runtime environment
    emscriptenRuntime.initMainThread()

    // Execute code
    invoke(
        store = store,
        instance = instance,
        name = "main",
        args = listOf(
            // argc
            I32(0),
            // argv
            I32(0),
        ),
    ).fold(
        onSuccess = { it },
        onError = { throw WasmException("main() failed") },
    )
}

class WasmException(message: String) : RuntimeException(message)
