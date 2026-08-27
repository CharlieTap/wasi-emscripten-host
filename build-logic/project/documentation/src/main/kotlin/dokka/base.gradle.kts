/*
 * Copyright 2024-2025, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.documentation.dokka

import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters

/*
 * Base configuration of dokkatoo
 */
plugins {
    id("org.jetbrains.dokka")
}

@Suppress("UnstableApiUsage")
private val htmlResourcesRoot = layout.settingsDirectory.dir("aggregate-documentation")

extensions.configure<DokkaExtension> {
    dokkaPublications.configureEach {
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(true)
    }

    dokkaSourceSets.configureEach {
        includes.from(
            "MODULE.md",
        )
        sourceLink {
            localDirectory.set(project.layout.projectDirectory)
            val remoteUrlSubpath = project.path.replace(':', '/')
            remoteUrl("https://github.com/CharlieTap/wasi-emscripten-host/tree/main$remoteUrlSubpath")
        }
        externalDocumentationLinks {
            create("arrow") {
                url("https://apidocs.arrow-kt.io")
            }
        }
    }

    pluginsConfiguration.withType<DokkaHtmlPluginParameters>().configureEach {
        homepageLink.set("https://weh.released.at")
        footerMessage.set("(C) wasi-emscripten-host project authors and contributors")
        customStyleSheets.from(
            htmlResourcesRoot.file("styles/font-jb-sans-auto.css"),
            htmlResourcesRoot.file("styles/weh.css"),
        )
    }
}
