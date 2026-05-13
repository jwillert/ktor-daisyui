# Kopetal — Zentrale Registry Design

**Date:** 2026-05-12  
**Status:** Approved  
**Replaces:** 2026-05-12-kopetal-hybrid-registry-design.md

## Problem

Das vorherige Design hat pro Maven-Modul ein eigenes Registry-Objekt (`KopetalFormsRegistry`). Wenn der Consumer 10 Module nutzt und jedes Modul einen `button`-Slot hat, muss er `button` 10x überschreiben — ein N×M-Problem.

## Lösung: Eine zentrale `KopetalRegistry` als reiner Key-Value-Store

### `kopetal-core` — das neue Basismodul

Ein schlankes Maven-Modul, das als einzige Dependency aller höheren Kopetal-Module dient:

```
kopetal-core  →  kotlinx-html-jvm (einzige externe Dependency)
kopetal-forms →  kopetal-core + ktor-server-core
```

### `KopetalRegistry` — reiner generischer Store

`KopetalRegistry` selbst hat **keine hardcodierten Properties**. Alle Slots gehen über typsichere `RegistryKey<T>`-Instanzen. Neue Module erweitern das System ohne `KopetalRegistry` selbst anzufassen.

```kotlin
class RegistryKey<T : Any>(val id: String)

object KopetalRegistry {
    private val slots = mutableMapOf<RegistryKey<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: RegistryKey<T>): T? = slots[key] as? T
    operator fun <T : Any> set(key: RegistryKey<T>, value: T) { slots[key] = value }
    fun <T : Any> remove(key: RegistryKey<T>) { slots.remove(key) }
}
```

### Shared Primitive Keys + Dispatch Functions

Ebenfalls in `kopetal-core` — einmal definiert, alle Module rufen sie auf:

```kotlin
val ButtonKey = RegistryKey<FlowContent.(label: String, disabled: Boolean) -> Unit>("kopetal.button")
val InputKey  = RegistryKey<FlowContent.(name: String, type: String, required: Boolean) -> Unit>("kopetal.input")

fun FlowContent.koButton(label: String, disabled: Boolean = false) =
    (KopetalRegistry[ButtonKey] ?: error("not installed")).invoke(this, label, disabled)

fun FlowContent.koInput(name: String, type: String = "text", required: Boolean = false) =
    (KopetalRegistry[InputKey] ?: error("not installed")).invoke(this, name, type, required)
```

### Consumer-Setup

```kotlin
// Application.kt
fun Application.module() {
    install(KopetalForms)         // setzt KopetalRegistry[ButtonKey] + [InputKey]
    KopetalComponents.configure() // überschreibt mit eigenen Versionen
}

// Oder manuell:
KopetalRegistry[ButtonKey] = { label, disabled -> myButton(label, disabled) }
```

```kotlin
// Template
koButton("Save")
koInput("email", type = "email", required = true)
```

### Naming: `koButton` in `kopetal-core` vs. Consumer-Implementierung

`kopetal-core` definiert `koButton` als Dispatch-Funktion. Wenn der Consumer per `addComponent --componentName=button` seine eigene Implementierung kopiert, entsteht eine zweite `koButton`-Funktion im Consumer-Projekt — in einem anderen Package. Kein Konflikt: in Templates importiert man `kopetal-core`'s `koButton`; in `KopetalComponents.kt` importiert man die eigene Implementierung zum Registrieren.

### Modul-spezifische Slots (Erweiterbarkeit)

```kotlin
// In kopetal-tables (zukünftig):
val RowActionKey = RegistryKey<FlowContent.(label: String, href: String) -> Unit>("kopetal.tables.rowAction")

fun FlowContent.koRowAction(label: String, href: String) =
    (KopetalRegistry[RowActionKey] ?: error("not installed")).invoke(this, label, href)
```

`KopetalRegistry` selbst bleibt unverändert.

## Modul-Struktur

```
kopetal/
├── plugin/            # Gradle-Plugin
├── registry/          # Quellcode-Komponenten (shadcn-style)
├── kopetal-core/      # NEU — dev.jwillert:kopetal-core
│   └── src/main/kotlin/dev/jwillert/kopetal/
│       └── KopetalRegistry.kt   # RegistryKey, KopetalRegistry, ButtonKey, InputKey, koButton, koInput
└── kopetal-forms/     # dev.jwillert:kopetal-forms (depends on kopetal-core)
    └── src/main/kotlin/dev/jwillert/kopetal/forms/
        ├── KopetalFormsPlugin.kt  # setzt KopetalRegistry[ButtonKey] und [InputKey]
        └── FormField.kt           # ruft koInput() aus kopetal-core auf
```

`KopetalFormsRegistry.kt` wird gelöscht.

## Änderungen an Plugin und Registry

`registry.json` bekommt ein neues `slotKey`-Feld (FQN des `RegistryKey`-Wertes):

```json
{
  "slotRegistryClass": "dev.jwillert.kopetal.KopetalRegistry",
  "slotKey": "dev.jwillert.kopetal.ButtonKey",
  "slotParams": "label: String, disabled: Boolean"
}
```

`KopetalComponentsGenerator` produziert dann:

```kotlin
import dev.jwillert.kopetal.ButtonKey
import dev.jwillert.kopetal.InputKey
import dev.jwillert.kopetal.KopetalRegistry

object KopetalComponents {
    fun configure() {
        KopetalRegistry[ButtonKey] = { label, disabled -> koButton(label = label, disabled = disabled) }
        KopetalRegistry[InputKey]  = { name, type, required -> koInput(name = name, type = type, required = required) }
    }
}
```

## Was nicht in Scope ist

- Weitere Module (`kopetal-tables`, `kopetal-auth`)
- Thread-Safety über Kotlin Coroutines (startup-time registry ist safe by design)
- Validierungslogik, CSS-Theming
