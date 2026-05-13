package dev.jwillert.kopetal

import kotlinx.html.FlowContent

class RegistryKey<T : Any>(val id: String)

object KopetalRegistry {
    private val slots = mutableMapOf<RegistryKey<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: RegistryKey<T>): T? = slots[key] as? T

    operator fun <T : Any> set(key: RegistryKey<T>, value: T) {
        slots[key] = value
    }

    fun <T : Any> remove(key: RegistryKey<T>) {
        slots.remove(key)
    }
}

val ButtonKey = RegistryKey<FlowContent.(label: String, disabled: Boolean) -> Unit>("kopetal.button")
val InputKey = RegistryKey<FlowContent.(name: String, type: String, required: Boolean) -> Unit>("kopetal.input")

fun FlowContent.koButton(label: String, disabled: Boolean = false) =
    (KopetalRegistry[ButtonKey]
        ?: error("KopetalRegistry[ButtonKey] not installed — register a button implementation before rendering"))
        .invoke(this, label, disabled)

fun FlowContent.koInput(name: String, type: String = "text", required: Boolean = false) =
    (KopetalRegistry[InputKey]
        ?: error("KopetalRegistry[InputKey] not installed — register an input implementation before rendering"))
        .invoke(this, name, type, required)
