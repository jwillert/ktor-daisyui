package dev.jwillert.ko.components

import kotlinx.html.FlowContent
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.input

fun FlowContent.koInput(
    name: String,
    type: String = "text",
    placeholder: String = "",
    required: Boolean = false,
    disabled: Boolean = false,
    additionalClasses: String = "",
    block: INPUT.() -> Unit = {},
) {
    val inputType = when (type) {
        "email" -> InputType.email
        "password" -> InputType.password
        "number" -> InputType.number
        "tel" -> InputType.tel
        "url" -> InputType.url
        "search" -> InputType.search
        "date" -> InputType.date
        "hidden" -> InputType.hidden
        "checkbox" -> InputType.checkBox
        "radio" -> InputType.radio
        else -> InputType.text
    }
    val cssClasses = listOfNotNull(
        "input input-bordered",
        additionalClasses.takeIf { it.isNotEmpty() },
    ).joinToString(" ")

    input(type = inputType, name = name, classes = cssClasses) {
        if (placeholder.isNotEmpty()) this.placeholder = placeholder
        if (required) attributes["required"] = "required"
        if (disabled) attributes["disabled"] = "disabled"
        block()
    }
}
