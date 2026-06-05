package dev.jwillert.ko.components

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FormFieldTest {

    @Test
    fun `formField renders label text and form-control`() {
        val html = createHTML().div { formField("Email Address", "email") }
        assertTrue(html.contains("Email Address"), "Expected label text: $html")
        assertTrue(html.contains("form-control"), "Expected form-control class: $html")
    }

    @Test
    fun `formField passes required to input`() {
        val html = createHTML().div { formField("Email", "email", required = true) }
        assertTrue(html.contains("required"), "Expected required: $html")
    }

    @Test
    fun `formField passes type to input`() {
        val html = createHTML().div { formField("Username", "username", type = "password") }
        assertTrue(html.contains("password"), "Expected password type: $html")
    }
}
