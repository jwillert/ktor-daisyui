package dev.jwillert.daisyui.vrt

import dev.jwillert.daisyui.components.ButtonSize
import dev.jwillert.daisyui.components.ButtonVariant
import dev.jwillert.daisyui.components.TableColumn
import dev.jwillert.daisyui.components.koButton
import dev.jwillert.daisyui.components.koCard
import dev.jwillert.daisyui.components.koInput
import dev.jwillert.daisyui.components.koModal
import dev.jwillert.daisyui.components.koTable
import kotlinx.html.p

object Scenarios {
    val all: List<Scenario> = listOf(
        // button
        Scenario("button-primary") { koButton("Primary", ButtonVariant.PRIMARY) },
        Scenario("button-disabled") { koButton("Disabled", ButtonVariant.PRIMARY, disabled = true) },
        Scenario("button-ghost") { koButton("Ghost", ButtonVariant.GHOST) },
        Scenario("button-error") { koButton("Error", ButtonVariant.ERROR) },

        // input
        Scenario("input-text-placeholder") { koInput("name", placeholder = "Your name") },
        Scenario("input-disabled") { koInput("name", placeholder = "Disabled", disabled = true) },
        Scenario("input-email-required") {
            koInput("email", type = "email", placeholder = "you@example.com", required = true)
        },

        // card
        Scenario("card-title-body") {
            koCard(title = "Card title") { p { +"Card body content." } }
        },
        Scenario("card-header-footer") {
            koCard(
                header = { +"Header" },
                footer = { koButton("Action", ButtonVariant.PRIMARY, size = ButtonSize.SM) },
            ) { p { +"Body between header and footer." } }
        },

        // modal (opened via showModal(), capture the modal box)
        Scenario(
            name = "modal-open",
            captureSelector = ".modal-box",
            beforeShot = "document.querySelector('dialog.modal').showModal()",
        ) {
            koModal(id = "demo", title = "Modal title") { p { +"Modal body content." } }
        },

        // table
        Scenario("table-zebra") {
            koTable(
                items = listOf("Alice" to 30, "Bob" to 25, "Carol" to 41),
                columns = listOf(
                    TableColumn("Name") { (name, _) -> +name },
                    TableColumn("Age") { (_, age) -> +age.toString() },
                ),
            )
        },
    )
}
