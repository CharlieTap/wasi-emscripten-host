/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.multiplatform

import at.released.weh.gradle.multiplatform.publish.configureWehPublishing
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

/*
 * Convention plugin with publishing defaults for Android libraries
 */
plugins {
    id("com.vanniktech.maven.publish.base")
}

configureWehPublishing(
    AndroidSingleVariantLibrary(
        javadocJar = JavadocJar.Empty(),
        sourcesJar = SourcesJar.Sources(),
        variant = "release",
    ),
)
