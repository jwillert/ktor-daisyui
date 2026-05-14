# Kopetal — Distribution Design

**Date:** 2026-05-14
**Status:** Approved

## Problem

Kopetal soll Komponenten über zwei Mechanismen bereitstellen:

1. **Maven-Dep** — Consumer bindet `kopetal-forms` ein und bekommt Komponenten sofort nutzbar, mit der Möglichkeit einzelne Teile per Slot zu überschreiben.
2. **addComponent** — Consumer kopiert Quellcode direkt ins Projekt und hat volle Kontrolle über CSS und HTML-Struktur.

Beide Wege sollen aus derselben Quelle schöpfen. Kein doppelter Code.

Zusätzlich: Consumer sollen jederzeit von Maven-Dep auf lokale Kopie migrieren können — graduell, ohne harten Cut.

## Lösung: Registry als Single Source of Truth

Die `registry/`-Verzeichnis im Kopetal-Repo ist die einzige Quelle für Komponenten-Implementierungen. Das Maven-Artefakt (`kopetal-forms`) enthält committed Quellen die per `addComponent` aus der Registry importiert wurden — kein Hardcoding.

### Workflow bei Registry-Änderung

1. Komponente in `registry/src/<name>/<Name>.kt` ändern
2. `./gradlew addComponent --componentName=<name>` in `kopetal-forms` ausführen
3. Geänderte Quelldatei committen
4. Neue Maven-Version publishen → Consumer erhalten Update per Version-Bump

## Modul-Struktur

```
kopetal/
├── registry/                     # SSOT — Quellcode-Komponenten
│   ├── registry.json             # Metadaten für addComponent
│   └── src/
│       ├── button/Button.kt
│       ├── input/Input.kt
│       ├── card/Card.kt
│       ├── modal/Modal.kt
│       └── table/Table.kt
│
├── kopetal-core/                  # (umbenannt von kopetal-registry)
│   └── src/main/kotlin/dev/jwillert/kopetal/
│       ├── KopetalRegistry.kt    # RegistryKey, KopetalRegistry
│       └── KopetalUi.kt          # ButtonKey, InputKey, koButton, koInput
│
├── kopetal-forms/                 # Maven-Artefakt
│   └── src/main/kotlin/dev/jwillert/kopetal/forms/
│       ├── components/           # ← per addComponent aus registry/ befüllt (committed)
│       │   ├── Button.kt
│       │   ├── Input.kt
│       │   └── KopetalComponents.kt  # auto-generiert
│       ├── FormField.kt
│       └── KopetalFormsPlugin.kt
│
├── plugin/                        # Gradle-Plugin (addComponent, CSS-Tasks)
└── sample/                        # Beispiel-App
```

`kopetal-ui` wird entfernt — die Funktionalität ist in `kopetal-core` enthalten.

## Registry-Format

`registry.json` beschreibt jede Komponente. Das `slotKey`-Feld ist optional — nur Slot-Komponenten (Button, Input) tragen es ein und erscheinen in `KopetalComponents.configure()`.

```json
[
  {
    "name": "button",
    "description": "DaisyUI Button",
    "file": "src/button/Button.kt",
    "kotlinPackage": "dev.jwillert.kopetal.components",
    "slotRegistryClass": "dev.jwillert.kopetal.KopetalRegistry",
    "slotKey": "dev.jwillert.kopetal.ButtonKey",
    "slotParams": "label: String, disabled: Boolean, block: BUTTON.() -> Unit"
  },
  {
    "name": "input",
    "description": "DaisyUI Input",
    "file": "src/input/Input.kt",
    "kotlinPackage": "dev.jwillert.kopetal.components",
    "slotRegistryClass": "dev.jwillert.kopetal.KopetalRegistry",
    "slotKey": "dev.jwillert.kopetal.InputKey",
    "slotParams": "name: String, type: String, required: Boolean, block: INPUT.() -> Unit"
  }
]
```

## Typsichere Attribut-Erweiterung

Jede Slot-Komponente nimmt einen `block`-Parameter des entsprechenden kotlinx.html-Typs. Damit können Consumer beliebige HTML-Attribute, Event-Handler und HTMX-Attribute typsicher setzen.

```kotlin
// kopetal-core — Slot-Signatur
val ButtonKey = RegistryKey<
    FlowContent.(label: String, disabled: Boolean, block: BUTTON.() -> Unit) -> Unit
>("kopetal.button")

fun FlowContent.koButton(
    label: String,
    disabled: Boolean = false,
    block: BUTTON.() -> Unit = {}
) = (KopetalRegistry[ButtonKey] ?: error("KopetalRegistry[ButtonKey] not installed"))
        .invoke(this, label, disabled, block)
```

```kotlin
// Implementierung (in registry/src/button/Button.kt und in kopetal-forms/components/Button.kt)
KopetalRegistry[ButtonKey] = { label, disabled, block ->
    button(classes = if (disabled) "btn btn-primary btn-disabled" else "btn btn-primary") {
        type = ButtonType.button
        if (disabled) attributes["disabled"] = "disabled"
        +label
        block()
    }
}
```

```kotlin
// Consumer-Aufruf
koButton("Speichern") {
    onClick = "handleSave()"
    attributes["hx-post"] = "/form"
    attributes["data-action"] = "primary"
}
```

Gleiches Muster für `Input` mit `block: INPUT.() -> Unit`. Höhere Komponenten wie `formField` können optional einen `inputBlock: INPUT.() -> Unit = {}` durchreichen.

## Consumer-Setup

### Weg A — Nur Maven-Dep

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.jwillert:kopetal-forms:1.0.0")
}

// Application.kt
fun Application.module() {
    install(KopetalForms)  // setzt Defaults aus Registry

    // Optional: einzelne Slots überschreiben
    KopetalRegistry[ButtonKey] = { label, disabled, block ->
        myButton(label, disabled, block)
    }
}
```

### Weg B — Nur addComponent (kein Maven-Dep)

```kotlin
// build.gradle.kts
kopetal {
    registryUrl = "https://raw.githubusercontent.com/jwillert/kopetal/main/registry"
}
```

```bash
./gradlew addComponent --componentName=button
./gradlew addComponent --componentName=input
```

`Button.kt`, `Input.kt` und `KopetalComponents.kt` landen in `src/` — direkt bearbeitbar, volle Kontrolle.

### Weg C — Migration: Maven-Dep → lokal

Der Slot-Mechanismus ist die Brücke. Beide können gleichzeitig aktiv sein:

```kotlin
// Application.kt — während der Migration
fun Application.module() {
    install(KopetalForms)          // 1. Maven-Defaults aktivieren
    KopetalComponents.configure()  // 2. lokale Versionen per Slot überschreiben
}
// Wenn alle Komponenten lokal: install(KopetalForms) entfernen → Dep kann aus build.gradle.kts
```

**Migrationspfad:**

| Phase | Zustand |
|-------|---------|
| 1 — Nur Dep | `install(KopetalForms)`, alles out-of-the-box |
| 2 — Hybrid | Dep bleibt, einzelne Komponenten per `addComponent` lokal + `KopetalComponents.configure()` |
| 3 — Nur lokal | Alle Komponenten lokal, `install(KopetalForms)` und Dep entfernt |

## Was nicht in Scope ist

- Weitere Module (`kopetal-tables`, `kopetal-auth`)
- Automatisches `addComponent` als Build-Step (bewusst manuell)
- Versionierung einzelner Registry-Einträge (nur das Maven-Artefakt hat eine Version)
- Thread-Safety über Startup-Zeit hinaus (Registry wird nur beim Start beschrieben)
- Validierungslogik in `formField`
- CSS-Theming-Integration
