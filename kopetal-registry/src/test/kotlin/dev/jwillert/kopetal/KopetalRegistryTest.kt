package dev.jwillert.kopetal

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KopetalRegistryTest {

    @Test
    fun `typed key stores and retrieves value`() {
        val key = RegistryKey<String>("test.key")
        KopetalRegistry[key] = "hello"
        assertEquals("hello", KopetalRegistry[key])
        KopetalRegistry.remove(key)
    }

    @Test
    fun `typed key returns null for unregistered key`() {
        val key = RegistryKey<String>("test.missing")
        assertNull(KopetalRegistry[key])
    }

    @Test
    fun `different typed keys are independent`() {
        val key1 = RegistryKey<String>("test.a")
        val key2 = RegistryKey<String>("test.b")
        KopetalRegistry[key1] = "alpha"
        KopetalRegistry[key2] = "beta"
        assertEquals("alpha", KopetalRegistry[key1])
        assertEquals("beta", KopetalRegistry[key2])
        KopetalRegistry.remove(key1)
        KopetalRegistry.remove(key2)
    }

    @Test
    fun `remove clears a registered key`() {
        val key = RegistryKey<String>("test.remove")
        KopetalRegistry[key] = "value"
        KopetalRegistry.remove(key)
        assertNull(KopetalRegistry[key])
    }
}
