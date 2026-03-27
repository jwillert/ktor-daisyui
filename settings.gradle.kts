pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "2.3.10"
    }
}

rootProject.name = "ktor-daisyui"

include("plugin")
include("registry")
