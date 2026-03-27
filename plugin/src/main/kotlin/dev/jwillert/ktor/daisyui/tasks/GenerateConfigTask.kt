package dev.jwillert.ktor.daisyui.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateConfigTask : DefaultTask() {

    @get:Input
    abstract val scanPaths: ListProperty<String>

    @get:Input
    abstract val themes: ListProperty<String>

    @get:OutputFile
    val postcssConfig: File = project.file("postcss.config.js")

    @get:OutputFile
    val inputCss: File = project.file("src/main/resources/static/css/input.css")

    init {
        group = "daisyui"
        description = "Generate Tailwind config and input CSS"
    }

    @TaskAction
    fun generate() {
        val configContent = """
export default {
  plugins: {
    '@tailwindcss/postcss': {},
  },
}
        """.trimIndent()

        if (!postcssConfig.exists()) {
            postcssConfig.writeText(configContent)
            logger.lifecycle("Generated postcss.config.js")
        }

        val sourceBlock = scanPaths.get().joinToString("\n") { scanPath ->
            val scanDir = project.projectDir.resolve(scanPath)
            val relativeScanDir = inputCss.parentFile.toPath()
                .relativize(scanDir.toPath())
                .toString()
                .replace(File.separatorChar, '/')

            "@source \"$relativeScanDir\";"
        }

        inputCss.parentFile.mkdirs()
        val inputCssContent = """
@import "tailwindcss";

/* Content paths */
$sourceBlock

/* Theme customization */
@theme {
  --color-primary: #570df8;
  --font-sans: Inter, sans-serif;
}

/* DaisyUI */
@plugin "daisyui";
        """.trimIndent()

        val inputCssFile = inputCss.relativeTo(project.projectDir)
        if (!inputCss.exists()) {
            inputCss.writeText(inputCssContent)
            logger.lifecycle("Generated input.css at $inputCssFile")
        }
    }
}
