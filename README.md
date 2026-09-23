# WASI host functions for Chasm

Kotlin Multiplatform implementation of the WebAssembly WASI Preview 1 host functions for the [Chasm] runtime.

The repository retains the Emscripten implementation for compatibility and development, but Maven Central releases
are intentionally limited to the WASI Preview 1 Chasm binding and its runtime dependencies.

[Chasm]: https://github.com/CharlieTap/chasm

## Development

The build uses JDK 25. Apple native targets require Xcode when built on macOS.

The main verification entry points mirror CI:

```shell
./gradlew styleCheck :bindings-chasm-wasip1:jvmTest :test-wasi-testsuite:bindings-test:jvmTest \
    :wasm-wasi-preview1:jvmTest :host:jvmTest :common-util:jvmTest :wasm-core:jvmTest \
    checkKotlinAbi \
    :bindings-chasm-wasip1:compileKotlinLinuxX64 :bindings-chasm-wasip1:compileKotlinLinuxArm64 \
    --no-configuration-cache
./gradlew :bindings-chasm-wasip1:compileKotlinMacosArm64 :bindings-chasm-wasip1:compileKotlinIosArm64 \
    :bindings-chasm-wasip1:compileKotlinIosSimulatorArm64 --no-configuration-cache
```

The published binding has the coordinate
`io.github.charlietap.wasi.emscripten.host:bindings-chasm-wasip1`. Its six internal runtime dependencies are published
at the same version and are resolved transitively.

The standalone Gradle samples can be compiled against this working tree while resolving the published Chasm 2.0
release from Maven Central:

```shell
./gradlew -p samples/wasm-gradle \
    -Pweh.source=/absolute/path/to/wasi-emscripten-host \
    compileKotlinJvm
```

See [RELEASING.md](RELEASING.md) for local publication validation and Maven Central release instructions.

## License

These services are licensed under Apache 2.0 License. Authors and contributors are listed in the
[Authors](AUTHORS) file.

```
Copyright 2024-2025 wasi-emscripten-host project authors and contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
