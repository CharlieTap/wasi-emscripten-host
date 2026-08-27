/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MemberNameEqualsClassName")

package at.released.weh.bindings.chasm.module.emscripten.function

import at.released.weh.bindings.chasm.memory.ChasmMemoryAccess
import at.released.weh.bindings.chasm.module.emscripten.HostFunctionProvider
import at.released.weh.emcripten.runtime.function.SyscallOpenatFunctionHandle
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.host.readI32
import io.github.charlietap.chasm.host.withMemory
import io.github.charlietap.chasm.host.writeI32

internal class SyscallOpenat(
    host: EmbedderHost,
    private val memoryIndex: ModuleIndex.MemoryIndex,
) : HostFunctionProvider {
    private val handle: SyscallOpenatFunctionHandle = SyscallOpenatFunctionHandle(host)
    override val function: HostFunction = HostFunction { parameters, results ->
        val rawDirFd = parameters.readI32(0)
        val pathnamePtr = parameters.readI32(1)
        val rawFlags = parameters.readI32(2)
        val varargsPtr = parameters.readI32(3)
        val fdOrErrno = withMemory(memoryIndex) {
            val mode = readI32(varargsPtr)
            handle.execute(this, rawDirFd, pathnamePtr, rawFlags, mode, ChasmMemoryAccess)
        }
        results.writeI32(0, fdOrErrno)
    }
}
