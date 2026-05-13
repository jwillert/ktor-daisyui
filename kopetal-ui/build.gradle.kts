plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "dev.jwillert"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kopetal-registry"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
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
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.jwillert"
            artifactId = "kopetal-ui"
            from(components["java"])
        }
    }
}
