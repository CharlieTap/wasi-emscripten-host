plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            mavenContent {
                snapshotsOnly()
            }
            content {
                includeGroup("io.github.charlietap.chasm")
                includeGroup("io.github.charlietap.wasi.emscripten.host")
            }
        }
        mavenCentral()
    }
}

providers.gradleProperty("weh.source").orNull?.let { wehSource ->
    includeBuild(wehSource)
}

rootProject.name = "wasm-gradle"
include("app-chasm-emscripten")
include("app-chasm-wasip1")
