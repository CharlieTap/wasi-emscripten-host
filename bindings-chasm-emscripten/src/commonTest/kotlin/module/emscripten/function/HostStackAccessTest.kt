/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.module.emscripten.function

import io.github.charlietap.chasm.host.HostExceptions
import io.github.charlietap.chasm.host.HostExterns
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.HostGc
import io.github.charlietap.chasm.host.HostGlobal
import io.github.charlietap.chasm.host.HostMemory
import io.github.charlietap.chasm.host.HostModuleInstance
import io.github.charlietap.chasm.host.HostReference
import io.github.charlietap.chasm.host.HostReferences
import io.github.charlietap.chasm.host.HostResources
import io.github.charlietap.chasm.host.HostTable
import io.github.charlietap.chasm.host.HostTag
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.host.readF64
import io.github.charlietap.chasm.host.readI32
import io.github.charlietap.chasm.host.readI64
import io.github.charlietap.chasm.host.writeF64
import io.github.charlietap.chasm.host.writeI32
import kotlin.test.Test
import kotlin.test.assertEquals

class HostStackAccessTest {
    @Test
    fun `preserves integer and floating point bit patterns`() {
        val nanBits = 0x7FF8_1234_5678_9ABCL
        val stack = longArrayOf(-1L, Long.MIN_VALUE, nanBits, 0L)

        with(stack) {
            assertEquals(-1, 0.readI32(0))
            assertEquals(Long.MIN_VALUE, 0.readI64(1))
            assertEquals(nanBits, 0.readF64(2).toRawBits())
            3.writeF64(0, Double.fromBits(nanBits))
        }

        assertEquals(nanBits, stack[3])
    }

    @Test
    fun `reads every parameter before writing an overlapping result`() {
        val stack = longArrayOf(7, 9)
        val add = HostFunction { parameters, results ->
            val left = parameters.readI32(0)
            val right = parameters.readI32(1)
            results.writeI32(0, left + right)
        }

        context(stack, TestModule, TestResources) {
            add.invoke(parameters = 0, results = 0)
        }

        assertEquals(16, stack[0])
        assertEquals(9, stack[1])
    }
}

private object TestModule : HostModuleInstance

private object TestResources : HostResources {
    override val references: HostReferences get() = error("unused")
    override val gc: HostGc get() = error("unused")
    override val externs: HostExterns get() = error("unused")
    override val exceptions: HostExceptions get() = error("unused")
    override fun memory(module: HostModuleInstance, index: ModuleIndex.MemoryIndex): HostMemory = error("unused")
    override fun growMemory(module: HostModuleInstance, index: ModuleIndex.MemoryIndex, pagesToAdd: Int): Int =
        error("unused")
    override fun table(module: HostModuleInstance, index: ModuleIndex.TableIndex): HostTable = error("unused")
    override fun growTable(
        module: HostModuleInstance,
        index: ModuleIndex.TableIndex,
        elementsToAdd: Int,
        value: HostReference,
    ): Int = error("unused")
    override fun global(module: HostModuleInstance, index: ModuleIndex.GlobalIndex): HostGlobal = error("unused")
    override fun tag(module: HostModuleInstance, index: ModuleIndex.TagIndex): HostTag = error("unused")
}
