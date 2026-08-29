/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.wasi.testsuite.codegen.generator

import com.squareup.kotlinpoet.ClassName

public enum class WasmRuntimeBindings(
    val runtimeFactoryClassName: ClassName,
) {
    CHASM(
        runtimeFactoryClassName = ClassName(
            "at.released.weh.wasi.bindings.test.chasm.base",
            "ChasmWasmTestRuntime",
        ).nestedClass("Factory"),
    ),
}
