package dev.jwillert.kopetal.forms

import dev.jwillert.kopetal.forms.components.KopetalComponents
import io.ktor.server.application.createApplicationPlugin

val KopetalForms = createApplicationPlugin("KopetalForms") {
    KopetalComponents.configure()
}
