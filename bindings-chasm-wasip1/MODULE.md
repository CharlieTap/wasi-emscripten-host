# Module bindings-chasm-wasip1

Implementation of WASI Preview 1 host functions for [Chasm] WebAssembly runtime.

[<img alt="Maven Central Version" src="https://img.shields.io/maven-central/v/at.released.weh/bindings-chasm-wasip1?style=flat-square">](https://central.sonatype.com/artifact/at.released.weh/bindings-chasm-wasip1/overview)

## Usage

Use [ChasmWasiPreview1Builder](https://weh.released.at/api/bindings-chasm-wasip1/at.released.weh.bindings.chasm.wasip1/-chasm-wasi-preview1-builder/index.html)
to set up host functions.

```kotlin
import at.released.weh.bindings.chasm.exception.ProcExitException
import at.released.weh.bindings.chasm.wasip1.ChasmWasiPreview1Builder
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.embedding.store

// Create Host and run code
EmbedderHost {
    fileSystem {
        addPreopenedDirectory(".", "/data")
    }
}.use {
    executeCode(it, wasmBinary)
}

fun executeCode(embedderHost: EmbedderHost, wasmBinary: ByteArray): Int {
    val store: Store = store()
    val module = module(wasmBinary).fold(
        onSuccess = { it },
        onError = { error("Cannot decode WebAssembly binary: $it") },
    )

    // Resolve the exported `memory` once while preparing the imports.
    val wasiImports: List<Import> = ChasmWasiPreview1Builder(store, module) {
        host = embedderHost
    }.build()

    // Instantiate the WebAssembly module
    val instance = instance(store, module, wasiImports).fold(
            onSuccess = { it },
            onError = { error("Cannot instantiate WebAssembly binary: $it") },
        )

    // Execute code
    try {
        invoke(store, instance, "_start").fold(
            onSuccess = { it },
            onError = { error("main() failed") },
        )
    } catch (pre: ProcExitException) {
        return pre.exitCode
    }
    return 0
}
```

## Performance model

The Chasm 2.0 binding uses raw `LongArray` host-function stack slots and direct
`HostMemory` scalar access. It resolves the module's exported `memory` once
when the builder is created, captures its typed memory index, and reacquires
the calling instance's memory through Chasm's typed `withMemory` caller scope,
then reacquires any unsafe JVM/Native backing storage inside each callback. The
default JVM and POSIX filesystem paths therefore perform scatter/gather reads
and writes without copying payload bytes; custom memories and filesystems use
the compatible copying fallback.

Run the retained JVM bridge benchmark against the configured Chasm snapshot with:

```shell
WEH_CHASM_BENCHMARK_ENFORCE=true ./gradlew \
    :bindings-chasm-wasip1:jvmTest \
    --tests 'at.released.weh.bindings.chasm.performance.ChasmBridgeBenchmarkTest' \
    --rerun-tasks
```

The environment flag enables the deliberately strict 5% callback and direct
filesystem timing gates. The allocation assertions run in every JVM test
execution.

[Chasm]: https://github.com/CharlieTap/chasm
