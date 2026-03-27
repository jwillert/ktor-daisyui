plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("kopetalPlugin") {
            id = "dev.jwillert.kopetal"
            implementationClass = "dev.jwillert.kopetal.KopetalPlugin"
            displayName = "Kopetal — Tailwind CSS + DaisyUI + Component Registry"
            description = "Installs Tailwind CSS with DaisyUI and provides a shadcn-like component registry for Kotlin HTML DSL"
            tags = listOf("tailwind", "daisyui", "ktor", "kotlin", "htmx", "components")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jwillert/kopetal")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
