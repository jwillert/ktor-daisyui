package dev.jwillert.kopetal

import kotlinx.html.BUTTON
import kotlinx.html.FlowContent
import kotlinx.html.INPUT

val ButtonKey = RegistryKey<FlowContent.(label: String, disabled: Boolean, block: BUTTON.() -> Unit) -> Unit>("kopetal.button")
val InputKey = RegistryKey<FlowContent.(name: String, type: String, required: Boolean, block: INPUT.() -> Unit) -> Unit>("kopetal.input")

fun FlowContent.koButton(
    label: String,
    disabled: Boolean = false,
    block: BUTTON.() -> Unit = {}
) = (KopetalRegistry[ButtonKey]
        ?: error("KopetalRegistry[ButtonKey] not installed — register a button implementation before rendering"))
        .invoke(this, label, disabled, block)

fun FlowContent.koInput(
    name: String,
    type: String = "text",
    required: Boolean = false,
    block: INPUT.() -> Unit = {}
) = (KopetalRegistry[InputKey]
        ?: error("KopetalRegistry[InputKey] not installed — register an input implementation before rendering"))
        .invoke(this, name, type, required, block)
