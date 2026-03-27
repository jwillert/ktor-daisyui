plugins {
    `kotlin-dsl` apply false
    `java-gradle-plugin` apply false
    `maven-publish` apply false
}

allprojects {
    group = "dev.jwillert.kopetal"
    version = rootProject.version
}
