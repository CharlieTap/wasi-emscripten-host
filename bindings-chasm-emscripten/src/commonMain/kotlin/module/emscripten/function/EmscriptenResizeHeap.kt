/*
 * Copyright 2024-2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.module.emscripten.function

import at.released.weh.bindings.chasm.module.emscripten.HostFunctionProvider
import at.released.weh.common.api.Logger
import at.released.weh.emcripten.runtime.function.EmscriptenResizeHeapFunctionHandle.Companion.calculateNewSizePages
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.Pages
import at.released.weh.wasm.core.memory.WASM_MEMORY_32_MAX_PAGES
import at.released.weh.wasm.core.memory.WASM_MEMORY_PAGE_SIZE
import io.github.charlietap.chasm.host.HostFunction
import io.github.charlietap.chasm.host.ModuleIndex
import io.github.charlietap.chasm.host.grow
import io.github.charlietap.chasm.host.readI32
import io.github.charlietap.chasm.host.withMemory
import io.github.charlietap.chasm.host.writeI32

internal class EmscriptenResizeHeap(
    host: EmbedderHost,
    private val memoryIndex: ModuleIndex.MemoryIndex,
) : HostFunctionProvider {
    private val logger: Logger = host.rootLogger.withTag("wasm-func:emscripten_resize_heap")

    override val function: HostFunction = HostFunction { parameters, results ->
        val requestedSize = parameters.readI32(0).toUInt().toLong()
        val grew = withMemory(memoryIndex) {
            val oldPages = Pages(byteSize.toLong() / WASM_MEMORY_PAGE_SIZE)
            if (requestedSize <= byteSize.toLong()) {
                true
            } else {
                val newSizePages = calculateNewSizePages(requestedSize, oldPages, WASM_MEMORY_32_MAX_PAGES)
                logger.v {
                    "emscripten_resize_heap($requestedSize). " +
                            "Requested: ${newSizePages.inBytes} bytes ($newSizePages pages)"
                }
                val pagesToAdd = (newSizePages.count - oldPages.count).toInt()
                val previousPages = grow(pagesToAdd)
                if (previousPages < 0) {
                    logger.e { "Cannot enlarge memory to $newSizePages pages" }
                    false
                } else {
                    true
                }
            }
        }
        // Emscripten's resize callback returns a C boolean: 1 on success, 0 on failure.
        results.writeI32(0, if (grew) 1 else 0)
    }
}
