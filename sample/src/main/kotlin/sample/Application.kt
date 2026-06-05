package sample

import dev.jwillert.ko.components.ButtonVariant
import dev.jwillert.ko.components.formField
import dev.jwillert.ko.components.koButton
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
                    h1(classes = "text-2xl font-bold mb-6") { +"Kopetal Sample" }

                    form(action = "/submit", method = FormMethod.post, classes = "flex flex-col gap-4") {
                        formField("Email", "email", type = "email", required = true)
                        formField("Password", "password", type = "password", required = true)
                        formField("Username", "username")

                        div(classes = "flex gap-2 mt-2") {
                            koButton("Submit")
                            koButton("Cancel", variant = ButtonVariant.GHOST, disabled = true)
                        }
                    }
                }
            }
        }
    }
}
