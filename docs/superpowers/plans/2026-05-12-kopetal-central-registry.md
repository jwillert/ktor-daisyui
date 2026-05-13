# Kopetal Central Registry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace per-module registries with a single `KopetalRegistry` (pure key-value store) in a new `kopetal-core` module so consumers register shared primitives once via typed keys.

**Architecture:** `KopetalRegistry` is a generic `RegistryKey<T>` map — no hardcoded properties. `ButtonKey`/`InputKey` and the `koButton`/`koInput` dispatch functions live in `kopetal-core`. `kopetal-forms` drops `KopetalFormsRegistry` and registers its defaults into the central keys. The Gradle plugin generator produces `KopetalRegistry[ButtonKey] = { ... }` using a new `slotKey` field in `registry.json`.

**Tech Stack:** Kotlin/JVM 21, kotlinx-html-jvm 0.11.0, Ktor 3.1.3, JUnit Jupiter 5.10.2, Gradle 8

---

## File Map

| Action | File |
|--------|------|
| Modify | `settings.gradle.kts` |
| Create | `kopetal-core/build.gradle.kts` |
| Create | `kopetal-core/src/main/kotlin/dev/jwillert/kopetal/KopetalRegistry.kt` |
| Create | `kopetal-core/src/test/kotlin/dev/jwillert/kopetal/KopetalRegistryTest.kt` |
| Delete | `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistry.kt` |
| Modify | `kopetal-forms/build.gradle.kts` |
| Modify | `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt` |
| Modify | `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/FormField.kt` |
| Modify | `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt` |
| Modify | `registry/registry.json` |
| Modify | `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/AddComponentTask.kt` |
| Modify | `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGenerator.kt` |
| Modify | `plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt` |

---

### Task 1: Add `kopetal-core` to the build

**Files:**
- Modify: `settings.gradle.kts`
- Create: `kopetal-core/build.gradle.kts`

- [ ] **Step 1: Add `kopetal-core` to `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "2.3.10"
    }
}

rootProject.name = "ktor-daisyui"

include("plugin")
include("registry")
include("kopetal-core")
include("kopetal-forms")
```

- [ ] **Step 2: Create `kopetal-core/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "dev.jwillert"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jwillert/ktor-daisyui")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.jwillert"
            artifactId = "kopetal-core"
            from(components["java"])
        }
    }
}
```

- [ ] **Step 3: Verify the build configuration is recognized**

Run: `./gradlew projects`

Expected: output includes `:kopetal-core` in the project list.

---

### Task 2: Implement `KopetalRegistry` with tests (TDD)

**Files:**
- Create: `kopetal-core/src/test/kotlin/dev/jwillert/kopetal/KopetalRegistryTest.kt`
- Create: `kopetal-core/src/main/kotlin/dev/jwillert/kopetal/KopetalRegistry.kt`

- [ ] **Step 1: Write failing tests**

Create `kopetal-core/src/test/kotlin/dev/jwillert/kopetal/KopetalRegistryTest.kt`:

```kotlin
package dev.jwillert.kopetal

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KopetalRegistryTest {

    @AfterEach
    fun restore() {
        KopetalRegistry.remove(ButtonKey)
        KopetalRegistry.remove(InputKey)
    }

    @Test
    fun `koButton throws when ButtonKey not registered`() {
        assertThrows(IllegalStateException::class.java) {
            createHTML().div { koButton("Test") }
        }
    }

    @Test
    fun `koInput throws when InputKey not registered`() {
        assertThrows(IllegalStateException::class.java) {
            createHTML().div { koInput("email") }
        }
    }

    @Test
    fun `koButton dispatches to registered implementation`() {
        var called = false
        KopetalRegistry[ButtonKey] = { _, _ -> called = true }
        createHTML().div { koButton("Click") }
        assertTrue(called)
    }

    @Test
    fun `koInput dispatches to registered implementation`() {
        var called = false
        KopetalRegistry[InputKey] = { _, _, _ -> called = true }
        createHTML().div { koInput("email") }
        assertTrue(called)
    }

    @Test
    fun `koButton passes label and disabled correctly`() {
        var capturedLabel = ""
        var capturedDisabled = true
        KopetalRegistry[ButtonKey] = { label, disabled ->
            capturedLabel = label
            capturedDisabled = disabled
        }
        createHTML().div { koButton("Save", disabled = false) }
        assertEquals("Save", capturedLabel)
        assertFalse(capturedDisabled)
    }

    @Test
    fun `koButton defaults disabled to false`() {
        var capturedDisabled = true
        KopetalRegistry[ButtonKey] = { _, disabled -> capturedDisabled = disabled }
        createHTML().div { koButton("Go") }
        assertFalse(capturedDisabled)
    }

    @Test
    fun `koInput passes name, type, required correctly`() {
        var capturedName = ""
        var capturedType = ""
        var capturedRequired = false
        KopetalRegistry[InputKey] = { name, type, required ->
            capturedName = name
            capturedType = type
            capturedRequired = required
        }
        createHTML().div { koInput("email", type = "email", required = true) }
        assertEquals("email", capturedName)
        assertEquals("email", capturedType)
        assertTrue(capturedRequired)
    }

    @Test
    fun `koInput defaults type to text and required to false`() {
        var capturedType = ""
        var capturedRequired = true
        KopetalRegistry[InputKey] = { _, type, required ->
            capturedType = type
            capturedRequired = required
        }
        createHTML().div { koInput("username") }
        assertEquals("text", capturedType)
        assertFalse(capturedRequired)
    }

    @Test
    fun `typed key stores and retrieves value`() {
        val key = RegistryKey<String>("test.key")
        KopetalRegistry[key] = "hello"
        assertEquals("hello", KopetalRegistry[key])
    }

    @Test
    fun `typed key returns null for unregistered key`() {
        val key = RegistryKey<String>("test.missing")
        assertNull(KopetalRegistry[key])
    }

    @Test
    fun `different typed keys are independent`() {
        val key1 = RegistryKey<String>("test.a")
        val key2 = RegistryKey<String>("test.b")
        KopetalRegistry[key1] = "alpha"
        KopetalRegistry[key2] = "beta"
        assertEquals("alpha", KopetalRegistry[key1])
        assertEquals("beta", KopetalRegistry[key2])
    }

    @Test
    fun `remove clears a registered key`() {
        val key = RegistryKey<String>("test.remove")
        KopetalRegistry[key] = "value"
        KopetalRegistry.remove(key)
        assertNull(KopetalRegistry[key])
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :kopetal-core:test`

Expected: FAIL — `KopetalRegistry`, `RegistryKey`, `ButtonKey`, `InputKey`, `koButton`, `koInput` do not exist yet.

- [ ] **Step 3: Implement `KopetalRegistry.kt`**

Create `kopetal-core/src/main/kotlin/dev/jwillert/kopetal/KopetalRegistry.kt`:

```kotlin
package dev.jwillert.kopetal

import kotlinx.html.FlowContent

class RegistryKey<T : Any>(val id: String)

object KopetalRegistry {
    private val slots = mutableMapOf<RegistryKey<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: RegistryKey<T>): T? = slots[key] as? T

    operator fun <T : Any> set(key: RegistryKey<T>, value: T) {
        slots[key] = value
    }

    fun <T : Any> remove(key: RegistryKey<T>) {
        slots.remove(key)
    }
}

val ButtonKey = RegistryKey<FlowContent.(label: String, disabled: Boolean) -> Unit>("kopetal.button")
val InputKey = RegistryKey<FlowContent.(name: String, type: String, required: Boolean) -> Unit>("kopetal.input")

fun FlowContent.koButton(label: String, disabled: Boolean = false) =
    (KopetalRegistry[ButtonKey]
        ?: error("KopetalRegistry[ButtonKey] not installed — call install(KopetalForms) in Application.module()"))
        .invoke(this, label, disabled)

fun FlowContent.koInput(name: String, type: String = "text", required: Boolean = false) =
    (KopetalRegistry[InputKey]
        ?: error("KopetalRegistry[InputKey] not installed — call install(KopetalForms) in Application.module()"))
        .invoke(this, name, type, required)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :kopetal-core:test`

Expected: 12 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add kopetal-core/ settings.gradle.kts
git commit -m "feat: add kopetal-core with central KopetalRegistry and typed keys"
```

---

### Task 3: Refactor `kopetal-forms` to use `KopetalRegistry`

**Files:**
- Delete: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistry.kt`
- Modify: `kopetal-forms/build.gradle.kts`
- Modify: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt`
- Modify: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/FormField.kt`
- Modify: `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt`

- [ ] **Step 1: Add `kopetal-core` dependency to `kopetal-forms/build.gradle.kts`**

Replace the full file:

```kotlin
plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "dev.jwillert"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kopetal-core"))
    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jwillert/ktor-daisyui")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.jwillert"
            artifactId = "kopetal-forms"
            from(components["java"])
        }
    }
}
```

- [ ] **Step 2: Delete `KopetalFormsRegistry.kt`**

Delete the file `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistry.kt`.

This file defined `FormsButtonSlot`, `FormsInputSlot`, and `KopetalFormsRegistry` — all replaced by `kopetal-core`.

- [ ] **Step 3: Update `KopetalFormsPlugin.kt`**

Replace the full file:

```kotlin
package dev.jwillert.kopetal.forms

import dev.jwillert.kopetal.ButtonKey
import dev.jwillert.kopetal.InputKey
import dev.jwillert.kopetal.KopetalRegistry
import io.ktor.server.application.createApplicationPlugin
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.button
import kotlinx.html.input

fun installKopetalFormsDefaults() {
    KopetalRegistry[ButtonKey] = { label, disabled ->
        button(classes = if (disabled) "btn btn-primary btn-disabled" else "btn btn-primary") {
            type = ButtonType.button
            if (disabled) attributes["disabled"] = "disabled"
            +label
        }
    }
    KopetalRegistry[InputKey] = { name, type, required ->
        val inputType = when (type) {
            "email" -> InputType.email
            "password" -> InputType.password
            "number" -> InputType.number
            "tel" -> InputType.tel
            "url" -> InputType.url
            "search" -> InputType.search
            "date" -> InputType.date
            "hidden" -> InputType.hidden
            "checkbox" -> InputType.checkBox
            "radio" -> InputType.radio
            else -> InputType.text
        }
        input(type = inputType, name = name, classes = "input input-bordered") {
            if (required) attributes["required"] = "required"
        }
    }
}

val KopetalForms = createApplicationPlugin("KopetalForms") {
    installKopetalFormsDefaults()
}
```

- [ ] **Step 4: Update `FormField.kt`**

Replace the full file:

```kotlin
package dev.jwillert.kopetal.forms

import dev.jwillert.kopetal.koInput
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.label
import kotlinx.html.span

fun FlowContent.formField(
    label: String,
    name: String,
    type: String = "text",
    required: Boolean = false,
) {
    div(classes = "form-control") {
        label(classes = "label") {
            span(classes = "label-text") { +label }
        }
        koInput(name, type, required)
    }
}
```

- [ ] **Step 5: Update `KopetalFormsRegistryTest.kt`**

Replace the full file:

```kotlin
package dev.jwillert.kopetal.forms

import dev.jwillert.kopetal.ButtonKey
import dev.jwillert.kopetal.InputKey
import dev.jwillert.kopetal.KopetalRegistry
import dev.jwillert.kopetal.koButton
import dev.jwillert.kopetal.koInput
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KopetalFormsRegistryTest {

    @AfterEach
    fun restore() {
        KopetalRegistry.remove(ButtonKey)
        KopetalRegistry.remove(InputKey)
    }

    @Test
    fun `button slot can be overridden`() {
        var called = false
        KopetalRegistry[ButtonKey] = { _, _ -> called = true }
        createHTML().div { koButton("Click me") }
        assertTrue(called)
    }

    @Test
    fun `input slot can be overridden independently of button`() {
        var inputCalled = false
        KopetalRegistry[InputKey] = { _, _, _ -> inputCalled = true }
        createHTML().div { koInput("email") }
        assertTrue(inputCalled)
    }

    @Test
    fun `default button renders with label and btn class`() {
        installKopetalFormsDefaults()

        val html = createHTML().div {
            koButton("Save")
        }

        assertTrue(html.contains("Save"), "Expected label in output: $html")
        assertTrue(html.contains("btn"), "Expected btn class in output: $html")
    }

    @Test
    fun `default input renders with name attribute`() {
        installKopetalFormsDefaults()

        val html = createHTML().div {
            koInput("username")
        }

        assertTrue(html.contains("username"), "Expected name in output: $html")
        assertTrue(html.contains("input"), "Expected input element in output: $html")
    }

    @Test
    fun `formField renders label text and delegates input to registry`() {
        installKopetalFormsDefaults()

        val html = createHTML().div {
            formField("Email Address", "email", type = "email", required = true)
        }

        assertTrue(html.contains("Email Address"), "Expected label in output: $html")
        assertTrue(html.contains("form-control"), "Expected form-control class in output: $html")
        assertTrue(html.contains("required"), "Expected required attribute in output: $html")
    }

    @Test
    fun `formField uses overridden input slot`() {
        var capturedName = ""
        var capturedRequired = false
        KopetalRegistry[InputKey] = { name, _, required ->
            capturedName = name
            capturedRequired = required
        }

        createHTML().div {
            formField("Email", "my-email", required = true)
        }

        assertTrue(capturedName == "my-email", "Expected name 'my-email', got '$capturedName'")
        assertTrue(capturedRequired, "Expected required=true")
    }
}
```

- [ ] **Step 6: Run `kopetal-forms` tests**

Run: `./gradlew :kopetal-forms:test`

Expected: 6 tests, 0 failures.

- [ ] **Step 7: Commit**

```bash
git add kopetal-forms/
git commit -m "refactor: kopetal-forms uses KopetalRegistry typed keys from kopetal-core"
```

---

### Task 4: Update generator, `AddComponentTask`, and `registry.json`

The generator must produce `KopetalRegistry[ButtonKey] = { ... }` instead of `KopetalRegistry.button = { ... }`. This requires a new `slotKey` field in `registry.json` and `ComponentEntry`, plus updated generator logic.

**Files:**
- Modify: `registry/registry.json`
- Modify: `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/AddComponentTask.kt`
- Modify: `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGenerator.kt`
- Modify: `plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt`

- [ ] **Step 1: Write failing tests for the updated generator**

Replace `plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt`:

```kotlin
package dev.jwillert.ktor.daisyui.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KopetalComponentsGeneratorTest {

    @Test
    fun `generateConfigureStatement returns null when slotRegistryClass is null`() {
        val entry = ComponentEntry(
            name = "modal",
            description = "modal",
            file = "src/modal/Modal.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = null,
            slotKey = null,
            slotParams = null
        )
        assertEquals(null, generateConfigureStatement(entry))
    }

    @Test
    fun `generateConfigureStatement returns null when slotKey is null even if slotRegistryClass is set`() {
        val entry = ComponentEntry(
            name = "button",
            description = "button",
            file = "src/button/Button.kt",
            kotlinPackage = "pkg",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = null,
            slotParams = "label: String, disabled: Boolean"
        )
        assertEquals(null, generateConfigureStatement(entry))
    }

    @Test
    fun `generateConfigureStatement returns null when slotParams is null even if slotKey is set`() {
        val entry = ComponentEntry(
            name = "button",
            description = "button",
            file = "src/button/Button.kt",
            kotlinPackage = "pkg",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = "dev.jwillert.kopetal.ButtonKey",
            slotParams = null
        )
        assertEquals(null, generateConfigureStatement(entry))
    }

    @Test
    fun `generateConfigureStatement generates correct adapter for button`() {
        val entry = ComponentEntry(
            name = "button",
            description = "button",
            file = "src/button/Button.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = "dev.jwillert.kopetal.ButtonKey",
            slotParams = "label: String, disabled: Boolean"
        )
        assertEquals(
            "KopetalRegistry[ButtonKey] = { label, disabled -> koButton(label = label, disabled = disabled) }",
            generateConfigureStatement(entry)
        )
    }

    @Test
    fun `generateConfigureStatement generates correct adapter for input`() {
        val entry = ComponentEntry(
            name = "input",
            description = "input",
            file = "src/input/Input.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
            slotKey = "dev.jwillert.kopetal.InputKey",
            slotParams = "name: String, type: String, required: Boolean"
        )
        assertEquals(
            "KopetalRegistry[InputKey] = { name, type, required -> koInput(name = name, type = type, required = required) }",
            generateConfigureStatement(entry)
        )
    }

    @Test
    fun `generateKopetalComponentsContent returns empty string when no slot entries`() {
        val entries = listOf(
            ComponentEntry("modal", "modal", "src/modal/Modal.kt", "pkg", null, null, null),
            ComponentEntry("card", "card", "src/card/Card.kt", "pkg", null, null, null),
        )
        assertEquals(
            "",
            generateKopetalComponentsContent(
                installedNames = setOf("modal", "card"),
                allRegistryEntries = entries,
                packageName = "com.example.components"
            )
        )
    }

    @Test
    fun `generateKopetalComponentsContent generates correct file for button and input`() {
        val entries = listOf(
            ComponentEntry(
                "button", "button", "src/button/Button.kt", "pkg",
                "dev.jwillert.kopetal.KopetalRegistry",
                "dev.jwillert.kopetal.ButtonKey",
                "label: String, disabled: Boolean"
            ),
            ComponentEntry(
                "input", "input", "src/input/Input.kt", "pkg",
                "dev.jwillert.kopetal.KopetalRegistry",
                "dev.jwillert.kopetal.InputKey",
                "name: String, type: String, required: Boolean"
            ),
            ComponentEntry("modal", "modal", "src/modal/Modal.kt", "pkg", null, null, null),
        )
        val result = generateKopetalComponentsContent(
            installedNames = setOf("button", "input"),
            allRegistryEntries = entries,
            packageName = "com.example.components"
        )

        val expected = "package com.example.components\n" +
            "\n" +
            "import dev.jwillert.kopetal.ButtonKey\n" +
            "import dev.jwillert.kopetal.InputKey\n" +
            "import dev.jwillert.kopetal.KopetalRegistry\n" +
            "\n" +
            "// Auto-generated by addComponent — do not edit manually\n" +
            "object KopetalComponents {\n" +
            "    fun configure() {\n" +
            "        KopetalRegistry[ButtonKey] = { label, disabled -> koButton(label = label, disabled = disabled) }\n" +
            "        KopetalRegistry[InputKey] = { name, type, required -> koInput(name = name, type = type, required = required) }\n" +
            "    }\n" +
            "}"
        assertEquals(expected, result)
    }

    @Test
    fun `generateKopetalComponentsContent only includes installed components`() {
        val entries = listOf(
            ComponentEntry(
                "button", "button", "src/button/Button.kt", "pkg",
                "dev.jwillert.kopetal.KopetalRegistry",
                "dev.jwillert.kopetal.ButtonKey",
                "label: String, disabled: Boolean"
            ),
            ComponentEntry(
                "input", "input", "src/input/Input.kt", "pkg",
                "dev.jwillert.kopetal.KopetalRegistry",
                "dev.jwillert.kopetal.InputKey",
                "name: String, type: String, required: Boolean"
            ),
        )
        val result = generateKopetalComponentsContent(
            installedNames = setOf("button"),
            allRegistryEntries = entries,
            packageName = "com.example.components"
        )

        assertTrue(result.contains("KopetalRegistry[ButtonKey]"), "Expected button slot")
        assertTrue(!result.contains("KopetalRegistry[InputKey]"), "Expected no input slot: $result")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :plugin:test`

Expected: FAIL — `ComponentEntry` has no `slotKey` field yet, and `generateConfigureStatement` produces the old format.

- [ ] **Step 3: Add `slotKey` field to `ComponentEntry` in `AddComponentTask.kt`**

In `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/AddComponentTask.kt`, update `ComponentEntry` and `parseRegistry`:

```kotlin
data class ComponentEntry(
    val name: String,
    val description: String,
    val file: String,
    val kotlinPackage: String,
    val slotRegistryClass: String? = null,
    val slotKey: String? = null,
    val slotParams: String? = null,
)
```

In `parseRegistry`, add `slotKey` extraction alongside `slotRegistryClass`:

```kotlin
private fun parseRegistry(json: String): List<ComponentEntry> {
    val entries = mutableListOf<ComponentEntry>()
    val objectPattern = Regex("""\{[^}]+\}""")
    val fieldPattern = Regex(""""(\w+)"\s*:\s*"([^"]+)"""")

    objectPattern.findAll(json).forEach { match ->
        val fields = fieldPattern.findAll(match.value).associate { it.groupValues[1] to it.groupValues[2] }
        val name = fields["name"] ?: return@forEach
        val description = fields["description"] ?: ""
        val file = fields["file"] ?: return@forEach
        val kotlinPackage = fields["kotlinPackage"] ?: "dev.jwillert.ktor.daisyui.components"
        val slotRegistryClass = fields["slotRegistryClass"]
        val slotKey = fields["slotKey"]
        val slotParams = fields["slotParams"]
        entries.add(ComponentEntry(name, description, file, kotlinPackage, slotRegistryClass, slotKey, slotParams))
    }

    return entries
}
```

- [ ] **Step 4: Update `KopetalComponentsGenerator.kt`**

Replace the full file:

```kotlin
package dev.jwillert.ktor.daisyui.tasks

fun generateConfigureStatement(entry: ComponentEntry): String? {
    val registryFqn = entry.slotRegistryClass ?: return null
    val keyFqn = entry.slotKey ?: return null
    val params = entry.slotParams ?: return null
    val registrySimpleName = registryFqn.substringAfterLast(".")
    val keySimpleName = keyFqn.substringAfterLast(".")
    val paramNames = params.split(",").map { it.trim().substringBefore(":").trim() }
    val args = paramNames.joinToString(", ") { "$it = $it" }
    val fnName = "ko${entry.name.replaceFirstChar { it.uppercaseChar() }}"
    return "$registrySimpleName[$keySimpleName] = { ${paramNames.joinToString(", ")} -> $fnName($args) }"
}

fun generateKopetalComponentsContent(
    installedNames: Set<String>,
    allRegistryEntries: List<ComponentEntry>,
    packageName: String,
): String {
    val slotEntries = allRegistryEntries.filter { entry ->
        entry.name in installedNames &&
            entry.slotRegistryClass != null &&
            entry.slotKey != null &&
            entry.slotParams != null
    }
    if (slotEntries.isEmpty()) return ""

    val imports = (
        slotEntries.mapNotNull { it.slotRegistryClass } +
            slotEntries.mapNotNull { it.slotKey }
        ).distinct().sorted()

    val statements = slotEntries.mapNotNull { generateConfigureStatement(it) }

    return buildString {
        appendLine("package $packageName")
        appendLine()
        imports.forEach { appendLine("import $it") }
        appendLine()
        appendLine("// Auto-generated by addComponent — do not edit manually")
        appendLine("object KopetalComponents {")
        appendLine("    fun configure() {")
        statements.forEach { appendLine("        $it") }
        appendLine("    }")
        append("}")
    }
}
```

- [ ] **Step 5: Update `registry/registry.json`**

Add `slotKey` to `button` and `input`, remove `slotRegistryClass` from `button` and `input` (no longer needed as a standalone field — but keep it so the import is generated):

```json
[
  {
    "name": "button",
    "description": "DaisyUI button variants with HTMX support",
    "file": "src/button/Button.kt",
    "kotlinPackage": "dev.jwillert.daisyui.components",
    "slotRegistryClass": "dev.jwillert.kopetal.KopetalRegistry",
    "slotKey": "dev.jwillert.kopetal.ButtonKey",
    "slotParams": "label: String, disabled: Boolean"
  },
  {
    "name": "modal",
    "description": "DaisyUI modal dialog with open/close helpers",
    "file": "src/modal/Modal.kt",
    "kotlinPackage": "dev.jwillert.ktor.daisyui.components"
  },
  {
    "name": "card",
    "description": "DaisyUI card with header, body and footer slots",
    "file": "src/card/Card.kt",
    "kotlinPackage": "dev.jwillert.ktor.daisyui.components"
  },
  {
    "name": "table",
    "description": "DaisyUI data table with HTMX pagination support",
    "file": "src/table/Table.kt",
    "kotlinPackage": "dev.jwillert.ktor.daisyui.components"
  },
  {
    "name": "input",
    "description": "DaisyUI text input with type and validation support",
    "file": "src/input/Input.kt",
    "kotlinPackage": "dev.jwillert.daisyui.components",
    "slotRegistryClass": "dev.jwillert.kopetal.KopetalRegistry",
    "slotKey": "dev.jwillert.kopetal.InputKey",
    "slotParams": "name: String, type: String, required: Boolean"
  }
]
```

- [ ] **Step 6: Run plugin tests**

Run: `./gradlew :plugin:test`

Expected: 7 tests, 0 failures.

- [ ] **Step 7: Run full test suite**

Run: `./gradlew test`

Expected: all tests pass across `:kopetal-core`, `:kopetal-forms`, `:plugin`.

- [ ] **Step 8: Commit**

```bash
git add registry/registry.json plugin/src/
git commit -m "refactor: generator uses RegistryKey syntax, add slotKey to registry.json"
```
