/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.memory

import at.released.weh.filesystem.FileSystem
import at.released.weh.wasi.preview1.memory.DirectWasiMemoryReader
import io.github.charlietap.chasm.host.HostMemory

internal expect class ChasmWasiMemoryReader(fileSystem: FileSystem) : DirectWasiMemoryReader<HostMemory>
