package dev.jwillert.kopetal.components

import kotlinx.html.*

fun FlowContent.koCard(
    title: String? = null,
    compact: Boolean = false,
    bordered: Boolean = true,
    additionalClasses: String = "",
    header: (DIV.() -> Unit)? = null,
    footer: (DIV.() -> Unit)? = null,
    block: DIV.() -> Unit
) {
    div {
        val classes = listOfNotNull(
            "card",
            "bg-base-100",
            "shadow-xl".takeIf { !compact },
            "card-compact".takeIf { compact },
            "card-bordered".takeIf { bordered },
            additionalClasses.takeIf { it.isNotEmpty() }
        ).joinToString(" ")
        this.classes = setOf(classes)

        header?.let {
            div {
                this.classes = setOf("card-header")
                it()
            }
        }

        div {
            this.classes = setOf("card-body")
            title?.let {
                h2 {
                    this.classes = setOf("card-title")
                    +it
                }
            }
            block()
        }

        footer?.let {
            div {
                this.classes = setOf("card-footer")
                it()
            }
        }
    }
}
