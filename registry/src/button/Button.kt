package dev.jwillert.daisyui.components

import kotlinx.html.*

enum class ButtonVariant(val css: String) {
    PRIMARY("btn btn-primary"),
    SECONDARY("btn btn-secondary"),
    ACCENT("btn btn-accent"),
    GHOST("btn btn-ghost"),
    LINK("btn btn-link"),
    OUTLINE("btn btn-outline"),
    ERROR("btn btn-error"),
    SUCCESS("btn btn-success"),
    WARNING("btn btn-warning"),
}

enum class ButtonSize(val css: String) {
    XS("btn-xs"),
    SM("btn-sm"),
    MD(""),
    LG("btn-lg"),
    XL("btn-xl"),
}

fun FlowContent.koButton(
    label: String,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ButtonSize = ButtonSize.MD,
    disabled: Boolean = false,
    type: ButtonType = ButtonType.button,
    additionalClasses: String = "",
    block: BUTTON.() -> Unit = {}
) {
    button {
        this.type = type
        val classes = listOfNotNull(
            variant.css,
            size.css.takeIf { it.isNotEmpty() },
            "btn-disabled".takeIf { disabled },
            additionalClasses.takeIf { it.isNotEmpty() }
        ).joinToString(" ")
        this.classes = setOf(classes)
        if (disabled) attributes["disabled"] = "disabled"
        +label
        block()
    }
}
