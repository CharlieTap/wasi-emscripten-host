/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.multiplatform.publish

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.Platform
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

internal fun Project.configureWehPublishing(platform: Platform) {
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    val wehVersions = createWehVersionsExtension()

    group = "io.github.charlietap.wasi.emscripten.host"
    version = wehVersions.rootVersion.get()

    extensions.configure<MavenPublishBaseExtension> {
        publishToMavenCentral()
        signAllPublications()

        configure(platform)

        pom {
            name.set(project.name)
            description.set(
                "Kotlin Multiplatform implementation of WebAssembly WASI Preview 1 host functions for Chasm",
            )
            url.set("https://github.com/CharlieTap/wasi-emscripten-host")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("CharlieTap")
                    name.set("Charlie Tapping")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/CharlieTap/wasi-emscripten-host.git")
                developerConnection.set("scm:git:ssh://git@github.com/CharlieTap/wasi-emscripten-host.git")
                url.set("https://github.com/CharlieTap/wasi-emscripten-host")
            }
        }
    }
}
