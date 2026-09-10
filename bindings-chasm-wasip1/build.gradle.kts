/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("at.released.weh.gradle.lint.binary-compatibility-validator")
    id("at.released.weh.gradle.multiplatform.kotlin")
    id("com.android.kotlin.multiplatform.library")
    id("at.released.weh.gradle.multiplatform.publish")
    id("at.released.weh.gradle.wasm.codegen.chasm.chasm-adapter-generator")
}

kotlin {
    android {
        namespace = "at.released.weh.bindings.chasm.wasip1"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        withHostTest {}
    }
    jvm()
    iosSimulatorArm64()
    iosArm64()
    linuxArm64()
    linuxX64()
    macosArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val byteArrayMain by creating {
            dependsOn(commonMain.get())
        }
        nativeMain.get().dependsOn(byteArrayMain)
        androidMain.get().dependsOn(byteArrayMain)

        val byteArrayTest by creating {
            dependsOn(commonTest.get())
        }
        nativeTest.get().dependsOn(byteArrayTest)
        getByName("androidHostTest").dependsOn(byteArrayTest)

        commonMain.dependencies {
            api(projects.host)
            api(libs.chasm)
            api(projects.commonApi)
            implementation(projects.commonUtil)
            implementation(projects.wasmWasiPreview1)
            implementation(libs.kotlinx.io)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
        }
    }
}
