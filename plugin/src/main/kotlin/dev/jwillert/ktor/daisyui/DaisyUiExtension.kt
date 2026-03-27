package dev.jwillert.ktor.daisyui

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class DaisyuiExtension {
    abstract val tailwindVersion: Property<String>
    abstract val daisyuiVersion: Property<String>
    abstract val scanPaths: ListProperty<String>
    abstract val themes: ListProperty<String>
    abstract val minify: Property<Boolean>
    abstract val componentsOutputDir: Property<String>
    abstract val registryUrl: Property<String>

    init {
        tailwindVersion.convention("4.2.1")
        daisyuiVersion.convention("5.5.19")
        scanPaths.convention(listOf("src/main/kotlin", "src/main/resources"))
        themes.convention(listOf("light"))
        minify.convention(true)
        componentsOutputDir.convention("src/main/kotlin/components")
        registryUrl.convention("https://raw.githubusercontent.com/jwillert/ktor-daisyui/main/ktor-daisyui-components/registry.json")
    }
}
