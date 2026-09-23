/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("at.released.weh.gradle.lint.abi-validation")
    id("at.released.weh.gradle.lint.android-lint-noagp")
    id("at.released.weh.gradle.multiplatform.kotlin")
    id("at.released.weh.gradle.multiplatform.poko")
}

group = "at.released.weh"

kotlin {
    jvm()
    iosSimulatorArm64()
    iosArm64()
    linuxArm64()
    linuxX64()
    macosArm64()
    mingwX64 {
        binaries.all {
            linkerOpts("-lntdll")
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.commonApi)
            api(projects.host)
            api(projects.wasmCore)
            api(projects.wasmWasiPreview1Core)
            implementation(libs.arrow.core)
            implementation(projects.commonUtil)
        }
        commonTest.dependencies {
            implementation(projects.testIoBootstrap)
            implementation(projects.testLogger)
            implementation(projects.hostTestFixtures)
            implementation(projects.wasmCoreTestFixtures)
            implementation(kotlin("test"))
            implementation(libs.assertk)
            implementation(libs.tempfolder)
        }
    }
}
