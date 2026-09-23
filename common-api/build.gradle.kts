/*
* Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
* for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
* SPDX-License-Identifier: Apache-2.0
*/

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    id("at.released.weh.gradle.lint.abi-validation")
    id("at.released.weh.gradle.lint.android-lint-noagp")
    id("at.released.weh.gradle.multiplatform.kotlin")
    id("at.released.weh.gradle.multiplatform.publish")
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }
    iosSimulatorArm64()
    iosArm64()
    linuxArm64()
    linuxX64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
