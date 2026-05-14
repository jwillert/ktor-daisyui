package dev.jwillert.kopetal.forms

import dev.jwillert.kopetal.ButtonKey
import dev.jwillert.kopetal.InputKey
import dev.jwillert.kopetal.KopetalRegistry
import dev.jwillert.kopetal.koButton
import dev.jwillert.kopetal.koInput
import dev.jwillert.kopetal.forms.components.KopetalComponents
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KopetalFormsRegistryTest {

    @AfterEach
    fun restore() {
        KopetalRegistry.remove(ButtonKey)
        KopetalRegistry.remove(InputKey)
    }

    @Test
    fun `button slot can be overridden`() {
        var called = false
        KopetalRegistry[ButtonKey] = { _, _, _ -> called = true }
        createHTML().div { koButton("Click me") }
        assertTrue(called)
    }

    @Test
    fun `input slot can be overridden independently of button`() {
        var inputCalled = false
        KopetalRegistry[InputKey] = { _, _, _, _ -> inputCalled = true }
        createHTML().div { koInput("email") }
        assertTrue(inputCalled)
    }

    @Test
    fun `default button renders with label and btn class`() {
        KopetalComponents.configure()

        val html = createHTML().div {
            koButton("Save")
        }

        assertTrue(html.contains("Save"), "Expected label in output: $html")
        assertTrue(html.contains("btn"), "Expected btn class in output: $html")
    }

    @Test
    fun `default input renders with name attribute`() {
        KopetalComponents.configure()

        val html = createHTML().div {
            koInput("username")
        }

        assertTrue(html.contains("username"), "Expected name in output: $html")
        assertTrue(html.contains("input"), "Expected input element in output: $html")
    }

    @Test
    fun `formField renders label text and delegates input to registry`() {
        KopetalComponents.configure()

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
        KopetalRegistry[InputKey] = { name, _, required, _ ->
            capturedName = name
            capturedRequired = required
        }

        createHTML().div {
            formField("Email", "my-email", required = true)
        }

        assertTrue(capturedName == "my-email", "Expected name 'my-email', got '$capturedName'")
        assertTrue(capturedRequired, "Expected required=true")
    }

    @Test
    fun `koButton block is invoked and can set html attributes`() {
        KopetalComponents.configure()
        val html = createHTML().div {
            koButton("Save") {
                attributes["data-test"] = "yes"
            }
        }
        assertTrue(html.contains("data-test=\"yes\""), "Expected data-test attribute: $html")
    }
}
