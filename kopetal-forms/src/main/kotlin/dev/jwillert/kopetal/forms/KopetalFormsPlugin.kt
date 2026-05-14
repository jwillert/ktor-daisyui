package dev.jwillert.kopetal.forms

import dev.jwillert.kopetal.ButtonKey
import dev.jwillert.kopetal.InputKey
import dev.jwillert.kopetal.KopetalRegistry
import io.ktor.server.application.createApplicationPlugin
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.button
import kotlinx.html.input

fun installKopetalFormsDefaults() {
    KopetalRegistry[ButtonKey] = { label, disabled, block ->
        button(classes = if (disabled) "btn btn-primary btn-disabled" else "btn btn-primary") {
            type = ButtonType.button
            if (disabled) attributes["disabled"] = "disabled"
            +label
            block()
        }
    }
    KopetalRegistry[InputKey] = { name, type, required, block ->
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
        input(type = inputType, name = name, classes = "input input-bordered") {
            if (required) attributes["required"] = "required"
            block()
        }
    }
}

val KopetalForms = createApplicationPlugin("KopetalForms") {
    installKopetalFormsDefaults()
}
