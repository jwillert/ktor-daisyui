# kopetal-forms Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `kopetal-forms` Maven artifact with `formField` that uses a per-module runtime registry (`KopetalFormsRegistry`), plus a new `koInput` source-registry component — enabling consumers to customize base components while pulling higher-level ones via Gradle dependency.

**Architecture:** Each Maven module owns its slot types as plain Kotlin function typealias. `KopetalFormsRegistry` is a global object configured once at startup via a Ktor plugin. `addComponent` is extended to generate `KopetalComponents.kt` which wires the consumer's installed components into the registry. No shared `kopetal-api` module — each library module is self-contained.

**Tech Stack:** Kotlin 2.3.10, kotlinx.html-jvm 0.11.0, Ktor 3.1.3, Gradle Kotlin DSL, JUnit 5

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `settings.gradle.kts` | Modify | Add `include("kopetal-forms")` |
| `kopetal-forms/build.gradle.kts` | Create | Module config, dependencies, publishing |
| `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistry.kt` | Create | Slot typealias definitions + registry object |
| `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt` | Create | Ktor plugin + default implementations |
| `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/FormField.kt` | Create | `formField` composable |
| `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt` | Create | Registry + plugin + formField tests |
| `registry/src/input/Input.kt` | Create | `koInput` source component |
| `registry/registry.json` | Modify | Add input entry + slot metadata for button/input |
| `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGenerator.kt` | Create | Pure functions: generate `KopetalComponents.kt` content |
| `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/AddComponentTask.kt` | Modify | Extend ComponentEntry, call generator, write file |
| `plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt` | Create | Unit tests for generator functions |
| `plugin/build.gradle.kts` | Modify | Add JUnit 5 test dependency |

---

### Task 1: Add kopetal-forms Gradle module

**Files:**
- Modify: `settings.gradle.kts`
- Create: `kopetal-forms/build.gradle.kts`

- [ ] **Step 1: Add module to settings**

Open `settings.gradle.kts`. Current content ends at `include("registry")`. Add one line:

```kotlin
// settings.gradle.kts
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
include("kopetal-forms")
```

- [ ] **Step 2: Create kopetal-forms/build.gradle.kts**

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

- [ ] **Step 3: Verify Gradle sync**

```bash
cd /path/to/kopetal
./gradlew :kopetal-forms:build
```

Expected: `BUILD SUCCESSFUL` (empty module compiles fine)

---

### Task 2: Create KopetalFormsRegistry

**Files:**
- Create: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistry.kt`
- Create: `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt`

- [ ] **Step 1: Write the failing test**

Create `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt`:

```kotlin
package dev.jwillert.kopetal.forms

import kotlinx.html.FlowContent
import kotlinx.html.stream.appendHTML
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KopetalFormsRegistryTest {

    private val originalButton = KopetalFormsRegistry.button
    private val originalInput = KopetalFormsRegistry.input

    @AfterEach
    fun restore() {
        KopetalFormsRegistry.button = originalButton
        KopetalFormsRegistry.input = originalInput
    }

    @Test
    fun `button slot can be overridden`() {
        KopetalFormsRegistry.button = { _, _ ->
            // custom button rendered as span for test identification
        }
        var called = false
        KopetalFormsRegistry.button = { label, disabled ->
            called = true
        }
        val html = buildString {
            appendHTML().div {
                KopetalFormsRegistry.button("Click me", false)
            }
        }
        assertTrue(called)
    }

    @Test
    fun `input slot can be overridden independently of button`() {
        var inputCalled = false
        KopetalFormsRegistry.input = { name, type, required ->
            inputCalled = true
        }

        val html = buildString {
            appendHTML().div {
                KopetalFormsRegistry.input("email", "email", true)
            }
        }

        assertTrue(inputCalled)
    }
}
```

- [ ] **Step 2: Run test — verify it fails**

```bash
./gradlew :kopetal-forms:test
```

Expected: FAIL — `KopetalFormsRegistry` does not exist yet

- [ ] **Step 3: Create KopetalFormsRegistry.kt**

Create `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistry.kt`:

```kotlin
package dev.jwillert.kopetal.forms

import kotlinx.html.FlowContent

typealias FormsButtonSlot = FlowContent.(label: String, disabled: Boolean) -> Unit
typealias FormsInputSlot = FlowContent.(name: String, type: String, required: Boolean) -> Unit

object KopetalFormsRegistry {
    var button: FormsButtonSlot = { _, _ -> error("KopetalForms plugin not installed — call install(KopetalForms) in Application.module()") }
    var input: FormsInputSlot = { _, _, _ -> error("KopetalForms plugin not installed — call install(KopetalForms) in Application.module()") }
}
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
./gradlew :kopetal-forms:test
```

Expected: `BUILD SUCCESSFUL`, 2 tests pass

- [ ] **Step 5: Commit**

```bash
git add kopetal-forms/ settings.gradle.kts
git commit -m "feat: add kopetal-forms module with KopetalFormsRegistry"
```

---

### Task 3: Add KopetalForms Ktor plugin with default implementations

**Files:**
- Create: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt`
- Modify: `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt`

- [ ] **Step 1: Add test for default button rendering**

Append to `KopetalFormsRegistryTest.kt` (inside the class):

```kotlin
    @Test
    fun `default button renders with label and btn class`() {
        installKopetalFormsDefaults()

        val html = buildString {
            appendHTML().div {
                KopetalFormsRegistry.button("Save", false)
            }
        }

        assertTrue(html.contains("Save"), "Expected label in output: $html")
        assertTrue(html.contains("btn"), "Expected btn class in output: $html")
    }

    @Test
    fun `default input renders with name attribute`() {
        installKopetalFormsDefaults()

        val html = buildString {
            appendHTML().div {
                KopetalFormsRegistry.input("username", "text", false)
            }
        }

        assertTrue(html.contains("username"), "Expected name in output: $html")
        assertTrue(html.contains("input"), "Expected input element in output: $html")
    }
```

Also add import at the top:
```kotlin
import dev.jwillert.kopetal.forms.installKopetalFormsDefaults
```

- [ ] **Step 2: Run test — verify it fails**

```bash
./gradlew :kopetal-forms:test --tests "*.KopetalFormsRegistryTest.default*"
```

Expected: FAIL — `installKopetalFormsDefaults` does not exist yet

- [ ] **Step 3: Create KopetalFormsPlugin.kt**

Create `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt`:

```kotlin
package dev.jwillert.kopetal.forms

import io.ktor.server.application.createApplicationPlugin
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.button
import kotlinx.html.input

fun installKopetalFormsDefaults() {
    KopetalFormsRegistry.button = { label, disabled ->
        button(classes = if (disabled) "btn btn-primary btn-disabled" else "btn btn-primary") {
            type = ButtonType.button
            if (disabled) attributes["disabled"] = "disabled"
            +label
        }
    }
    KopetalFormsRegistry.input = { name, type, required ->
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

- [ ] **Step 4: Run tests — verify they pass**

```bash
./gradlew :kopetal-forms:test
```

Expected: `BUILD SUCCESSFUL`, all tests pass

- [ ] **Step 5: Commit**

```bash
git add kopetal-forms/src/
git commit -m "feat: add KopetalForms Ktor plugin with default button and input renderers"
```

---

### Task 4: Add formField component

**Files:**
- Create: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/FormField.kt`
- Modify: `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt`

- [ ] **Step 1: Write the failing test**

Append to `KopetalFormsRegistryTest.kt` (inside the class):

```kotlin
    @Test
    fun `formField renders label text and delegates input to registry`() {
        installKopetalFormsDefaults()

        val html = buildString {
            appendHTML().div {
                formField("Email Address", "email", type = "email", required = true)
            }
        }

        assertTrue(html.contains("Email Address"), "Expected label in output: $html")
        assertTrue(html.contains("form-control"), "Expected form-control class in output: $html")
        assertTrue(html.contains("required"), "Expected required attribute in output: $html")
    }

    @Test
    fun `formField uses overridden input slot`() {
        var capturedName = ""
        var capturedRequired = false
        KopetalFormsRegistry.input = { name, _, required ->
            capturedName = name
            capturedRequired = required
        }

        buildString {
            appendHTML().div {
                formField("Email", "my-email", required = true)
            }
        }

        assertTrue(capturedName == "my-email", "Expected name 'my-email', got '$capturedName'")
        assertTrue(capturedRequired, "Expected required=true")
    }
```

Add import at the top:
```kotlin
import dev.jwillert.kopetal.forms.formField
```

- [ ] **Step 2: Run test — verify it fails**

```bash
./gradlew :kopetal-forms:test --tests "*.KopetalFormsRegistryTest.formField*"
```

Expected: FAIL — `formField` does not exist yet

- [ ] **Step 3: Create FormField.kt**

Create `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/FormField.kt`:

```kotlin
package dev.jwillert.kopetal.forms

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.label
import kotlinx.html.span

fun FlowContent.formField(
    label: String,
    name: String,
    type: String = "text",
    required: Boolean = false,
    disabled: Boolean = false,
) {
    div(classes = "form-control") {
        label(classes = "label") {
            span(classes = "label-text") { +label }
        }
        KopetalFormsRegistry.input(name, type, required)
    }
}
```

- [ ] **Step 4: Run all tests — verify they pass**

```bash
./gradlew :kopetal-forms:test
```

Expected: `BUILD SUCCESSFUL`, all tests pass

- [ ] **Step 5: Commit**

```bash
git add kopetal-forms/src/
git commit -m "feat: add formField component"
```

---

### Task 5: Add koInput to source registry

**Files:**
- Create: `registry/src/input/Input.kt`
- Modify: `registry/registry.json`

- [ ] **Step 1: Create Input.kt**

Create `registry/src/input/Input.kt`:

```kotlin
package dev.jwillert.daisyui.components

import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.input

fun FlowContent.koInput(
    name: String,
    type: String = "text",
    placeholder: String = "",
    required: Boolean = false,
    disabled: Boolean = false,
    additionalClasses: String = "",
) {
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
    val cssClasses = listOfNotNull(
        "input input-bordered",
        additionalClasses.takeIf { it.isNotEmpty() }
    ).joinToString(" ")

    input(type = inputType, name = name, classes = cssClasses) {
        if (placeholder.isNotEmpty()) this.placeholder = placeholder
        if (required) attributes["required"] = "required"
        if (disabled) attributes["disabled"] = "disabled"
    }
}
```

- [ ] **Step 2: Add input to registry.json**

Open `registry/registry.json`. Current content:
```json
[
  {
    "name": "button",
    "description": "DaisyUI button variants with HTMX support",
    "file": "src/button/Button.kt",
    "kotlinPackage": "dev.jwillert.daisyui.components"
  },
  ...
]
```

Add the input entry (before the closing `]`):

```json
[
  {
    "name": "button",
    "description": "DaisyUI button variants with HTMX support",
    "file": "src/button/Button.kt",
    "kotlinPackage": "dev.jwillert.daisyui.components"
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
    "kotlinPackage": "dev.jwillert.daisyui.components"
  }
]
```

- [ ] **Step 3: Commit**

```bash
git add registry/
git commit -m "feat: add koInput to source registry"
```

---

### Task 6: Add slot metadata to registry.json

This enables the `addComponent` task (Task 8) to know which components map to which `KopetalFormsRegistry` slots and generate the correct adapter code.

**Files:**
- Modify: `registry/registry.json`

- [ ] **Step 1: Add slotRegistryClass and slotParams to button and input**

Replace the full `registry/registry.json` content with:

```json
[
  {
    "name": "button",
    "description": "DaisyUI button variants with HTMX support",
    "file": "src/button/Button.kt",
    "kotlinPackage": "dev.jwillert.daisyui.components",
    "slotRegistryClass": "dev.jwillert.kopetal.forms.KopetalFormsRegistry",
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
    "slotRegistryClass": "dev.jwillert.kopetal.forms.KopetalFormsRegistry",
    "slotParams": "name: String, type: String, required: Boolean"
  }
]
```

Note: `modal`, `card`, `table` have no slot fields — they remain registry-only components with no Maven-library counterpart yet.

- [ ] **Step 2: Commit**

```bash
git add registry/registry.json
git commit -m "feat: add slot metadata to registry.json for button and input"
```

---

### Task 7: Extract KopetalComponentsGenerator + tests

Pure generation logic is extracted before wiring it into the Gradle task (Task 8), so it can be unit-tested independently.

**Files:**
- Create: `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGenerator.kt`
- Create: `plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt`
- Modify: `plugin/build.gradle.kts` (add test dependency)

- [ ] **Step 1: Add JUnit 5 to plugin/build.gradle.kts**

Open `plugin/build.gradle.kts` and add:

```kotlin
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("daisyuiPlugin") {
            id = "dev.jwillert.daisyui"
            implementationClass = "dev.jwillert.ktor.daisyui.DaisyUiPlugin"
            displayName = "Daisyui — Tailwind CSS + DaisyUI + Component Registry"
            description = "Installs Tailwind CSS with DaisyUI and provides a shadcn-like component registry for Kotlin HTML DSL"
            tags = listOf("tailwind", "daisyui", "ktor", "kotlin", "htmx", "components")
        }
    }
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
}
```

- [ ] **Step 2: Write failing tests**

Create `plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt`:

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
            slotRegistryClass = "dev.jwillert.kopetal.forms.KopetalFormsRegistry",
            slotParams = "label: String, disabled: Boolean"
        )
        val result = generateConfigureStatement(entry)
        assertEquals(
            "KopetalFormsRegistry.button = { label, disabled -> koButton(label = label, disabled = disabled) }",
            result
        )
    }

    @Test
    fun `generateConfigureStatement generates correct adapter for input`() {
        val entry = ComponentEntry(
            name = "input",
            description = "input",
            file = "src/input/Input.kt",
            kotlinPackage = "dev.jwillert.daisyui.components",
            slotRegistryClass = "dev.jwillert.kopetal.forms.KopetalFormsRegistry",
            slotParams = "name: String, type: String, required: Boolean"
        )
        val result = generateConfigureStatement(entry)
        assertEquals(
            "KopetalFormsRegistry.input = { name, type, required -> koInput(name = name, type = type, required = required) }",
            result
        )
    }

    @Test
    fun `generateKopetalComponentsContent returns empty string when no slot entries`() {
        val entries = listOf(
            ComponentEntry("modal", "modal", "src/modal/Modal.kt", "pkg", null, null),
            ComponentEntry("card", "card", "src/card/Card.kt", "pkg", null, null),
        )
        val result = generateKopetalComponentsContent(
            installedNames = setOf("modal", "card"),
            allRegistryEntries = entries,
            packageName = "com.example.components"
        )
        assertEquals("", result)
    }

    @Test
    fun `generateKopetalComponentsContent generates correct file for button and input`() {
        val entries = listOf(
            ComponentEntry(
                "button", "button", "src/button/Button.kt", "pkg",
                "dev.jwillert.kopetal.forms.KopetalFormsRegistry", "label: String, disabled: Boolean"
            ),
            ComponentEntry(
                "input", "input", "src/input/Input.kt", "pkg",
                "dev.jwillert.kopetal.forms.KopetalFormsRegistry", "name: String, type: String, required: Boolean"
            ),
            ComponentEntry("modal", "modal", "src/modal/Modal.kt", "pkg", null, null),
        )
        val result = generateKopetalComponentsContent(
            installedNames = setOf("button", "input"),
            allRegistryEntries = entries,
            packageName = "com.example.components"
        )

        assertTrue(result.contains("package com.example.components"), "Expected package declaration")
        assertTrue(result.contains("import dev.jwillert.kopetal.forms.KopetalFormsRegistry"), "Expected registry import")
        assertTrue(result.contains("import kotlinx.html.FlowContent"), "Expected FlowContent import")
        assertTrue(result.contains("object KopetalComponents"), "Expected object declaration")
        assertTrue(result.contains("fun configure()"), "Expected configure function")
        assertTrue(result.contains("KopetalFormsRegistry.button"), "Expected button slot assignment")
        assertTrue(result.contains("KopetalFormsRegistry.input"), "Expected input slot assignment")
        assertTrue(result.contains("koButton("), "Expected koButton call")
        assertTrue(result.contains("koInput("), "Expected koInput call")
    }

    @Test
    fun `generateKopetalComponentsContent only includes installed components`() {
        val entries = listOf(
            ComponentEntry(
                "button", "button", "src/button/Button.kt", "pkg",
                "dev.jwillert.kopetal.forms.KopetalFormsRegistry", "label: String, disabled: Boolean"
            ),
            ComponentEntry(
                "input", "input", "src/input/Input.kt", "pkg",
                "dev.jwillert.kopetal.forms.KopetalFormsRegistry", "name: String, type: String, required: Boolean"
            ),
        )
        // Only button is installed, input is not
        val result = generateKopetalComponentsContent(
            installedNames = setOf("button"),
            allRegistryEntries = entries,
            packageName = "com.example.components"
        )

        assertTrue(result.contains("KopetalFormsRegistry.button"), "Expected button slot")
        assertTrue(!result.contains("KopetalFormsRegistry.input"), "Expected no input slot: $result")
    }
}
```

- [ ] **Step 3: Run tests — verify they fail**

```bash
./gradlew :plugin:test
```

Expected: FAIL — `generateConfigureStatement` and `generateKopetalComponentsContent` not found

- [ ] **Step 4: Create KopetalComponentsGenerator.kt**

Create `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGenerator.kt`:

```kotlin
package dev.jwillert.ktor.daisyui.tasks

fun generateConfigureStatement(entry: ComponentEntry): String? {
    val fqn = entry.slotRegistryClass ?: return null
    val params = entry.slotParams ?: return null
    val simpleName = fqn.substringAfterLast(".")
    val paramNames = params.split(",").map { it.trim().substringBefore(":").trim() }
    val args = paramNames.joinToString(", ") { "$it = $it" }
    val fnName = "ko${entry.name.replaceFirstChar { it.uppercaseChar() }}"
    return "$simpleName.${entry.name} = { ${paramNames.joinToString(", ")} -> $fnName($args) }"
}

fun generateKopetalComponentsContent(
    installedNames: Set<String>,
    allRegistryEntries: List<ComponentEntry>,
    packageName: String,
): String {
    val slotEntries = allRegistryEntries.filter { entry ->
        entry.name in installedNames &&
            entry.slotRegistryClass != null &&
            entry.slotParams != null
    }
    if (slotEntries.isEmpty()) return ""

    val imports = slotEntries
        .mapNotNull { it.slotRegistryClass }
        .distinct()
        .sorted()

    val statements = slotEntries.mapNotNull { generateConfigureStatement(it) }

    return buildString {
        appendLine("package $packageName")
        appendLine()
        appendLine("import kotlinx.html.FlowContent")
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

- [ ] **Step 5: Extend ComponentEntry to carry slot fields**

Open `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/AddComponentTask.kt`. Replace the `ComponentEntry` data class (lines 12-17):

```kotlin
data class ComponentEntry(
    val name: String,
    val description: String,
    val file: String,
    val kotlinPackage: String,
    val slotRegistryClass: String? = null,
    val slotParams: String? = null,
)
```

Also update `parseRegistry` to read the two new optional fields:

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
        val kotlinPackage = fields["kotlinPackage"] ?: "dev.jwillert.daisyui.components"
        val slotRegistryClass = fields["slotRegistryClass"]
        val slotParams = fields["slotParams"]
        entries.add(ComponentEntry(name, description, file, kotlinPackage, slotRegistryClass, slotParams))
    }

    return entries
}
```

- [ ] **Step 6: Run all plugin tests — verify they pass**

```bash
./gradlew :plugin:test
```

Expected: `BUILD SUCCESSFUL`, all tests pass

- [ ] **Step 7: Commit**

```bash
git add plugin/
git commit -m "feat: extract KopetalComponentsGenerator with unit tests"
```

---

### Task 8: Wire generator into AddComponentTask

**Files:**
- Modify: `plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/AddComponentTask.kt`

- [ ] **Step 1: Add the generate-and-write step to addComponent()**

Open `AddComponentTask.kt`. After the block that writes the component source file (after line `logger.lifecycle("Component '${component.name}' added at ...")`), add the KopetalComponents generation:

The full updated `addComponent()` function body (replace existing `addComponent()` method):

```kotlin
@TaskAction
fun addComponent() {
    val name = componentName.orNull
        ?: throw GradleException(
            "No component specified. Use: ./gradlew addComponent --componentName=<name>"
        )

    logger.lifecycle("Fetching registry from ${registryUrl.get()} ...")
    val registry = fetchRegistry()

    val component = registry.find { it.name == name }
        ?: throw GradleException(
            "Component '$name' not found in registry.\nAvailable components: ${registry.joinToString(", ") { it.name }}"
        )

    logger.lifecycle("Adding component '${component.name}': ${component.description}")

    val componentSource = fetchComponentSource(component)

    val outputDir = project.file(componentsOutputDir.get())
    outputDir.mkdirs()

    val targetFile = File(outputDir, "${component.name.replaceFirstChar { it.uppercaseChar() }}.kt")

    if (targetFile.exists()) {
        logger.lifecycle("Component file already exists at ${targetFile.relativeTo(project.projectDir)} — overwriting.")
    }

    val packageDeclaration = "package ${derivePackage(outputDir)}"
    val sourceWithPackage = if (componentSource.startsWith("package ")) {
        val lines = componentSource.lines().toMutableList()
        lines[0] = packageDeclaration
        lines.joinToString("\n")
    } else {
        "$packageDeclaration\n\n$componentSource"
    }

    targetFile.writeText(sourceWithPackage)
    logger.lifecycle("Component '${component.name}' added at ${targetFile.relativeTo(project.projectDir)}")

    // Regenerate KopetalComponents.kt for all installed slot components
    val installedNames = outputDir.listFiles()
        ?.filter { it.extension == "kt" && it.name != "KopetalComponents.kt" }
        ?.map { it.nameWithoutExtension.replaceFirstChar { c -> c.lowercaseChar() } }
        ?.toSet() ?: emptySet()

    val packageName = derivePackage(outputDir)
    val componentsContent = generateKopetalComponentsContent(installedNames, registry, packageName)

    if (componentsContent.isNotEmpty()) {
        val componentsFile = File(outputDir, "KopetalComponents.kt")
        componentsFile.writeText(componentsContent)
        logger.lifecycle("KopetalComponents.kt updated at ${componentsFile.relativeTo(project.projectDir)}")
    }
}
```

- [ ] **Step 2: Build the plugin to verify it compiles**

```bash
./gradlew :plugin:build
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Smoke-test the full flow manually**

In a test consumer project (or the coralive-app), run:

```bash
./gradlew addComponent --componentName=button
./gradlew addComponent --componentName=input
```

Expected:
- `src/main/kotlin/components/Button.kt` created
- `src/main/kotlin/components/Input.kt` created
- `src/main/kotlin/components/KopetalComponents.kt` created with content:

```kotlin
package <your.package>.components

import kotlinx.html.FlowContent
import dev.jwillert.kopetal.forms.KopetalFormsRegistry

// Auto-generated by addComponent — do not edit manually
object KopetalComponents {
    fun configure() {
        KopetalFormsRegistry.button = { label, disabled -> koButton(label = label, disabled = disabled) }
        KopetalFormsRegistry.input = { name, type, required -> koInput(name = name, type = type, required = required) }
    }
}
```

- [ ] **Step 4: Run all tests**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL` across all modules

- [ ] **Step 5: Commit**

```bash
git add plugin/src/main/kotlin/dev/jwillert/ktor/daisyui/tasks/AddComponentTask.kt
git commit -m "feat: addComponent generates KopetalComponents.kt for slot wiring"
```

---

## Consumer Usage Summary

After all tasks are complete, a consumer project sets up kopetal as follows:

```kotlin
// build.gradle.kts
plugins {
    id("dev.jwillert.daisyui") version "0.1.0"
}
dependencies {
    implementation("dev.jwillert:kopetal-forms:0.1.0")
}
```

```bash
./gradlew addComponent --componentName=input   # copies koInput, updates KopetalComponents.kt
./gradlew addComponent --componentName=button  # copies koButton, updates KopetalComponents.kt
```

```kotlin
// Application.kt
fun Application.module() {
    install(KopetalForms)         // sets Maven defaults
    KopetalComponents.configure() // overrides with your installed versions
}
```

```kotlin
// In a route
call.respondHtml {
    body {
        formField("Email", "email", type = "email", required = true)
        // → calls your koInput (customizable), not the Maven default
    }
}
```
