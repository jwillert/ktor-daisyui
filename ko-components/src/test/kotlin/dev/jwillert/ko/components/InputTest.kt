package dev.jwillert.ko.components

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InputTest {

    @Test
    fun `koInput renders name attribute and input element`() {
        val html = createHTML().div { koInput("username") }
        assertTrue(html.contains("username"), "Expected name: $html")
        assertTrue(html.contains("<input"), "Expected input element: $html")
    }

    @Test
    fun `koInput renders input-bordered class`() {
        val html = createHTML().div { koInput("email") }
        assertTrue(html.contains("input"), "Expected input class: $html")
    }

    @Test
    fun `koInput renders required attribute when required`() {
        val html = createHTML().div { koInput("email", required = true) }
        assertTrue(html.contains("required"), "Expected required: $html")
    }

    @Test
    fun `koInput renders placeholder when set`() {
        val html = createHTML().div { koInput("email", placeholder = "Enter email") }
        assertTrue(html.contains("Enter email"), "Expected placeholder: $html")
    }

    @Test
    fun `koInput renders disabled attribute when disabled`() {
        val html = createHTML().div { koInput("field", disabled = true) }
        assertTrue(html.contains("disabled"), "Expected disabled: $html")
    }
}
