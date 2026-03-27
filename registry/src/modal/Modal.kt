package dev.jwillert.daisyui.components

import kotlinx.html.*

fun FlowContent.koModal(
    id: String,
    title: String,
    closeLabel: String = "Close",
    block: DIV.() -> Unit
) {
    dialog {
        this.id = id
        classes = setOf("modal")
        div {
            classes = setOf("modal-box")
            h3 {
                classes = setOf("font-bold", "text-lg")
                +title
            }
            div {
                classes = setOf("py-4")
                block()
            }
            div {
                classes = setOf("modal-action")
                form {
                    method = FormMethod.dialog
                    button {
                        classes = setOf("btn")
                        +closeLabel
                    }
                }
            }
        }
        form {
            method = FormMethod.dialog
            classes = setOf("modal-backdrop")
            button { +"close" }
        }
    }
}

fun FlowContent.koModalTrigger(modalId: String, label: String, variant: ButtonVariant = ButtonVariant.PRIMARY) {
    button {
        classes = setOf(variant.css)
        attributes["onclick"] = "document.getElementById('$modalId').showModal()"
        +label
    }
}
