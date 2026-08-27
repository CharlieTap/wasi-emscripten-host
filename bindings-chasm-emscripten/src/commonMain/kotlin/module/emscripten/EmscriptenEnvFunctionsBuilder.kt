/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.module.emscripten

import at.released.weh.bindings.chasm.ext.toChasmFunctionTypes
import at.released.weh.bindings.chasm.module.emscripten.function.AbortJs
import at.released.weh.bindings.chasm.module.emscripten.function.AssertFail
import at.released.weh.bindings.chasm.module.emscripten.function.EmscriptenAsmConstAsyncOnMainThread
import at.released.weh.bindings.chasm.module.emscripten.function.EmscriptenAsmConstInt
import at.released.weh.bindings.chasm.module.emscripten.function.EmscriptenConsoleError
import at.released.weh.bindings.chasm.module.emscripten.function.EmscriptenDateNow
import at.released.weh.bindings.chasm.module.emscripten.function.EmscriptenGetNow
import at.released.weh.bindings.chasm.module.emscripten.function.EmscriptenGetNowIsMonotonic
import at.released.weh.bindings.chasm.module.emscripten.function.EmscriptenResizeHeap
import at.released.weh.bindings.chasm.module.emscripten.function.EmscriptenRuntimeKeepaliveClear
import at.released.weh.bindings.chasm.module.emscripten.function.Getentropy
import at.released.weh.bindings.chasm.module.emscripten.function.HandleStackOverflow
import at.released.weh.bindings.chasm.module.emscripten.function.LocaltimeJs
import at.released.weh.bindings.chasm.module.emscripten.function.MmapJs
import at.released.weh.bindings.chasm.module.emscripten.function.MunmapJs
import at.released.weh.bindings.chasm.module.emscripten.function.NotImplementedEmscriptenFunction
import at.released.weh.bindings.chasm.module.emscripten.function.SetittimerJs
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallChmod
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallFaccessat
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallFchmod
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallFchown32
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallFcntl64
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallFdatasync
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallFstat64
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallFtruncate64
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallGetcwd
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallMkdirat
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallOpenat
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallReadlinkat
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallRmdir
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallUnlinkat
import at.released.weh.bindings.chasm.module.emscripten.function.SyscallUtimensat
import at.released.weh.bindings.chasm.module.emscripten.function.TzsetJs
import at.released.weh.bindings.chasm.module.emscripten.function.syscallLstat64
import at.released.weh.bindings.chasm.module.emscripten.function.syscallStat64
import at.released.weh.emcripten.runtime.EmscriptenHostFunction
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.ABORT_JS
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.ASSERT_FAIL
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_ASM_CONST_ASYNC_ON_MAIN_THREAD
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_ASM_CONST_INT
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_CONSOLE_ERROR
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_DATE_NOW
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_GET_NOW
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_GET_NOW_IS_MONOTONIC
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_RESIZE_HEAP
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.EMSCRIPTEN_RUNTIME_KEEPALIVE_CLEAR
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.GETENTROPY
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.HANDLE_STACK_OVERFLOW
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.LOCALTIME_JS
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.MMAP_JS
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.MUNMAP_JS
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SETITIMER_JS
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_CHMOD
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_FACCESSAT
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_FCHMOD
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_FCHOWN32
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_FCNTL64
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_FDATASYNC
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_FSTAT64
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_FTRUNCATE64
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_GETCWD
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_LSTAT64
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_MKDIRAT
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_OPENAT
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_READLINKAT
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_RMDIR
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_STAT64
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_UNLINKAT
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.SYSCALL_UTIMENSAT
import at.released.weh.emcripten.runtime.EmscriptenHostFunction.TZSET_JS
import at.released.weh.emcripten.runtime.export.stack.EmscriptenStack
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.WasmModules.ENV_MODULE_NAME
import io.github.charlietap.chasm.embedding.function
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.embedding.shapes.Import as ChasmImport

@Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
internal fun createEmscriptenHostFunctions(
    store: Store,
    memoryIndex: ModuleIndex.MemoryIndex,
    host: EmbedderHost,
    emscriptenStackRef: () -> EmscriptenStack,
    moduleName: String = ENV_MODULE_NAME,
): List<ChasmImport> {
    val functionTypes = EmscriptenHostFunction.entries.map(EmscriptenHostFunction::type).toChasmFunctionTypes()
    return EmscriptenHostFunction.entries.map { emscriptenFunc ->
        ChasmImport(
            moduleName = moduleName,
            entityName = emscriptenFunc.wasmName,
            value = function(
                store = store,
                type = functionTypes.getValue(emscriptenFunc.type),
                function = emscriptenFunc.createChasmHostFunction(host, memoryIndex, emscriptenStackRef),
            ),
        )
    }
}

@Suppress("CyclomaticComplexMethod")
private fun EmscriptenHostFunction.createChasmHostFunction(
    host: EmbedderHost,
    memoryIndex: ModuleIndex.MemoryIndex,
    emscriptenStackRef: () -> EmscriptenStack,
): HostFunction = when (this) {
    ABORT_JS -> AbortJs(host)
    ASSERT_FAIL -> AssertFail(host, memoryIndex)
    EMSCRIPTEN_ASM_CONST_ASYNC_ON_MAIN_THREAD -> EmscriptenAsmConstAsyncOnMainThread(host)
    EMSCRIPTEN_ASM_CONST_INT -> EmscriptenAsmConstInt(host)
    EMSCRIPTEN_CONSOLE_ERROR -> EmscriptenConsoleError(host, memoryIndex)
    EMSCRIPTEN_DATE_NOW -> EmscriptenDateNow(host)
    EMSCRIPTEN_GET_NOW -> EmscriptenGetNow(host)
    EMSCRIPTEN_GET_NOW_IS_MONOTONIC -> EmscriptenGetNowIsMonotonic(host)
    EMSCRIPTEN_RESIZE_HEAP -> EmscriptenResizeHeap(host, memoryIndex)
    EMSCRIPTEN_RUNTIME_KEEPALIVE_CLEAR -> EmscriptenRuntimeKeepaliveClear(host)
    GETENTROPY -> Getentropy(host, memoryIndex)
    HANDLE_STACK_OVERFLOW -> HandleStackOverflow(host, emscriptenStackRef)
    LOCALTIME_JS -> LocaltimeJs(host, memoryIndex)
    MMAP_JS -> MmapJs(host)
    MUNMAP_JS -> MunmapJs(host)
    SETITIMER_JS -> SetittimerJs(host)
    SYSCALL_CHMOD -> SyscallChmod(host, memoryIndex)
    SYSCALL_FACCESSAT -> SyscallFaccessat(host, memoryIndex)
    SYSCALL_FCHMOD -> SyscallFchmod(host)
    SYSCALL_FCHOWN32 -> SyscallFchown32(host)
    SYSCALL_FCNTL64 -> SyscallFcntl64(host, memoryIndex)
    SYSCALL_FDATASYNC -> SyscallFdatasync(host)
    SYSCALL_FSTAT64 -> SyscallFstat64(host, memoryIndex)
    SYSCALL_FTRUNCATE64 -> SyscallFtruncate64(host)
    SYSCALL_GETCWD -> SyscallGetcwd(host, memoryIndex)
    SYSCALL_LSTAT64 -> syscallLstat64(host, memoryIndex)
    SYSCALL_MKDIRAT -> SyscallMkdirat(host, memoryIndex)
    SYSCALL_OPENAT -> SyscallOpenat(host, memoryIndex)
    SYSCALL_READLINKAT -> SyscallReadlinkat(host, memoryIndex)
    SYSCALL_RMDIR -> SyscallRmdir(host, memoryIndex)
    SYSCALL_STAT64 -> syscallStat64(host, memoryIndex)
    SYSCALL_UNLINKAT -> SyscallUnlinkat(host, memoryIndex)
    SYSCALL_UTIMENSAT -> SyscallUtimensat(host, memoryIndex)
    TZSET_JS -> TzsetJs(host, memoryIndex)
    else -> NotImplementedEmscriptenFunction(this)
}.function
