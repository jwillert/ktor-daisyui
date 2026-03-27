package dev.jwillert.ktor.daisyui

import dev.jwillert.ktor.daisyui.tasks.AddComponentTask
import dev.jwillert.ktor.daisyui.tasks.BuildCssTask
import dev.jwillert.ktor.daisyui.tasks.GenerateConfigTask
import dev.jwillert.ktor.daisyui.tasks.InstallTailwindTask
import dev.jwillert.ktor.daisyui.tasks.WatchCssTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class DaisyUiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "daisyui",
            DaisyuiExtension::class.java
        )

        val installTask = project.tasks.register(
            "installTailwind",
            InstallTailwindTask::class.java
        ) {
            tailwindVersion.set(extension.tailwindVersion)
            daisyuiVersion.set(extension.daisyuiVersion)
        }

        val configTask = project.tasks.register(
            "generateTailwindConfig",
            GenerateConfigTask::class.java
        ) {
            scanPaths.set(extension.scanPaths)
            themes.set(extension.themes)
        }

        val buildTask = project.tasks.register(
            "buildCss",
            BuildCssTask::class.java
        ) {
            dependsOn(installTask, configTask)
            minify.set(extension.minify)
        }

        project.tasks.register(
            "watchCss",
            WatchCssTask::class.java
        ) {
            dependsOn(installTask, configTask)
        }

        project.tasks.register(
            "addComponent",
            AddComponentTask::class.java
        ) {
            componentsOutputDir.set(extension.componentsOutputDir)
            registryUrl.set(extension.registryUrl)
        }

        project.tasks.named("processResources") {
            dependsOn(buildTask)
        }

        project.logger.lifecycle("Daisyui Plugin applied to ${project.name}")
    }
}
