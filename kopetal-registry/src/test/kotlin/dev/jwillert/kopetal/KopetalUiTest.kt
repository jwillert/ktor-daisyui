package dev.jwillert.kopetal

import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.input
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
        KopetalRegistry[ButtonKey] = { _, _, _ -> called = true }
        createHTML().div { koButton("Click") }
        assertTrue(called)
    }

    @Test
    fun `koInput dispatches to registered implementation`() {
        var called = false
        KopetalRegistry[InputKey] = { _, _, _, _ -> called = true }
        createHTML().div { koInput("email") }
        assertTrue(called)
    }

    @Test
    fun `koButton passes label and disabled correctly`() {
        var capturedLabel = ""
        var capturedDisabled = true
        KopetalRegistry[ButtonKey] = { label, disabled, _ ->
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
        KopetalRegistry[ButtonKey] = { _, disabled, _ -> capturedDisabled = disabled }
        createHTML().div { koButton("Go") }
        assertFalse(capturedDisabled)
    }

    @Test
    fun `koInput passes name, type, required correctly`() {
        var capturedName = ""
        var capturedType = ""
        var capturedRequired = false
        KopetalRegistry[InputKey] = { name, type, required, _ ->
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
        KopetalRegistry[InputKey] = { _, type, required, _ ->
            capturedType = type
            capturedRequired = required
        }
        createHTML().div { koInput("username") }
        assertEquals("text", capturedType)
        assertFalse(capturedRequired)
    }

    @Test
    fun `koButton passes block to registered implementation`() {
        var blockInvoked = false
        KopetalRegistry[ButtonKey] = { _, _, block ->
            blockInvoked = true
            createHTML().button { block() }
        }
        createHTML().div { koButton("Click") { } }
        assertTrue(blockInvoked)
    }

    @Test
    fun `koButton block receives correct HTML element context`() {
        KopetalRegistry[ButtonKey] = { _, _, block ->
            button { block() }
        }
        val html = createHTML().div {
            koButton("X") { attributes["data-test"] = "yes" }
        }
        assertTrue(html.contains("data-test=\"yes\""), "Expected data-test attribute: $html")
    }

    @Test
    fun `koInput passes block to registered implementation`() {
        var blockInvoked = false
        KopetalRegistry[InputKey] = { _, _, _, block ->
            blockInvoked = true
            createHTML().input { block() }
        }
        createHTML().div { koInput("name") { } }
        assertTrue(blockInvoked)
    }
}
