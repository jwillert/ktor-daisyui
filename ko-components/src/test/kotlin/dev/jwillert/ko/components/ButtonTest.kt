package dev.jwillert.ko.components

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ButtonTest {

    @Test
    fun `koButton renders label and btn class`() {
        val html = createHTML().div { koButton("Save") }
        assertTrue(html.contains("Save"), "Expected label: $html")
        assertTrue(html.contains("btn"), "Expected btn class: $html")
    }

    @Test
    fun `koButton default variant is primary`() {
        val html = createHTML().div { koButton("Click") }
        assertTrue(html.contains("btn-primary"), "Expected btn-primary: $html")
    }

    @Test
    fun `koButton applies secondary variant`() {
        val html = createHTML().div { koButton("Click", variant = ButtonVariant.SECONDARY) }
        assertTrue(html.contains("btn-secondary"), "Expected btn-secondary: $html")
        assertFalse(html.contains("btn-primary"), "Expected no btn-primary: $html")
    }

    @Test
    fun `koButton applies disabled class and attribute when disabled`() {
        val html = createHTML().div { koButton("Submit", disabled = true) }
        assertTrue(html.contains("btn-disabled"), "Expected btn-disabled: $html")
        assertTrue(html.contains("disabled"), "Expected disabled attribute: $html")
    }

    @Test
    fun `koButton block sets html attributes`() {
        val html = createHTML().div {
            koButton("Save") { attributes["data-test"] = "yes" }
        }
        assertTrue(html.contains("data-test=\"yes\""), "Expected data-test attribute: $html")
    }
}
