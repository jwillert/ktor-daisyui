plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ko-components"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    implementation("io.ktor:ktor-server-netty:3.1.3")
    implementation("io.ktor:ktor-server-html-builder:3.1.3")
    implementation("ch.qos.logback:logback-classic:1.5.13")
}

application {
    mainClass.set("sample.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}
