/*
 * Copyright 2024-2025, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.gradle.multiplatform

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/*
 * Convention plugin that configures Kotlin in projects with the Kotlin Multiplatform plugin
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    explicitApi = ExplicitApiMode.Warning

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )
    }

    sourceSets {
        all {
            languageSettings {
                languageVersion = "2.4"
                apiVersion = "2.4"
                listOf(
                    "kotlin.RequiresOptIn",
                    "kotlin.ExperimentalStdlibApi",
                    "kotlin.time.ExperimentalTime",
                    "at.released.weh.common.api.InternalWasiEmscriptenHostApi",
                ).forEach(::optIn)
            }
        }
        matching { sourceSet ->
            listOf("apple", "ios", "linux", "macos", "mingw", "native").any { prefix ->
                sourceSet.name.startsWith(prefix, ignoreCase = true)
            }
        }.configureEach {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-jvm-default=no-compatibility",
            "-Xlambdas=indy",
        )
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_17
}
