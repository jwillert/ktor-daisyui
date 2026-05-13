# Kopetal — Hybrid Registry + Maven Library Design

**Date:** 2026-05-12  
**Status:** Approved

## Problem

Kopetal hat bereits einen shadcn-ähnlichen Registry-Mechanismus (Quellcode-Kopie via `addComponent`). Es fehlen zwei Dinge:

1. Base-Komponenten (z.B. `Input`) die der Consumer anpassen kann
2. Higher-level Komponenten (z.B. `FormField`) die per Maven verfügbar sind und automatisch die angepassten Base-Komponenten des Consumers verwenden

## Lösung: Zwei-Schichten-Architektur

### Schicht 1 — Registry (Quellcode-Kopie, consumer-owned)

Komponenten werden per `./gradlew addComponent --componentName=input` in das Consumer-Projekt kopiert. Der Consumer besitzt und modifiziert den Code frei. Diese Schicht ist für Base-Komponenten gedacht.

### Schicht 2 — Maven Library (compiled, automatisch aktualisierbar)

Higher-level Komponenten die auf Base-Komponenten aufbauen werden als Maven-Artefakte ausgeliefert. Sie referenzieren Base-Komponenten nicht direkt, sondern über ein **modul-lokales Registry-Objekt**.

### Verbindung: Modul-lokale Registry (kein zentrales kopetal-api)

Jedes Maven-Modul definiert sein **eigenes** Registry-Objekt und seine eigenen Slot-Typen als Kotlin-Funktionstypen. Keine geteilten Prop-Klassen zwischen Modulen.

Der Vorteil: Der Consumer schreibt einen Adapter-Lambda und kann in seiner eigenen Implementierung beliebig viele eigene Props hinzufügen — solange die Signatur des Slots erfüllt wird.

```
App-Startup:
  install(KopetalForms)                →  KopetalFormsRegistry.button = maven-default
                                           KopetalFormsRegistry.input  = maven-default

  KopetalComponents.configure()        →  KopetalFormsRegistry.button = eigene Implementierung
                                           KopetalFormsRegistry.input  = eigene Implementierung

Request-Zeit (read-only, thread-safe):
  formField("Email", "email")  →  KopetalFormsRegistry.input("email", "text", false)
                               →  Consumer-Adapter wird aufgerufen
                               →  koInput(name, type, required, myExtraProp = "...")
```

## Modul-Abhängigkeiten

```
kopetal-forms  →  (keine eigene module-dependency, nur kotlinx.html + ktor)
plugin         →  (keine Abhängigkeit auf forms)
registry       →  (kein Gradle-Modul, nur Quellcode-Dateien)
```

Consumer-Projekt:
```
consumer  →  kopetal-forms  (implementation dependency)
consumer  →  plugin         (nur buildscript/plugins block)
```

## Modul-Struktur

```
kopetal/
├── plugin/          # Gradle-Plugin (vorhanden)
│                    # Publiziert als: dev.jwillert:kopetal-plugin
├── registry/        # Quellcode-Registry (vorhanden + koInput neu)
│   ├── registry.json
│   └── src/
│       ├── button/Button.kt
│       ├── input/Input.kt      ← NEU
│       ├── card/Card.kt
│       ├── modal/Modal.kt
│       └── table/Table.kt
└── kopetal-forms/   # NEU — Publiziert als: dev.jwillert:kopetal-forms
    └── src/
        ├── KopetalFormsRegistry.kt  # Slot-Typen + Registry-Objekt
        ├── FormField.kt             # nutzt KopetalFormsRegistry
        └── KopetalFormsPlugin.kt    # Ktor Plugin mit Maven-Defaults
```

## kopetal-forms

### Slot-Typen und Registry

Jeder Slot ist ein einfacher Kotlin-Funktionstyp. Die Parameter sind genau das was `kopetal-forms` beim Aufruf übergeben wird — nicht mehr, nicht weniger.

```kotlin
// Nur die Parameter die kopetal-forms selbst kennt und übergibt:
typealias FormsButtonSlot = FlowContent.(label: String, disabled: Boolean) -> Unit
typealias FormsInputSlot  = FlowContent.(name: String, type: String, required: Boolean) -> Unit

object KopetalFormsRegistry {
    var button: FormsButtonSlot = { label, disabled -> /* maven default */ }
    var input:  FormsInputSlot  = { name, type, required -> /* maven default */ }
}
```

Der Consumer registriert einen Adapter — seine eigene Implementierung kann mehr Props haben, solange der Slot-Vertrag erfüllt ist:

```kotlin
// Consumer hat eigenen Button mit extra icon-Prop:
KopetalFormsRegistry.button = { label, disabled ->
    myButton(label = label, disabled = disabled, icon = null)  // icon = eigener Default
}

// Oder direkt auf seine koButton-Funktion zeigen wenn die Signatur passt:
KopetalFormsRegistry.button = FlowContent::koButton
```

### Ktor-Plugin

```kotlin
val KopetalForms = createApplicationPlugin("KopetalForms") {
    KopetalFormsRegistry.button = { label, disabled ->
        button(classes = if (disabled) "btn btn-disabled" else "btn btn-primary") { +label }
    }
    KopetalFormsRegistry.input = { name, type, required ->
        input(classes = "input input-bordered") {
            this.name = name
            this.type = InputType.valueOf(type)
            if (required) attributes["required"] = "required"
        }
    }
}
```

### FormField

```kotlin
fun FlowContent.formField(
    label: String,
    name: String,
    type: String = "text",
    required: Boolean = false,
    disabled: Boolean = false,
) {
    div(classes = "form-control") {
        label(classes = "label") { span { +label } }
        KopetalFormsRegistry.input(name, type, required)
    }
}
```

## addComponent — Erweiterung

Der `addComponent`-Task kopiert wie bisher den Quellcode. Zusätzlich schreibt/aktualisiert er `KopetalComponents.kt`:

```kotlin
// Automatisch generiert — nicht manuell bearbeiten
object KopetalComponents {
    fun configure() {
        KopetalFormsRegistry.button = FlowContent::koButton
        KopetalFormsRegistry.input  = FlowContent::koInput
    }
}
```

`KopetalComponents.kt` wird generiert in: `{componentsOutputDir}/KopetalComponents.kt`  
(default: `src/main/kotlin/components/KopetalComponents.kt`)

Die Datei wird bei jedem `addComponent`-Aufruf neu geschrieben. Wenn eine Komponente nicht installiert ist, erscheint sie nicht in `configure()` — der Maven-Default bleibt aktiv.

## Consumer-Setup (Komplettbeispiel)

```kotlin
// build.gradle.kts
plugins {
    id("dev.jwillert.daisyui") version "0.2.0"
}

dependencies {
    implementation("dev.jwillert:kopetal-forms:0.1.0")
}
```

```bash
./gradlew addComponent --componentName=input
./gradlew addComponent --componentName=button
```

```kotlin
// Application.kt
fun Application.module() {
    install(KopetalForms)         // 1. Maven-Defaults registrieren
    KopetalComponents.configure() // 2. Eigene Versionen aus Registry überschreiben
}
```

```kotlin
// In einer Route — kein Slot-Parameter nötig:
call.respondHtml {
    body {
        formField("Email", "email", type = "email", required = true)
        // → nutzt KopetalFormsRegistry.input → Consumer's koInput
    }
}
```

## Erweiterbarkeit: weitere Module

Jedes spätere Modul (z.B. `kopetal-tables`) folgt demselben Muster — eigenes Registry-Objekt, eigene Slot-Typen. `KopetalComponents.configure()` wird um die neuen Slots erweitert sobald das Modul installiert wird.

```kotlin
// Zukünftig:
object KopetalTablesRegistry {
    var actionButton: FlowContent.(label: String, href: String) -> Unit = { ... }
    var badge: FlowContent.(text: String, color: String) -> Unit = { ... }
}
```

## Erster Release-Scope (v0.1.0)

| Was | Typ | Status |
|-----|-----|--------|
| `koButton` | Registry-Komponente | vorhanden — Signatur prüfen/anpassen |
| `koInput` | Registry-Komponente | NEU |
| `KopetalFormsRegistry` + Slot-Typen | kopetal-forms | NEU |
| `KopetalForms` Ktor-Plugin | kopetal-forms | NEU |
| `formField` | kopetal-forms | NEU |
| `addComponent` generiert `KopetalComponents.kt` | plugin | NEU |

## Was explizit nicht in Scope ist

- Weitere thematische Artefakte (`kopetal-tables`, `kopetal-auth`)
- Weitere Slot-Typen über Button und Input hinaus
- Validierungslogik in FormField
- CSS-Theming-Integration
