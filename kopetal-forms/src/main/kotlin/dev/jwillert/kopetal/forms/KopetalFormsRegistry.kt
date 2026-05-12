package dev.jwillert.kopetal.forms

import kotlinx.html.FlowContent

typealias FormsButtonSlot = FlowContent.(label: String, disabled: Boolean) -> Unit
typealias FormsInputSlot = FlowContent.(name: String, type: String, required: Boolean) -> Unit

object KopetalFormsRegistry {
    var button: FormsButtonSlot = { _, _ -> error("KopetalForms plugin not installed — call install(KopetalForms) in Application.module()") }
    var input: FormsInputSlot = { _, _, _ -> error("KopetalForms plugin not installed — call install(KopetalForms) in Application.module()") }
}
