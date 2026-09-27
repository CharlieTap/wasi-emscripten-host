/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.test.android

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import at.released.weh.bindings.chasm.wasip1.ChasmWasiPreview1Builder
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.expect
import io.github.charlietap.chasm.embedding.store
import java.io.File

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EmbedderHost {
            fileSystem {
                addPreopenedDirectory(filesDir.absolutePath, "/data")
            }
        }.use { host ->
            val store = store()
            val binary = assets.open("open-file.wasm").use { it.readBytes() }
            val module = module(binary).expect("decode WASI consumer")
            val imports = ChasmWasiPreview1Builder(store, module) {
                this.host = host
            }.buildRequired()
            val instance = instance(store, module, imports).expect("instantiate WASI consumer")
            invoke(store, instance, "run").expect("open and close file through WASI")
        }

        check(File(filesDir, "probe.txt").isFile)
        setContentView(TextView(this).apply { text = "WASI path_open / fd_close passed" })
    }
}
