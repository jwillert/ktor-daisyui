package dev.jwillert.daisyui.vrt

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * Runs `playwright run-server` inside the pinned Playwright image so rendering is
 * deterministic regardless of the host. The image tag must match the Playwright
 * Java client version (1.49.0).
 */
class PlaywrightServerContainer : GenericContainer<PlaywrightServerContainer>(
    DockerImageName.parse("mcr.microsoft.com/playwright:v1.49.0-jammy"),
) {
    init {
        withExposedPorts(SERVER_PORT)
        withCommand(
            "/bin/sh", "-c",
            "npx playwright run-server --port $SERVER_PORT --host 0.0.0.0",
        )
        waitingFor(Wait.forListeningPort())
    }

    fun wsEndpoint(): String = "ws://$host:${getMappedPort(SERVER_PORT)}/"

    companion object {
        private const val SERVER_PORT = 3000
    }
}
