package dev.jwillert.ktor.daisyui.common

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

fun ExecOperations.execPlatformAware(project: Project, args: List<String>, action: Action<ExecSpec>? = Action {}): ExecResult {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    return exec {
        workingDir(project.projectDir)
        if (isWindows) {
            commandLine("cmd", "/c")
            args(args)
        } else {
            commandLine(args)
        }
        action?.execute(this)
    }
}
