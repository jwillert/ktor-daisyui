pluginManagement {
    includeBuild("plugin")
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "2.3.10"
        id("dev.jwillert.ktor-vrt") version "0.3.0"
    }
}

rootProject.name = "ktor-daisyui"

include("registry")
include("ko-components")
include("sample")
