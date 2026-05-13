package sample

import dev.jwillert.kopetal.ButtonKey
import dev.jwillert.kopetal.InputKey
import dev.jwillert.kopetal.KopetalRegistry
import dev.jwillert.kopetal.forms.KopetalForms
import dev.jwillert.kopetal.forms.formField
import dev.jwillert.kopetal.koButton
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    // Step 1: install Maven defaults (DaisyUI button + input)
    install(KopetalForms)

    // Step 2: override individual slots — only what you need to change
    KopetalRegistry[ButtonKey] = { label, disabled ->
        button(classes = if (disabled) "btn btn-ghost btn-disabled" else "btn btn-secondary") {
            type = ButtonType.button
            if (disabled) attributes["disabled"] = "disabled"
            +label
        }
    }

    // InputKey stays as the KopetalForms default (DaisyUI input input-bordered)

    routing {
        get("/") {
            call.respondHtml {
                head {
                    title("Kopetal Sample")
                    link(
                        rel = "stylesheet",
                        href = "https://cdn.jsdelivr.net/npm/daisyui@4/dist/full.min.css"
                    )
                    script(src = "https://cdn.tailwindcss.com") {}
                }
                body(classes = "p-8 max-w-lg") {
                    h1(classes = "text-2xl font-bold mb-6") { +"Kopetal Forms Sample" }

                    form(action = "/submit", method = FormMethod.post, classes = "flex flex-col gap-4") {
                        // formField delegates its input rendering to KopetalRegistry[InputKey]
                        formField("Email", "email", type = "email", required = true)
                        formField("Password", "password", type = "password", required = true)
                        formField("Username", "username")

                        div(classes = "flex gap-2 mt-2") {
                            // koButton dispatches to KopetalRegistry[ButtonKey]
                            koButton("Submit")
                            koButton("Cancel", disabled = true)
                        }
                    }
                }
            }
        }
    }
}
