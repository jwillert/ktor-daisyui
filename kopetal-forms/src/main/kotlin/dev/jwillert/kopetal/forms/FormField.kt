package dev.jwillert.kopetal.forms

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.label
import kotlinx.html.span

fun FlowContent.formField(
    label: String,
    name: String,
    type: String = "text",
    required: Boolean = false,
    disabled: Boolean = false,
) {
    div(classes = "form-control") {
        label(classes = "label") {
            span(classes = "label-text") { +label }
        }
        KopetalFormsRegistry.input(this, name, type, required)
    }
}
