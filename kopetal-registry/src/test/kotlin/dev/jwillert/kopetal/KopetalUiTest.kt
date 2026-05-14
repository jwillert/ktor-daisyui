package dev.jwillert.kopetal

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KopetalUiTest {

    @AfterEach
    fun restore() {
        KopetalRegistry.remove(ButtonKey)
        KopetalRegistry.remove(InputKey)
    }

    @Test
    fun `koButton throws when ButtonKey not registered`() {
        assertThrows(IllegalStateException::class.java) {
            createHTML().div { koButton("Test") }
        }
    }

    @Test
    fun `koInput throws when InputKey not registered`() {
        assertThrows(IllegalStateException::class.java) {
            createHTML().div { koInput("email") }
        }
    }

    @Test
    fun `koButton dispatches to registered implementation`() {
        var called = false
        KopetalRegistry[ButtonKey] = { _, _ -> called = true }
        createHTML().div { koButton("Click") }
        assertTrue(called)
    }

    @Test
    fun `koInput dispatches to registered implementation`() {
        var called = false
        KopetalRegistry[InputKey] = { _, _, _ -> called = true }
        createHTML().div { koInput("email") }
        assertTrue(called)
    }

    @Test
    fun `koButton passes label and disabled correctly`() {
        var capturedLabel = ""
        var capturedDisabled = true
        KopetalRegistry[ButtonKey] = { label, disabled ->
            capturedLabel = label
            capturedDisabled = disabled
        }
        createHTML().div { koButton("Save", disabled = false) }
        assertEquals("Save", capturedLabel)
        assertFalse(capturedDisabled)
    }

    @Test
    fun `koButton defaults disabled to false`() {
        var capturedDisabled = true
        KopetalRegistry[ButtonKey] = { _, disabled -> capturedDisabled = disabled }
        createHTML().div { koButton("Go") }
        assertFalse(capturedDisabled)
    }

    @Test
    fun `koInput passes name, type, required correctly`() {
        var capturedName = ""
        var capturedType = ""
        var capturedRequired = false
        KopetalRegistry[InputKey] = { name, type, required ->
            capturedName = name
            capturedType = type
            capturedRequired = required
        }
        createHTML().div { koInput("email", type = "email", required = true) }
        assertEquals("email", capturedName)
        assertEquals("email", capturedType)
        assertTrue(capturedRequired)
    }

    @Test
    fun `koInput defaults type to text and required to false`() {
        var capturedType = ""
        var capturedRequired = true
        KopetalRegistry[InputKey] = { _, type, required ->
            capturedType = type
            capturedRequired = required
        }
        createHTML().div { koInput("username") }
        assertEquals("text", capturedType)
        assertFalse(capturedRequired)
    }
}
