/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")

package at.released.weh.gradle.documentation.docusaurus

import at.released.weh.gradle.documentation.docusaurus.BuildDocusaurusWebsiteTask.Companion.DOCUSAURUS_BUILD_DIRECTORIES
import at.released.weh.gradle.documentation.docusaurus.BuildDocusaurusWebsiteTask.Companion.registerBuildWebsiteTask
import com.github.gradle.node.NodeExtension
import com.github.gradle.node.npm.task.NpmInstallTask
import com.github.gradle.node.npm.task.NpmTask

/*
 * Convention plugin responsible for building static website using Docusaurus
 */
plugins {
    id("com.github.node-gradle.node")
}

val websiteExtension = createDocusaurusWebsiteExtension()
val websiteNodePackageDir: Provider<Directory> = layout.buildDirectory.dir("docusaurus/nodePackage")

extensions.configure<NodeExtension> {
    npmInstallCommand.set("ci")
    nodeProjectDir.set(websiteNodePackageDir)
}

val prepareNodePackageTask: TaskProvider<Sync> = tasks.register<Sync>("prepareNodePackage") {
    // npmInstall uses `npm ci`, so retaining node_modules is both unnecessary and unsafe after a lockfile change.
    // Remove it before Sync examines the destination: stale package-manager symlinks may have missing targets.
    doFirst {
        project.delete(websiteNodePackageDir.get().dir("node_modules"))
    }
    from(websiteExtension.websiteDirectory) {
        exclude(DOCUSAURUS_BUILD_DIRECTORIES)
    }
    into(websiteNodePackageDir)
    preserve {
        include(".docusaurus", "build")
    }
}

val npmInstallTask: TaskProvider<NpmInstallTask> = tasks.named<NpmInstallTask>("npmInstall")
npmInstallTask.configure {
    dependsOn(prepareNodePackageTask)
}

val checkDocusaurusWebsiteTask = tasks.register<NpmTask>("checkDocusaurusWebsite") {
    description = "Runs the Docusaurus TypeScript checks"
    dependsOn(npmInstallTask)
    args.addAll("run", "typecheck")
}

registerBuildWebsiteTask(
    websiteDirectory = websiteNodePackageDir,
    outputDirectory = websiteExtension.outputDirectory,
).configure {
    dependsOn(npmInstallTask, checkDocusaurusWebsiteTask)
}
