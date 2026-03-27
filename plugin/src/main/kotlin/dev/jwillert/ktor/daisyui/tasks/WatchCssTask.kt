package dev.jwillert.ktor.daisyui.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class WatchCssTask : DefaultTask() {

    private val inputCss: File = project.file("src/main/resources/static/css/input.css")
    private val outputCss: File = project.file("src/main/resources/static/css/output.css")

    init {
        group = "daisyui"
        description = "Watch and rebuild CSS on changes"
    }

    @TaskAction
    fun watch() {
        logger.lifecycle("Starting Tailwind watch mode...")
        logger.lifecycle("Watching: ${inputCss.relativeTo(project.projectDir)}")
        logger.lifecycle("Output: ${outputCss.relativeTo(project.projectDir)}")
        logger.lifecycle("Press Ctrl+C to stop")

        val processBuilder = ProcessBuilder(
            "npx", "@tailwindcss/cli",
            "-i", inputCss.absolutePath,
            "-o", outputCss.absolutePath,
            "--watch"
        )

        processBuilder.inheritIO()
        processBuilder.directory(project.projectDir)

        val process = processBuilder.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            logger.lifecycle("Stopping Tailwind watch...")
            process.destroy()
            process.waitFor()
            logger.lifecycle("Tailwind watch stopped")
        })

        try {
            val exitCode = process.waitFor()

            if (exitCode == 0 || exitCode == 130) {
                logger.lifecycle("Tailwind watch stopped normally")
            } else {
                logger.error("Tailwind watch exited with code: $exitCode")
            }
        } catch (e: InterruptedException) {
            process.destroy()
            logger.lifecycle("Tailwind watch interrupted")
        }
    }
}
