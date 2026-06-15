allprojects {
    group = "dev.jwillert.ktor.daisyui"
    version = rootProject.version
}

tasks.register("publishAll") {
    group = "publishing"
    description = "Publishes ko-components and the included daisyui plugin build"
    dependsOn(":ko-components:publish")
    dependsOn(gradle.includedBuild("plugin").task(":publish"))
}
