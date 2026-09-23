plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

providers.gradleProperty("weh.source").orNull?.let { wehSource ->
    includeBuild(wehSource)
}

rootProject.name = "wasm-gradle"
include("app-chasm-emscripten")
include("app-chasm-wasip1")
