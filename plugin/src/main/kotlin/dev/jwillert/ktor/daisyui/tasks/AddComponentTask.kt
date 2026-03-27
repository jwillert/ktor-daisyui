package dev.jwillert.kopetal.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

data class ComponentEntry(
    val name: String,
    val description: String,
    val file: String,
    val kotlinPackage: String
)

abstract class AddComponentTask : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val componentName: Property<String>

    @get:Input
    abstract val componentsOutputDir: Property<String>

    @get:Input
    abstract val registryUrl: Property<String>

    init {
        group = "daisyui"
        description = "Add a Kopetal component to your project (shadcn-like registry)"
    }

    @TaskAction
    fun addComponent() {
        val name = componentName.orNull
            ?: throw GradleException(
                "No component specified. Use: ./gradlew addComponent --componentName=<name>"
            )

        logger.lifecycle("Fetching registry from ${registryUrl.get()} ...")
        val registry = fetchRegistry()

        val component = registry.find { it.name == name }
            ?: throw GradleException(
                "Component '$name' not found in registry.\nAvailable components: ${registry.joinToString(", ") { it.name }}"
            )

        logger.lifecycle("Adding component '${component.name}': ${component.description}")

        val componentSource = fetchComponentSource(component)

        val outputDir = project.file(componentsOutputDir.get())
        outputDir.mkdirs()

        val targetFile = File(outputDir, "${component.name.replaceFirstChar { it.uppercaseChar() }}.kt")

        if (targetFile.exists()) {
            logger.lifecycle("Component file already exists at ${targetFile.relativeTo(project.projectDir)} — overwriting.")
        }

        val packageDeclaration = "package ${derivePackage(outputDir)}"
        val sourceWithPackage = if (componentSource.startsWith("package ")) {
            val lines = componentSource.lines().toMutableList()
            lines[0] = packageDeclaration
            lines.joinToString("\n")
        } else {
            "$packageDeclaration\n\n$componentSource"
        }

        targetFile.writeText(sourceWithPackage)

        logger.lifecycle("Component '${component.name}' added at ${targetFile.relativeTo(project.projectDir)}")
    }

    private fun fetchRegistry(): List<ComponentEntry> {
        val url = registryUrl.get()
        val json = URI(url).toURL().readText()
        return parseRegistry(json)
    }

    private fun fetchComponentSource(component: ComponentEntry): String {
        val baseUrl = registryUrl.get().substringBeforeLast("/")
        val fileUrl = "$baseUrl/${component.file}"
        return URI(fileUrl).toURL().readText()
    }

    private fun derivePackage(outputDir: File): String {
        val srcMain = project.file("src/main/kotlin")
        return if (outputDir.startsWith(srcMain)) {
            srcMain.toPath().relativize(outputDir.toPath())
                .toString()
                .replace(File.separatorChar, '.')
        } else {
            "components"
        }
    }

    private fun parseRegistry(json: String): List<ComponentEntry> {
        val entries = mutableListOf<ComponentEntry>()
        val objectPattern = Regex("""\{[^}]+\}""")
        val fieldPattern = Regex(""""(\w+)"\s*:\s*"([^"]+)"""")

        objectPattern.findAll(json).forEach { match ->
            val fields = fieldPattern.findAll(match.value).associate { it.groupValues[1] to it.groupValues[2] }
            val name = fields["name"] ?: return@forEach
            val description = fields["description"] ?: ""
            val file = fields["file"] ?: return@forEach
            val kotlinPackage = fields["kotlinPackage"] ?: "dev.jwillert.ktor.daisyui.components"
            entries.add(ComponentEntry(name, description, file, kotlinPackage))
        }

        return entries
    }
}
