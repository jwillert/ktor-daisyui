package dev.jwillert.kopetal

import kotlinx.html.FlowContent

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
