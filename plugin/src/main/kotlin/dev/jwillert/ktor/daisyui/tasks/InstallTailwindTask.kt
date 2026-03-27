package dev.jwillert.kopetal.tasks

import dev.jwillert.kopetal.common.execPlatformAware
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class InstallTailwindTask
@Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {

    @get:Input
    abstract val tailwindVersion: Property<String>

    @get:Input
    abstract val daisyuiVersion: Property<String>

    @get:OutputDirectory
    val nodeModules: File = project.file("node_modules")

    init {
        group = "daisyui"
        description = "Install Tailwind CSS and DaisyUI"
    }

    @TaskAction
    fun install() {
        logger.lifecycle("Installing Tailwind CSS ${tailwindVersion.get()} and DaisyUI ${daisyuiVersion.get()}...")

        val args = listOf(
            "npm", "install", "--no-save",
            "tailwindcss@${tailwindVersion.get()}",
            "@tailwindcss/cli@${tailwindVersion.get()}",
            "daisyui@${daisyuiVersion.get()}"
        )

        execOperations.execPlatformAware(project, args)

        logger.lifecycle("Tailwind installation complete!")
    }
}
