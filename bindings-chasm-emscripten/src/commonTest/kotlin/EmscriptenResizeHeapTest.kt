/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm

import at.released.weh.host.EmbedderHostBuilder
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.expect
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.runtime.value.NumberValue
import kotlin.test.Test
import kotlin.test.assertEquals

class EmscriptenResizeHeapTest {
    @Test
    fun `grows exported memory and reports boolean success or failure`() {
        val host = EmbedderHostBuilder().build()
        try {
            val store = store()
            val module = module(RESIZE_MODULE).expect("resize module must decode")
            val builder = ChasmEmscriptenHostBuilder(store, module) { this.host = host }
            val imports = builder.setupEmscriptenFunctions().emscriptenFunctions
            val instance = instance(store, module, imports).expect("resize module must instantiate")

            fun invokeInt(name: String, argument: Int? = null): Int {
                val arguments = argument?.let { listOf(NumberValue.I32(it)) }.orEmpty()
                val result = invoke(store, instance, name, arguments).expect("$name must execute")
                return (result.single() as NumberValue.I32).value
            }

            assertEquals(1, invokeInt("size"))
            assertEquals(1, invokeInt("resize", 1), "an already satisfied request succeeds")
            assertEquals(1, invokeInt("size"))

            assertEquals(1, invokeInt("resize", 65_537), "growth within the declared maximum succeeds")
            assertEquals(2, invokeInt("size"))

            assertEquals(1, invokeInt("resize", 131_073), "growth to the declared maximum succeeds")
            assertEquals(3, invokeInt("size"))

            assertEquals(0, invokeInt("resize", 196_609), "growth beyond the maximum fails as C false")
            assertEquals(3, invokeInt("size"), "failed growth leaves memory unchanged")
        } finally {
            host.close()
        }
    }
}

private val RESIZE_MODULE: ByteArray = hexToByteArray(
    "0061736d01000000010a0260017f017f6000017f021e0103656e7616656d736372697074656e5f726573697a655f686561" +
            "7000000303020001050401010103071a03066d656d6f7279020006726573697a6500010473697a6500020a0d0206002000" +
            "10000b04003f000b",
)

private fun hexToByteArray(hex: String): ByteArray = ByteArray(hex.length / 2) { index ->
    hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
}
