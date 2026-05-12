package dev.jwillert.kopetal.forms

import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import dev.jwillert.kopetal.forms.formField

class KopetalFormsRegistryTest {

    private val originalButton = KopetalFormsRegistry.button
    private val originalInput = KopetalFormsRegistry.input

    @AfterEach
    fun restore() {
        KopetalFormsRegistry.button = originalButton
        KopetalFormsRegistry.input = originalInput
    }

    @Test
    fun `button slot can be overridden`() {
        var called = false
        KopetalFormsRegistry.button = { _, _ -> called = true }

        buildString {
            appendHTML().div {
                this@div.apply { KopetalFormsRegistry.button(this, "Click me", false) }
            }
        }

        assertTrue(called)
    }

    @Test
    fun `input slot can be overridden independently of button`() {
        var inputCalled = false
        KopetalFormsRegistry.input = { _, _, _ -> inputCalled = true }

        buildString {
            appendHTML().div {
                this@div.apply { KopetalFormsRegistry.input(this, "email", "email", true) }
            }
        }

        assertTrue(inputCalled)
    }

    @Test
    fun `default button renders with label and btn class`() {
        installKopetalFormsDefaults()

        val html = createHTML().div {
            KopetalFormsRegistry.button(this, "Save", false)
        }

        assertTrue(html.contains("Save"), "Expected label in output: $html")
        assertTrue(html.contains("btn"), "Expected btn class in output: $html")
    }

    @Test
    fun `default input renders with name attribute`() {
        installKopetalFormsDefaults()

        val html = createHTML().div {
            KopetalFormsRegistry.input(this, "username", "text", false)
        }

        assertTrue(html.contains("username"), "Expected name in output: $html")
        assertTrue(html.contains("input"), "Expected input element in output: $html")
    }

    @Test
    fun `formField renders label text and delegates input to registry`() {
        installKopetalFormsDefaults()

        val html = createHTML().div {
            formField("Email Address", "email", type = "email", required = true)
        }

        assertTrue(html.contains("Email Address"), "Expected label in output: $html")
        assertTrue(html.contains("form-control"), "Expected form-control class in output: $html")
        assertTrue(html.contains("required"), "Expected required attribute in output: $html")
    }

    @Test
    fun `formField uses overridden input slot`() {
        var capturedName = ""
        var capturedRequired = false
        KopetalFormsRegistry.input = { name, _, required ->
            capturedName = name
            capturedRequired = required
        }

        createHTML().div {
            formField("Email", "my-email", required = true)
        }

        assertTrue(capturedName == "my-email", "Expected name 'my-email', got '$capturedName'")
        assertTrue(capturedRequired, "Expected required=true")
    }
}
