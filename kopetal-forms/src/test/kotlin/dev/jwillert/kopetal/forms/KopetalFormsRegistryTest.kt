package dev.jwillert.kopetal.forms

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
        KopetalFormsRegistry.button = { label, disabled ->
            called = true
        }
        buildString {
            appendHTML().div {
                this@div.apply { KopetalFormsRegistry.button.invoke(this, "Click me", false) }
            }
        }
        assertTrue(called)
    }

    @Test
    fun `input slot can be overridden independently of button`() {
        var inputCalled = false
        KopetalFormsRegistry.input = { name, type, required ->
            inputCalled = true
        }

        buildString {
            appendHTML().div {
                this@div.apply { KopetalFormsRegistry.input.invoke(this, "email", "email", true) }
            }
        }

        assertTrue(inputCalled)
    }
}
