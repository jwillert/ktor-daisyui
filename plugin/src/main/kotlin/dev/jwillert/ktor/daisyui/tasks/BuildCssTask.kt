package dev.jwillert.ktor.daisyui.tasks

import dev.jwillert.ktor.daisyui.common.execPlatformAware
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class BuildCssTask
@Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {

    @get:Input
    abstract val minify: Property<Boolean>

    private val inputCss: File = project.file("src/main/resources/static/css/input.css")
    private val outputCss: File = project.file("src/main/resources/static/css/output.css")

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val kotlinSources = project.fileTree("src/main/kotlin") {
        include("**/*.kt")
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputCssFile: File = inputCss

    init {
        group = "daisyui"
        description = "Build CSS with Tailwind"

        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun build() {
        logger.lifecycle("Building Tailwind CSS...")

        if (!inputCss.exists()) {
            throw RuntimeException("Input CSS not found: ${inputCss.relativeTo(project.projectDir)}")
        }

        if (outputCss.exists()) {
            outputCss.delete()
        }

        outputCss.parentFile.mkdirs()

        val args = mutableListOf(
            "npx", "@tailwindcss/cli",
            "-i", inputCss.absolutePath,
            "-o", outputCss.absolutePath
        )

        if (minify.get()) {
            args.add("--minify")
        }

        logger.lifecycle("Running: ${args.joinToString(" ")}")

        val result = execOperations.execPlatformAware(project, args) {
            isIgnoreExitValue = true
        }

        if (result.exitValue != 0) {
            throw RuntimeException("Tailwind build failed with exit code ${result.exitValue}")
        }

        if (!outputCss.exists()) {
            throw RuntimeException("Output CSS was not created!")
        }

        val sizeKb = outputCss.length() / 1024
        logger.lifecycle("CSS built successfully! Size: ${sizeKb}KB")
        logger.lifecycle("Location: ${outputCss.relativeTo(project.projectDir)}")
    }
}
