plugins {
    kotlin("jvm")
    id("dev.jwillert.daisyui")
    id("dev.jwillert.ktor-vrt")
}

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
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

// The daisyui plugin writes generated CSS into src/main/resources/static/css;
// exclude it so processResources does not gain an implicit dependency on buildCss.
sourceSets {
    named("main") {
        resources.exclude("static/css/**")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
}

// Generate real Tailwind/DaisyUI CSS from the component sources.
daisyui {
    scanPaths = listOf("src")
    themes = listOf("light")
}

// Keep `build`/CI free of the Node/Tailwind toolchain — only the VRT tasks need CSS.
tasks.named("processResources") {
    setDependsOn(emptyList<Any>())
}

// The vrt source set + vrtTest/vrtTestDocker tasks come from the dev.jwillert.ktor-vrt
// plugin; this block only supplies the project-specific configuration.
ktorVrt {
    css.set(layout.projectDirectory.file("src/main/resources/static/css/output.css"))
    cssTaskDependency.set("buildCss")
    htmlAttributes.put("data-theme", "light")
    wrapperClasses.set(listOf("inline-block", "p-4"))
    // goldenDir defaults to src/vrt/resources/golden — unchanged.
}
