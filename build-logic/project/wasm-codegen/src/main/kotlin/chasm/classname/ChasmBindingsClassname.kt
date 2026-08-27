/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.wasm.codegen.chasm.classname

import com.squareup.kotlinpoet.ClassName

object ChasmBindingsClassname {
    const val PACKAGE = "at.released.weh.bindings.chasm.module.wasi"
    val CHASM_FUNCTIONS_CLASS_NAME = ClassName(PACKAGE, "ChasmWasiPreview1Functions")
    val CHASM_MEMORY_ACCESS = ClassName("at.released.weh.bindings.chasm.memory", "ChasmMemoryAccess")

    val CHASM_WASI_MEMORY_READER =
        ClassName("at.released.weh.bindings.chasm.memory", "ChasmWasiMemoryReader")
    val CHASM_WASI_MEMORY_WRITER =
        ClassName("at.released.weh.bindings.chasm.memory", "ChasmWasiMemoryWriter")
}
