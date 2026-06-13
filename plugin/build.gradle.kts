plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "dev.jwillert.ktor.daisyui"
version = providers.gradleProperty("version").getOrElse("0.0.0-SNAPSHOT")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("daisyuiPlugin") {
            id = "dev.jwillert.daisyui"
            implementationClass = "dev.jwillert.ktor.daisyui.DaisyUiPlugin"
            displayName = "Daisyui — Tailwind CSS + DaisyUI + Component Registry"
            description = "Installs Tailwind CSS with DaisyUI and provides a shadcn-like component registry for Kotlin HTML DSL"
            tags = listOf("tailwind", "daisyui", "ktor", "kotlin", "htmx", "components")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jwillert/ktor-daisyui")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
