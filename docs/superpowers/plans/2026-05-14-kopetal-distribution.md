# Kopetal Distribution Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Umstrukturierung von Kopetal auf eine Registry-als-SSOT-Architektur mit zwei Distributionswegen (Maven-Dep + addComponent) und typsicherer `block`-Erweiterbarkeit auf Slot-Ebene.

**Architecture:** Die `registry/`-Verzeichnis ist die einzige Quelle für Komponenten-Implementierungen. `kopetal-core` (umbenannt von `kopetal-registry`) enthält die Infrastruktur (KopetalRegistry, Slot-Keys, Dispatch-Funktionen). `kopetal-forms` bekommt seine Default-Implementierungen per manuell ausgeführtem `addComponent` aus der Registry — committed in `components/`. Jeder Slot akzeptiert einen `block`-Parameter des entsprechenden kotlinx.html-Typs für typsichere HTML-Attribut-Erweiterung.

**Tech Stack:** Kotlin JVM, Gradle multi-project, kotlinx-html-jvm, Ktor, JUnit 5

---

## File Map

| Status | Pfad | Verantwortung |
|--------|------|----------------|
| Rename + merge | `kopetal-registry/` → bleibt als Verzeichnis, Inhalt von `kopetal-ui` hinzugefügt | Infrastruktur: KopetalRegistry + Slot-Keys + Dispatch-Funktionen |
| Modified | `kopetal-registry/build.gradle.kts` | artfiactId → `kopetal-core`, kotlinx-html dep hinzufügen |
| Created | `kopetal-registry/src/main/kotlin/dev/jwillert/kopetal/KopetalUi.kt` | ButtonKey, InputKey, koButton, koInput (mit block) |
| Created | `kopetal-registry/src/test/kotlin/dev/jwillert/kopetal/KopetalUiTest.kt` | Tests für Dispatch + block-Passthrough |
| Deleted | `kopetal-ui/` (ganzes Modul) | — |
| Modified | `settings.gradle.kts` | kopetal-ui entfernen |
| Modified | `kopetal-forms/build.gradle.kts` | kopetal-ui dep entfernen |
| Modified | `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt` | Hardcoded Defaults → KopetalComponents.configure() |
| Created | `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/Button.kt` | Manuell aus registry/ kopiert (package angepasst) |
| Created | `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/Input.kt` | Manuell aus registry/ kopiert (package angepasst) |
| Created | `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/KopetalComponents.kt` | Auto-generiert (manuell erstellt für Plan) |
| Modified | `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt` | Lambda-Signaturen + KopetalComponents.configure() |
| Modified | `registry/src/input/Input.kt` | block-Parameter hinzufügen |
| Modified | `registry/registry.json` | slotParams für block erweitern |
| Modified | `plugin/src/test/kotlin/.../KopetalComponentsGeneratorTest.kt` | Test für block in slotParams |
| Modified | `sample/build.gradle.kts` | kopetal-ui dep entfernen |
| Modified | `sample/src/main/kotlin/sample/Application.kt` | Slot-Override für block-Parameter |

---

## Task 1: kopetal-ui in kopetal-registry mergen

**Files:**
- Create: `kopetal-registry/src/main/kotlin/dev/jwillert/kopetal/KopetalUi.kt`
- Create: `kopetal-registry/src/test/kotlin/dev/jwillert/kopetal/KopetalUiTest.kt`
- Modify: `kopetal-registry/build.gradle.kts`
- Modify: `kopetal-forms/build.gradle.kts`
- Modify: `sample/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Delete: `kopetal-ui/` (ganzes Verzeichnis)

- [ ] **Step 1: KopetalUi.kt in kopetal-registry anlegen**

  `kopetal-registry/src/main/kotlin/dev/jwillert/kopetal/KopetalUi.kt` — gleicher Inhalt wie aktuell `kopetal-ui/src/main/kotlin/dev/jwillert/kopetal/KopetalUi.kt` (noch ohne block, das kommt in Task 2):

  ```kotlin
  package dev.jwillert.kopetal

  import kotlinx.html.FlowContent

  val ButtonKey = RegistryKey<FlowContent.(label: String, disabled: Boolean) -> Unit>("kopetal.button")
  val InputKey = RegistryKey<FlowContent.(name: String, type: String, required: Boolean) -> Unit>("kopetal.input")

  fun FlowContent.koButton(label: String, disabled: Boolean = false) =
      (KopetalRegistry[ButtonKey]
          ?: error("KopetalRegistry[ButtonKey] not installed — register a button implementation before rendering"))
          .invoke(this, label, disabled)

  fun FlowContent.koInput(name: String, type: String = "text", required: Boolean = false) =
      (KopetalRegistry[InputKey]
          ?: error("KopetalRegistry[InputKey] not installed — register an input implementation before rendering"))
          .invoke(this, name, type, required)
  ```

- [ ] **Step 2: KopetalUiTest.kt in kopetal-registry anlegen**

  `kopetal-registry/src/test/kotlin/dev/jwillert/kopetal/KopetalUiTest.kt` — gleicher Inhalt wie in `kopetal-ui/src/test/kotlin/dev/jwillert/kopetal/KopetalUiTest.kt`:

  ```kotlin
  package dev.jwillert.kopetal

  import kotlinx.html.div
  import kotlinx.html.stream.createHTML
  import org.junit.jupiter.api.AfterEach
  import org.junit.jupiter.api.Assertions.*
  import org.junit.jupiter.api.Test

  class KopetalUiTest {

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
  }
  ```

- [ ] **Step 3: Tests laufen lassen — müssen PASS sein (aktuell in kopetal-ui passieren sie)**

  ```bash
  ./gradlew :kopetal-registry:test
  ```

  Erwartet: alle Tests PASS (KopetalUiTest + KopetalRegistryTest)

- [ ] **Step 4: kopetal-registry/build.gradle.kts aktualisieren**

  kotlinx-html dependency hinzufügen + artifactId auf `kopetal-core` setzen:

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
              artifactId = "kopetal-core"
              from(components["java"])
          }
      }
  }
  ```

- [ ] **Step 5: kopetal-forms/build.gradle.kts — kopetal-ui dep entfernen**

  Zeile `implementation(project(":kopetal-ui"))` entfernen:

  ```kotlin
  dependencies {
      implementation(project(":kopetal-registry"))
      implementation("io.ktor:ktor-server-core:3.1.3")
      implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

      testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
      testImplementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
      testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  }
  ```

- [ ] **Step 6: sample/build.gradle.kts — kopetal-ui dep entfernen**

  ```kotlin
  dependencies {
      implementation(project(":kopetal-registry"))
      implementation(project(":kopetal-forms"))
      implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
      implementation("io.ktor:ktor-server-netty:3.1.3")
      implementation("io.ktor:ktor-server-html-builder:3.1.3")
      implementation("ch.qos.logback:logback-classic:1.5.13")
  }
  ```

- [ ] **Step 7: settings.gradle.kts — kopetal-ui entfernen**

  ```kotlin
  include("plugin")
  include("registry")
  include("kopetal-registry")
  include("kopetal-forms")
  include("sample")
  ```

- [ ] **Step 8: Gesamtbuild verifizieren**

  ```bash
  ./gradlew :kopetal-registry:test :kopetal-forms:test :plugin:test
  ```

  Erwartet: alle Tests PASS

- [ ] **Step 9: Commit**

  ```bash
  git rm -r kopetal-ui/
  git add kopetal-registry/src kopetal-registry/build.gradle.kts \
      kopetal-forms/build.gradle.kts sample/build.gradle.kts settings.gradle.kts
  git commit -m "merge kopetal-ui into kopetal-registry, publish as kopetal-core"
  ```

---

## Task 2: `block`-Parameter zu Slot-Signaturen hinzufügen

**Files:**
- Modify: `kopetal-registry/src/main/kotlin/dev/jwillert/kopetal/KopetalUi.kt`
- Modify: `kopetal-registry/src/test/kotlin/dev/jwillert/kopetal/KopetalUiTest.kt`

- [ ] **Step 1: Failing-Tests für block-Passthrough schreiben**

  In `kopetal-registry/src/test/kotlin/dev/jwillert/kopetal/KopetalUiTest.kt` folgende Tests ergänzen:

  ```kotlin
  @Test
  fun `koButton passes block to registered implementation`() {
      var blockInvoked = false
      KopetalRegistry[ButtonKey] = { _, _, block ->
          blockInvoked = true
          createHTML().button { block() }
      }
      createHTML().div { koButton("Click") { } }
      assertTrue(blockInvoked)
  }

  @Test
  fun `koButton block receives correct HTML element context`() {
      KopetalRegistry[ButtonKey] = { _, _, block ->
          button { block() }
      }
      val html = createHTML().div {
          koButton("X") { attributes["data-test"] = "yes" }
      }
      assertTrue(html.contains("data-test=\"yes\""), "Expected data-test attribute: $html")
  }

  @Test
  fun `koInput passes block to registered implementation`() {
      var blockInvoked = false
      KopetalRegistry[InputKey] = { _, _, _, block ->
          blockInvoked = true
          createHTML().input { block() }
      }
      createHTML().div { koInput("name") { } }
      assertTrue(blockInvoked)
  }
  ```

- [ ] **Step 2: Tests laufen lassen — müssen FAIL sein**

  ```bash
  ./gradlew :kopetal-registry:test
  ```

  Erwartet: Compilefehler — `ButtonKey`-Slot-Typ akzeptiert keinen dritten Lambda-Parameter

- [ ] **Step 3: KopetalUi.kt mit block-Parametern aktualisieren**

  ```kotlin
  package dev.jwillert.kopetal

  import kotlinx.html.BUTTON
  import kotlinx.html.FlowContent
  import kotlinx.html.INPUT

  val ButtonKey = RegistryKey<FlowContent.(label: String, disabled: Boolean, block: BUTTON.() -> Unit) -> Unit>("kopetal.button")
  val InputKey = RegistryKey<FlowContent.(name: String, type: String, required: Boolean, block: INPUT.() -> Unit) -> Unit>("kopetal.input")

  fun FlowContent.koButton(
      label: String,
      disabled: Boolean = false,
      block: BUTTON.() -> Unit = {}
  ) = (KopetalRegistry[ButtonKey]
          ?: error("KopetalRegistry[ButtonKey] not installed — register a button implementation before rendering"))
          .invoke(this, label, disabled, block)

  fun FlowContent.koInput(
      name: String,
      type: String = "text",
      required: Boolean = false,
      block: INPUT.() -> Unit = {}
  ) = (KopetalRegistry[InputKey]
          ?: error("KopetalRegistry[InputKey] not installed — register an input implementation before rendering"))
          .invoke(this, name, type, required, block)
  ```

- [ ] **Step 4: Bestehende Tests in KopetalUiTest.kt auf neue Signaturen aktualisieren**

  Alle Lambda-Literale die auf `ButtonKey` registrieren: `{ _, _ -> ... }` → `{ _, _, _ -> ... }` (block als letzter Parameter ignorieren wenn nicht gebraucht).
  
  Alle Lambda-Literale die auf `InputKey` registrieren: `{ _, _, _ -> ... }` → `{ _, _, _, _ -> ... }`.

  Vollständige aktualisierte Datei:

  ```kotlin
  package dev.jwillert.kopetal

  import kotlinx.html.button
  import kotlinx.html.div
  import kotlinx.html.input
  import kotlinx.html.stream.createHTML
  import org.junit.jupiter.api.AfterEach
  import org.junit.jupiter.api.Assertions.*
  import org.junit.jupiter.api.Test

  class KopetalUiTest {

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
          KopetalRegistry[ButtonKey] = { _, _, _ -> called = true }
          createHTML().div { koButton("Click") }
          assertTrue(called)
      }

      @Test
      fun `koInput dispatches to registered implementation`() {
          var called = false
          KopetalRegistry[InputKey] = { _, _, _, _ -> called = true }
          createHTML().div { koInput("email") }
          assertTrue(called)
      }

      @Test
      fun `koButton passes label and disabled correctly`() {
          var capturedLabel = ""
          var capturedDisabled = true
          KopetalRegistry[ButtonKey] = { label, disabled, _ ->
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
          KopetalRegistry[ButtonKey] = { _, disabled, _ -> capturedDisabled = disabled }
          createHTML().div { koButton("Go") }
          assertFalse(capturedDisabled)
      }

      @Test
      fun `koInput passes name, type, required correctly`() {
          var capturedName = ""
          var capturedType = ""
          var capturedRequired = false
          KopetalRegistry[InputKey] = { name, type, required, _ ->
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
          KopetalRegistry[InputKey] = { _, type, required, _ ->
              capturedType = type
              capturedRequired = required
          }
          createHTML().div { koInput("username") }
          assertEquals("text", capturedType)
          assertFalse(capturedRequired)
      }

      @Test
      fun `koButton passes block to registered implementation`() {
          var blockInvoked = false
          KopetalRegistry[ButtonKey] = { _, _, block ->
              blockInvoked = true
              createHTML().button { block() }
          }
          createHTML().div { koButton("Click") { } }
          assertTrue(blockInvoked)
      }

      @Test
      fun `koButton block receives correct HTML element context`() {
          KopetalRegistry[ButtonKey] = { _, _, block ->
              button { block() }
          }
          val html = createHTML().div {
              koButton("X") { attributes["data-test"] = "yes" }
          }
          assertTrue(html.contains("data-test=\"yes\""), "Expected data-test attribute: $html")
      }

      @Test
      fun `koInput passes block to registered implementation`() {
          var blockInvoked = false
          KopetalRegistry[InputKey] = { _, _, _, block ->
              blockInvoked = true
              createHTML().input { block() }
          }
          createHTML().div { koInput("name") { } }
          assertTrue(blockInvoked)
      }
  }
  ```

- [ ] **Step 5: Tests laufen lassen — müssen PASS sein**

  ```bash
  ./gradlew :kopetal-registry:test
  ```

  Erwartet: alle Tests PASS

- [ ] **Step 6: Commit**

  ```bash
  git add kopetal-registry/src/main/kotlin/dev/jwillert/kopetal/KopetalUi.kt \
      kopetal-registry/src/test/kotlin/dev/jwillert/kopetal/KopetalUiTest.kt
  git commit -m "add block parameter to ButtonKey, InputKey, koButton, koInput slots"
  ```

---

## Task 3: Registry-Komponenten aktualisieren (Input + registry.json)

**Files:**
- Modify: `registry/src/input/Input.kt`
- Modify: `registry/registry.json`
- Modify: `plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt`

- [ ] **Step 1: Test für Generator mit block-Parameter schreiben**

  In `plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt` ergänzen:

  ```kotlin
  @Test
  fun `generateConfigureStatement handles block parameter in slotParams`() {
      val entry = ComponentEntry(
          name = "button",
          description = "button",
          file = "src/button/Button.kt",
          kotlinPackage = "dev.jwillert.daisyui.components",
          slotRegistryClass = "dev.jwillert.kopetal.KopetalRegistry",
          slotKey = "dev.jwillert.kopetal.ButtonKey",
          slotParams = "label: String, disabled: Boolean, block: BUTTON.() -> Unit"
      )
      assertEquals(
          "KopetalRegistry[ButtonKey] = { label, disabled, block -> koButton(label = label, disabled = disabled, block = block) }",
          generateConfigureStatement(entry)
      )
  }
  ```

- [ ] **Step 2: Test laufen lassen — muss PASS sein (Generator ist bereits generisch genug)**

  ```bash
  ./gradlew :plugin:test --tests "*.KopetalComponentsGeneratorTest.generateConfigureStatement handles block parameter in slotParams"
  ```

  Erwartet: PASS — kein Codechange am Generator nötig

- [ ] **Step 3: registry/src/input/Input.kt — block-Parameter hinzufügen**

  ```kotlin
  package dev.jwillert.daisyui.components

  import kotlinx.html.FlowContent
  import kotlinx.html.INPUT
  import kotlinx.html.InputType
  import kotlinx.html.input

  fun FlowContent.koInput(
      name: String,
      type: String = "text",
      placeholder: String = "",
      required: Boolean = false,
      disabled: Boolean = false,
      additionalClasses: String = "",
      block: INPUT.() -> Unit = {}
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
          block()
      }
  }
  ```

- [ ] **Step 4: registry/registry.json — slotParams für button und input aktualisieren**

  ```json
  [
    {
      "name": "button",
      "description": "DaisyUI button variants with HTMX support",
      "file": "src/button/Button.kt",
      "kotlinPackage": "dev.jwillert.daisyui.components",
      "slotRegistryClass": "dev.jwillert.kopetal.KopetalRegistry",
      "slotKey": "dev.jwillert.kopetal.ButtonKey",
      "slotParams": "label: String, disabled: Boolean, block: BUTTON.() -> Unit"
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
      "slotParams": "name: String, type: String, required: Boolean, block: INPUT.() -> Unit"
    }
  ]
  ```

- [ ] **Step 5: Plugin-Tests laufen lassen**

  ```bash
  ./gradlew :plugin:test
  ```

  Erwartet: alle Tests PASS

- [ ] **Step 6: Commit**

  ```bash
  git add registry/src/input/Input.kt registry/registry.json \
      plugin/src/test/kotlin/dev/jwillert/ktor/daisyui/tasks/KopetalComponentsGeneratorTest.kt
  git commit -m "add block parameter to registry Input component and update slotParams in registry.json"
  ```

---

## Task 4: kopetal-forms/components/ manuell befüllen

**Files:**
- Create: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/Button.kt`
- Create: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/Input.kt`
- Create: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/KopetalComponents.kt`

Diese Dateien entsprechen dem was `addComponent` automatisch erzeugen würde. Bei zukünftigen Registry-Änderungen: Plugin in kopetal-forms konfigurieren (s. Ende dieses Tasks) und `addComponent` erneut ausführen.

- [ ] **Step 1: Verzeichnis anlegen**

  ```bash
  mkdir -p kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components
  ```

- [ ] **Step 2: Button.kt anlegen**

  `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/Button.kt` — Kopie aus `registry/src/button/Button.kt` mit angepasstem Package:

  ```kotlin
  package dev.jwillert.kopetal.forms.components

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
  ```

- [ ] **Step 3: Input.kt anlegen**

  `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/Input.kt` — Kopie aus `registry/src/input/Input.kt` (nach Task 3 mit block) mit angepasstem Package:

  ```kotlin
  package dev.jwillert.kopetal.forms.components

  import kotlinx.html.FlowContent
  import kotlinx.html.INPUT
  import kotlinx.html.InputType
  import kotlinx.html.input

  fun FlowContent.koInput(
      name: String,
      type: String = "text",
      placeholder: String = "",
      required: Boolean = false,
      disabled: Boolean = false,
      additionalClasses: String = "",
      block: INPUT.() -> Unit = {}
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
          block()
      }
  }
  ```

- [ ] **Step 4: KopetalComponents.kt anlegen**

  `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/KopetalComponents.kt`:

  ```kotlin
  package dev.jwillert.kopetal.forms.components

  import dev.jwillert.kopetal.ButtonKey
  import dev.jwillert.kopetal.InputKey
  import dev.jwillert.kopetal.KopetalRegistry

  // Auto-generated by addComponent — do not edit manually
  object KopetalComponents {
      fun configure() {
          KopetalRegistry[ButtonKey] = { label, disabled, block -> koButton(label = label, disabled = disabled, block = block) }
          KopetalRegistry[InputKey] = { name, type, required, block -> koInput(name = name, type = type, required = required, block = block) }
      }
  }
  ```

- [ ] **Step 5: Kompilierung prüfen**

  ```bash
  ./gradlew :kopetal-forms:compileKotlin
  ```

  Erwartet: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

  ```bash
  git add kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/components/
  git commit -m "add components directory to kopetal-forms with button, input and KopetalComponents"
  ```

  **Hinweis für zukünftige Registry-Updates:** Um diese Dateien neu zu generieren wenn sich die Registry ändert, den Befehl aus Task 4 Step 7 (unten) wiederholen. Alternativ kann das Plugin in kopetal-forms/build.gradle.kts konfiguriert werden (nach `./gradlew :plugin:publishToMavenLocal`):

  ```kotlin
  // kopetal-forms/build.gradle.kts — nach Plugin-Publish
  plugins {
      id("dev.jwillert.daisyui") version "0.1.0"
  }

  daisyui {
      registryUrl.set("file://${rootDir}/registry/registry.json")
      componentsOutputDir.set("src/main/kotlin/dev/jwillert/kopetal/forms/components")
  }
  ```

  Dann: `./gradlew :kopetal-forms:addComponent --componentName=button && ./gradlew :kopetal-forms:addComponent --componentName=input`

---

## Task 5: KopetalFormsPlugin auf KopetalComponents.configure() umstellen

**Files:**
- Modify: `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt`
- Modify: `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt`

- [ ] **Step 1: Tests aktualisieren**

  `kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt`:

  ```kotlin
  package dev.jwillert.kopetal.forms

  import dev.jwillert.kopetal.ButtonKey
  import dev.jwillert.kopetal.InputKey
  import dev.jwillert.kopetal.KopetalRegistry
  import dev.jwillert.kopetal.koButton
  import dev.jwillert.kopetal.koInput
  import dev.jwillert.kopetal.forms.components.KopetalComponents
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
          KopetalRegistry[ButtonKey] = { _, _, _ -> called = true }
          createHTML().div { koButton("Click me") }
          assertTrue(called)
      }

      @Test
      fun `input slot can be overridden independently of button`() {
          var inputCalled = false
          KopetalRegistry[InputKey] = { _, _, _, _ -> inputCalled = true }
          createHTML().div { koInput("email") }
          assertTrue(inputCalled)
      }

      @Test
      fun `default button renders with label and btn class`() {
          KopetalComponents.configure()

          val html = createHTML().div {
              koButton("Save")
          }

          assertTrue(html.contains("Save"), "Expected label in output: $html")
          assertTrue(html.contains("btn"), "Expected btn class in output: $html")
      }

      @Test
      fun `default input renders with name attribute`() {
          KopetalComponents.configure()

          val html = createHTML().div {
              koInput("username")
          }

          assertTrue(html.contains("username"), "Expected name in output: $html")
          assertTrue(html.contains("input"), "Expected input element in output: $html")
      }

      @Test
      fun `formField renders label text and delegates input to registry`() {
          KopetalComponents.configure()

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
          KopetalRegistry[InputKey] = { name, _, required, _ ->
              capturedName = name
              capturedRequired = required
          }

          createHTML().div {
              formField("Email", "my-email", required = true)
          }

          assertTrue(capturedName == "my-email", "Expected name 'my-email', got '$capturedName'")
          assertTrue(capturedRequired, "Expected required=true")
      }

      @Test
      fun `koButton block is invoked and can set html attributes`() {
          KopetalComponents.configure()
          val html = createHTML().div {
              koButton("Save") {
                  attributes["data-test"] = "yes"
              }
          }
          assertTrue(html.contains("data-test=\"yes\""), "Expected data-test attribute: $html")
      }
  }
  ```

- [ ] **Step 2: Tests laufen lassen — müssen PASS sein**

  ```bash
  ./gradlew :kopetal-forms:test
  ```

  Erwartet: PASS — `KopetalComponents` existiert bereits aus Task 4, die Tests rufen `configure()` direkt auf. Step 3 stellt sicher dass auch `install(KopetalForms)` (der Plugin-Einstiegspunkt für Consumer) dasselbe tut.

- [ ] **Step 3: KopetalFormsPlugin.kt aktualisieren**

  `kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt`:

  ```kotlin
  package dev.jwillert.kopetal.forms

  import dev.jwillert.kopetal.forms.components.KopetalComponents
  import io.ktor.server.application.createApplicationPlugin

  val KopetalForms = createApplicationPlugin("KopetalForms") {
      KopetalComponents.configure()
  }
  ```

- [ ] **Step 4: Tests laufen lassen — müssen PASS sein**

  ```bash
  ./gradlew :kopetal-forms:test
  ```

  Erwartet: alle Tests PASS

- [ ] **Step 5: Commit**

  ```bash
  git add kopetal-forms/src/main/kotlin/dev/jwillert/kopetal/forms/KopetalFormsPlugin.kt \
      kopetal-forms/src/test/kotlin/dev/jwillert/kopetal/forms/KopetalFormsRegistryTest.kt
  git commit -m "replace hardcoded plugin defaults with KopetalComponents.configure()"
  ```

---

## Task 6: Sample-App aktualisieren

**Files:**
- Modify: `sample/src/main/kotlin/sample/Application.kt`

- [ ] **Step 1: Application.kt — Slot-Override für block-Parameter aktualisieren**

  Der `ButtonKey`-Slot in der Sample-App hat aktuell die alte Signatur (`{ label, disabled -> ... }`). Mit dem neuen block-Parameter muss er auf `{ label, disabled, block -> ... }` erweitert werden. `InputKey` wird weiterhin auf den Default aus `KopetalComponents.configure()` gesetzt:

  ```kotlin
  fun Application.module() {
      install(KopetalForms)

      // Override button slot — eigenes Styling, block wird durchgereicht
      KopetalRegistry[ButtonKey] = { label, disabled, block ->
          button(classes = if (disabled) "btn btn-ghost btn-disabled" else "btn btn-secondary") {
              type = ButtonType.button
              if (disabled) attributes["disabled"] = "disabled"
              +label
              block()
          }
      }
  }
  ```

- [ ] **Step 2: Sample kompilieren**

  ```bash
  ./gradlew :sample:compileKotlin
  ```

  Erwartet: BUILD SUCCESSFUL

- [ ] **Step 3: Gesamtbuild und alle Tests**

  ```bash
  ./gradlew build
  ```

  Erwartet: BUILD SUCCESSFUL, alle Tests PASS

- [ ] **Step 4: Commit**

  ```bash
  git add sample/src/main/kotlin/sample/Application.kt
  git commit -m "update sample app slot override for block parameter"
  ```

---

## Gesamt-Verifikation

- [ ] Alle Tests grün: `./gradlew test`
- [ ] Sample läuft: `./gradlew :sample:run` und http://localhost:8080 aufrufen
- [ ] Verifizieren dass `koButton("Submit") { attributes["hx-post"] = "/form" }` in sample funktioniert
