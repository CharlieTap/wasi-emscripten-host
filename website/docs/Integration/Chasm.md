---
sidebar_label: 'Chasm'
sidebar_position: 1
description: 'Implementation of WASI Preview 1 and Emscripten host functions for Chasm'
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Chasm Integration

[Chasm] is an experimental WebAssembly runtime built on Kotlin Multiplatform.
It supports Android API 26+, JVM JDK 17+, and a variety of multiplatform targets.

This integration targets the Chasm 2.0 host-function API.

Chasm 2.0 and these bindings are currently under development. The coordinates below describe the intended release;
use the composite-build instructions in [Memory and performance](#memory-and-performance) until those artifacts are
published.

## Wasi Preview 1 Bindings Integration

Check [WASI Preview 1](../WASIP1) to see the current limitations of the WASI P1 implementation.

### Installation

Until Chasm 2.0 is released, add Central's snapshot repository:

```kotlin
repositories {
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        content {
            includeGroup("io.github.charlietap.chasm")
        }
    }
    mavenCentral()
}
```

Then add the required dependencies:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation("io.github.charlietap.chasm:chasm:2.0.0-SNAPSHOT")
        implementation("at.released.weh:bindings-chasm-wasip1:0.7.0-SNAPSHOT")
    }
}
```

### Usage

Below is an example demonstrating the execution of **helloworld.wasm**, built using Emscripten with the
`STANDALONE_WASM` flag.

```kotlin
import at.released.weh.bindings.chasm.wasip1.ChasmWasiPreview1Builder
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.embedding.store
import java.io.InputStream

fun main() {
    // Load WebAssembly binary
    val helloWorldBytes = checkNotNull(
        Thread.currentThread().contextClassLoader.getResource("helloworld_wasi.wasm"),
    ).openStream().use(InputStream::readAllBytes)

    // Create Host and run code
    EmbedderHost {
        fileSystem {
            addPreopenedDirectory(".", "/data")
        }
    }.use {
        executeCode(it, helloWorldBytes)
    }
}

fun executeCode(embedderHost: EmbedderHost, wasmBinary: ByteArray): Int {
    val store: Store = store()
    val module = module(wasmBinary).fold(
        onSuccess = { it },
        onError = { throw WasmException("Cannot decode WebAssembly binary: $it") },
    )

    // Resolve the exported `memory` once while preparing the imports.
    val wasiImports: List<Import> = ChasmWasiPreview1Builder(store, module) {
        host = embedderHost
    }.build()

    // Instantiate the WebAssembly module
    val instance = instance(store, module, wasiImports).fold(
            onSuccess = { it },
            onError = { throw WasmException("Cannot instantiate WebAssembly binary: $it") },
        )

    // Execute code
    invoke(store, instance, "_start").fold(
        onSuccess = { "Success" },
        onError = { executionError -> executionError.error },
    )

    return 0
}

class WasmException(message: String) : RuntimeException(message)
```

### Memory and performance

The Chasm 2.0 builder requires the decoded module because WASI Preview 1 uses
the module's exported memory named `memory`. The binding resolves that export
once, validates that it is a memory, and captures its typed Chasm memory index.
It does not assume index zero and it does not repeat the name lookup in host
callbacks.

Numeric callbacks operate directly on Chasm's raw stack slots. Scalar guest
memory accesses use `HostMemory` directly, while the default JVM and Native
filesystem paths borrow the current Chasm backing storage for the duration of
each scatter/gather operation. This removes payload copies while remaining
safe across memory growth. A custom Chasm memory or filesystem automatically
uses the compatible copying fallback.

The build resolves the work-in-progress Chasm 2.0 release from Central's
snapshot repository:

```shell
./gradlew :bindings-chasm-wasip1:jvmTest
```

The strict callback and direct-filesystem timing gates are opt-in so unrelated
parallel test load does not make the suite flaky:

```shell
WEH_CHASM_BENCHMARK_ENFORCE=true ./gradlew \
    :bindings-chasm-wasip1:jvmTest \
    --tests 'at.released.weh.bindings.chasm.performance.ChasmBridgeBenchmarkTest' \
    --rerun-tasks
```

## Emscripten bindings integration

### Installation

Add the required dependencies:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation("io.github.charlietap.chasm:chasm:2.0.0-SNAPSHOT")
        implementation("at.released.weh:bindings-chasm-emscripten:0.7.0-SNAPSHOT")
    }
}
```

### Usage

Below is an example demonstrating the execution of **helloworld.wasm**, prepared
in the "[Emscripten Example](../Emscripten#example)".

```kotlin
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

private fun executeCode(embedderHost: EmbedderHost) {
    val store: Store = store()

    // Decode first so both import builders can resolve the exported memory.
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
```

## Other samples

* https://github.com/CharlieTap/wasi-emscripten-host/tree/main/samples
  This directory in the the source repository contains more examples of using the library.
* https://github.com/illarionov/wehdemo  
  This example showcases how to execute a Kotlin/Wasm-WASI binary in a Kotlin Multiplatform project.

[Chasm]: https://github.com/CharlieTap/chasm
[Samples]: https://github.com/CharlieTap/wasi-emscripten-host/tree/main/samples
