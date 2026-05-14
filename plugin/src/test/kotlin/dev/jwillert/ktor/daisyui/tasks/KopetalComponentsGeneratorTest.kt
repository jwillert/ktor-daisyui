package dev.jwillert.ktor.daisyui.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KopetalComponentsGeneratorTest {

    @Test
    fun `generateConfigureStatement returns null when slotRegistryClass is null`() {
        val entry = ComponentEntry(
            name = "modal",
            description = "modal",
            file = "src/modal/Modal.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = null,
            slotKey = null,
            slotParams = null
        )
        assertEquals(null, generateConfigureStatement(entry))
    }

    @Test
    fun `generateConfigureStatement returns null when slotKey is null even if slotRegistryClass is set`() {
        val entry = ComponentEntry(
            name = "button",
            description = "button",
            file = "src/button/Button.kt",
            kotlinPackage = "pkg",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = null,
            slotParams = "label: String, disabled: Boolean"
        )
        assertEquals(null, generateConfigureStatement(entry))
    }

    @Test
    fun `generateConfigureStatement returns null when slotParams is null even if slotKey is set`() {
        val entry = ComponentEntry(
            name = "button",
            description = "button",
            file = "src/button/Button.kt",
            kotlinPackage = "pkg",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = "dev.jwillert.kopetal.ButtonKey",
            slotParams = null
        )
        assertEquals(null, generateConfigureStatement(entry))
    }

    @Test
    fun `generateConfigureStatement generates correct adapter for button`() {
        val entry = ComponentEntry(
            name = "button",
            description = "button",
            file = "src/button/Button.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = "dev.jwillert.kopetal.ButtonKey",
            slotParams = "label: String, disabled: Boolean, block: BUTTON.() -> Unit"
        )
        assertEquals(
            "KopetalRegistry[ButtonKey] = { label, disabled, block -> koButton(label = label, disabled = disabled, block = block) }",
            generateConfigureStatement(entry)
        )
    }

    @Test
    fun `generateConfigureStatement generates correct adapter for input`() {
        val entry = ComponentEntry(
            name = "input",
            description = "input",
            file = "src/input/Input.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = "dev.jwillert.kopetal.InputKey",
            slotParams = "name: String, type: String, required: Boolean, block: INPUT.() -> Unit"
        )
        assertEquals(
            "KopetalRegistry[InputKey] = { name, type, required, block -> koInput(name = name, type = type, required = required, block = block) }",
            generateConfigureStatement(entry)
        )
    }

    @Test
    fun `generateConfigureStatement handles BUTTON block parameter in slotParams`() {
        val entry = ComponentEntry(
            name = "button",
            description = "button",
            file = "src/button/Button.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = "dev.jwillert.kopetal.ButtonKey",
            slotParams = "label: String, disabled: Boolean, block: BUTTON.() -> Unit"
        )
        assertEquals(
            "KopetalRegistry[ButtonKey] = { label, disabled, block -> koButton(label = label, disabled = disabled, block = block) }",
            generateConfigureStatement(entry)
        )
    }

    @Test
    fun `generateConfigureStatement handles INPUT block parameter in slotParams`() {
        val entry = ComponentEntry(
            name = "input",
            description = "input",
            file = "src/input/Input.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = "dev.jwillert.kopetal.InputKey",
            slotParams = "name: String, type: String, required: Boolean, block: INPUT.() -> Unit"
        )
        assertEquals(
            "KopetalRegistry[InputKey] = { name, type, required, block -> koInput(name = name, type = type, required = required, block = block) }",
            generateConfigureStatement(entry)
        )
    }

    @Test
    fun `generateKopetalComponentsContent returns empty string when no slot entries`() {
        val entries = listOf(
            ComponentEntry("modal", "modal", "src/modal/Modal.kt", "pkg", null, null, null),
            ComponentEntry("card", "card", "src/card/Card.kt", "pkg", null, null, null),
        )
        assertEquals(
            "",
            generateKopetalComponentsContent(
                installedNames = setOf("modal", "card"),
                allRegistryEntries = entries,
                packageName = "com.example.components"
            )
        )
    }

    @Test
    fun `generateKopetalComponentsContent generates correct file for button and input`() {
        val entries = listOf(
            ComponentEntry(
                "button", "button", "src/button/Button.kt", "pkg",
                "dev.jwillert.kopetal.KopetalRegistry",
                "dev.jwillert.kopetal.ButtonKey",
                "label: String, disabled: Boolean, block: BUTTON.() -> Unit"
            ),
            ComponentEntry(
                "input", "input", "src/input/Input.kt", "pkg",
                "dev.jwillert.kopetal.KopetalRegistry",
                "dev.jwillert.kopetal.InputKey",
                "name: String, type: String, required: Boolean, block: INPUT.() -> Unit"
            ),
            ComponentEntry("modal", "modal", "src/modal/Modal.kt", "pkg", null, null, null),
        )
        val result = generateKopetalComponentsContent(
            installedNames = setOf("button", "input"),
            allRegistryEntries = entries,
            packageName = "com.example.components"
        )

        val expected = "package com.example.components\n" +
            "\n" +
            "import dev.jwillert.kopetal.ButtonKey\n" +
            "import dev.jwillert.kopetal.InputKey\n" +
            "import dev.jwillert.kopetal.KopetalRegistry\n" +
            "\n" +
            "// Auto-generated by addComponent — do not edit manually\n" +
            "object KopetalComponents {\n" +
            "    fun configure() {\n" +
            "        KopetalRegistry[ButtonKey] = { label, disabled, block -> koButton(label = label, disabled = disabled, block = block) }\n" +
            "        KopetalRegistry[InputKey] = { name, type, required, block -> koInput(name = name, type = type, required = required, block = block) }\n" +
            "    }\n" +
            "}"
        assertEquals(expected, result)
    }

    @Test
    fun `generateKopetalComponentsContent only includes installed components`() {
        val entries = listOf(
            ComponentEntry(
                "button", "button", "src/button/Button.kt", "pkg",
                "dev.jwillert.kopetal.KopetalRegistry",
                "dev.jwillert.kopetal.ButtonKey",
                "label: String, disabled: Boolean, block: BUTTON.() -> Unit"
            ),
            ComponentEntry(
                "input", "input", "src/input/Input.kt", "pkg",
                "dev.jwillert.kopetal.KopetalRegistry",
                "dev.jwillert.kopetal.InputKey",
                "name: String, type: String, required: Boolean, block: INPUT.() -> Unit"
            ),
        )
        val result = generateKopetalComponentsContent(
            installedNames = setOf("button"),
            allRegistryEntries = entries,
            packageName = "com.example.components"
        )

        assertTrue(result.contains("KopetalRegistry[ButtonKey]"), "Expected button slot")
        assertTrue(!result.contains("KopetalRegistry[InputKey]"), "Expected no input slot: $result")
    }
}
