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

providers.gradleProperty("weh.chasm.source").orNull?.let { chasmSource ->
    includeBuild(chasmSource)
}

rootProject.name = "wasm-gradle"
include("app-graalvm-emscripten")
include("app-graalvm-wasip1")
include("app-chicory-emscripten")
include("app-chicory-wasip1")
include("app-chasm-emscripten")
include("app-chasm-wasip1")
