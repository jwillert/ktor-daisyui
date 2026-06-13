package dev.jwillert.daisyui.vrt

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData

class ComponentVisualTest : FunSpec({
    val harness = VrtHarness()

    beforeSpec { harness.start() }
    afterSpec { harness.close() }

    withData(
        nameFn = { it.name },
        Scenarios.all,
    ) { scenario ->
        harness.check(scenario)?.let { fail(it) }
    }
})
