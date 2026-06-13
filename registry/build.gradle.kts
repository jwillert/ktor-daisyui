plugins {
    kotlin("jvm")
    id("dev.jwillert.daisyui")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

// Flat component layout for `main`; tests and VRT live outside `src/` so the
// component directories stay unpolluted.
sourceSets {
    named("main") {
        kotlin.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("src/main/resources"))
    }
    named("test") {
        kotlin.setSrcDirs(listOf("test/kotlin"))
        resources.setSrcDirs(listOf("test/resources"))
    }
    create("vrt") {
        kotlin.setSrcDirs(listOf("vrt/kotlin"))
        resources.setSrcDirs(listOf("vrt/resources"))
    }
}

val main = sourceSets["main"]
val vrt = sourceSets["vrt"]
vrt.compileClasspath += main.output
vrt.runtimeClasspath += main.output

configurations["vrtImplementation"].extendsFrom(configurations["implementation"])
configurations["vrtRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])

val playwrightVersion = "1.49.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    "vrtImplementation"("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    "vrtImplementation"("com.microsoft.playwright:playwright:$playwrightVersion")
    "vrtImplementation"("org.testcontainers:testcontainers:1.20.4")
    "vrtImplementation"("com.github.romankh3:image-comparison:4.4.0")
    "vrtImplementation"("io.kotest:kotest-runner-junit5:5.9.1")
    "vrtImplementation"("io.kotest:kotest-assertions-core:5.9.1")
    "vrtImplementation"("io.kotest:kotest-framework-datatest:5.9.1")
}

// Generate real Tailwind/DaisyUI CSS from the component sources.
daisyui {
    scanPaths = listOf("src")
    themes = listOf("light")
}

// The plugin wires processResources -> buildCss. We only need CSS for the VRT
// tasks (which depend on buildCss explicitly), so detach it to keep `build`/CI
// free of the Node/Tailwind toolchain.
tasks.named("processResources") {
    setDependsOn(emptyList<Any>())
}

val outputCss = layout.projectDirectory.file("src/main/resources/static/css/output.css")
val goldenDir = layout.projectDirectory.dir("vrt/resources/golden")
val diffDir = layout.buildDirectory.dir("vrt/diff")

fun Test.configureVrt(mode: String) {
    group = "verification"
    description = "Visual regression tests (mode=$mode)"
    testClassesDirs = vrt.output.classesDirs
    classpath = vrt.runtimeClasspath
    useJUnitPlatform()
    dependsOn("buildCss")
    systemProperty("vrt.mode", mode)
    systemProperty("vrt.css", outputCss.asFile.absolutePath)
    systemProperty("vrt.goldenDir", goldenDir.asFile.absolutePath)
    systemProperty("vrt.diffDir", diffDir.get().asFile.absolutePath)
    if (project.hasProperty("updateGoldens")) systemProperty("vrt.updateGoldens", "true")
    outputs.upToDateWhen { false }
    testLogging { showStandardStreams = true }
}

tasks.register<Test>("vrtTest") {
    configureVrt("local")
}

tasks.register<Test>("vrtTestDocker") {
    configureVrt("docker")
}
