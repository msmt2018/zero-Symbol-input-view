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
            // Existing Sora Editor integration can reuse the legacy executor:
            // SymbolActionExecutor.execute(editor, action.type, action.payload, drawer.onOpenManagerListener)
        }
    }

drawer.onOpenManagerListener = {
    // Open the host application's symbol manager.
}

drawer.reloadGroups()
drawer.setExpanded(expanded = true, animate = true)
drawer.selectGroup(index = 1)

// Optional manager command model (the UI can be implemented by the host).
val manager = SymbolGroupManager(SharedPreferencesSymbolGroupRepository(this))
val newGroup = manager.addGroup("Custom")
manager.addSymbol(
    newGroup.id,
    SymbolEntry(0L, "=>", SymbolAction(type = 0, payload = " => "))
)
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

## Existing configuration migration

On first V2 startup, if the V2 store is empty, the library checks the legacy
`advanced_symbol_prefs/symbol_json_data` value and performs a one-way copy into
the V2 repository. The legacy preference is never modified. The migration
preserves action IDs and payloads; deterministic IDs are assigned because the
legacy schema did not store stable IDs.

## Symbol management

`SymbolGroupManager` provides repository-backed group and symbol commands:
create, rename, delete and reorder groups; add, edit, remove and reorder symbols;
copy or move symbols between groups. It exposes immutable snapshots for a host
manager screen and persists each successful command.

## Storage

The default repository uses the isolated SharedPreferences namespace `symbol_drawer_v2` and key `groups_json_v2`. The legacy `advanced_symbol_prefs` store is not read or written.

## Design notes

See [ARCHITECTURE.md](ARCHITECTURE.md) for responsibilities, gesture arbitration, performance strategy, and compatibility constraints.
