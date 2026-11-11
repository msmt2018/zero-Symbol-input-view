# Zero Symbol Input View V2

V2 is a parallel, independently packaged implementation of the integrated symbol drawer. It does not replace or write to the legacy library.

## Include in this repository

The root Gradle settings already include:

```kotlin
implementation(project(":zero-symbol-input-view-v2"))
```

The module directory is intentionally nested at:

`zero-Symbol-input-view/zero-symbol-input-view-v2`

## Basic use

```kotlin
val drawer = findViewById<AdvancedSymbolDrawerView>(R.id.symbol_drawer_v2)

drawer.onSymbolActionListener =
    AdvancedSymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
        val action = if (isLongPress) symbol.longAction else symbol.shortAction
        if (action != null) {
            // Bridge action.type / action.payload to the host editor's action executor.
        }
    }

drawer.onOpenManagerListener = {
    // Open the host application's symbol manager.
}

drawer.reloadGroups()
drawer.setExpanded(expanded = true, animate = true)
drawer.selectGroup(index = 1)
```

## Data model

Submit immutable snapshots with stable group and symbol IDs:

```kotlin
drawer.submitGroups(
    listOf(
        SymbolGroup(
            id = "kotlin",
            title = "Kotlin",
            symbols = listOf(
                SymbolEntry(
                    id = 1L,
                    label = "fun",
                    shortAction = SymbolAction(type = 0, payload = "fun ")
                )
            )
        )
    )
)
```

## Storage

The default repository uses the isolated SharedPreferences namespace `symbol_drawer_v2` and key `groups_json_v2`. The legacy `advanced_symbol_prefs` store is not read or written.

## Design notes

See [ARCHITECTURE.md](ARCHITECTURE.md) for responsibilities, gesture arbitration, performance strategy, and compatibility constraints.
