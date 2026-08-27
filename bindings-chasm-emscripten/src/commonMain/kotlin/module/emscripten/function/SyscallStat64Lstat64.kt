/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MemberNameEqualsClassName")

package at.released.weh.bindings.chasm.module.emscripten.function

import at.released.weh.bindings.chasm.memory.ChasmMemoryAccess
import at.released.weh.bindings.chasm.module.emscripten.HostFunctionProvider
import at.released.weh.emcripten.runtime.function.SyscallStatLstat64FunctionHandle
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.host.readI32
import io.github.charlietap.chasm.host.withMemory
import io.github.charlietap.chasm.host.writeI32

internal fun syscallStat64(
    host: EmbedderHost,
    memoryIndex: ModuleIndex.MemoryIndex,
): HostFunctionProvider = SyscallStat64Lstat64(memoryIndex, SyscallStatLstat64FunctionHandle.syscallStat64(host))

internal fun syscallLstat64(
    host: EmbedderHost,
    memoryIndex: ModuleIndex.MemoryIndex,
): HostFunctionProvider = SyscallStat64Lstat64(memoryIndex, SyscallStatLstat64FunctionHandle.syscallLstat64(host))

internal class SyscallStat64Lstat64(
    private val memoryIndex: ModuleIndex.MemoryIndex,
    private val handle: SyscallStatLstat64FunctionHandle,
) : HostFunctionProvider {
    override val function: HostFunction = HostFunction { parameters, results ->
        val pathnamePtr = parameters.readI32(0)
        val dstAddr = parameters.readI32(1)
        val result = withMemory(memoryIndex) {
            handle.execute(this, pathnamePtr, dstAddr, ChasmMemoryAccess)
        }
        results.writeI32(0, result)
    }
}
