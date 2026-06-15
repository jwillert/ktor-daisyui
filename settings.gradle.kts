pluginManagement {
    includeBuild("plugin")
    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jwillert/ktor-plugins")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
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
