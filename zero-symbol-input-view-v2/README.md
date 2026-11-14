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
val drawer = findViewById<SymbolDrawerView>(R.id.symbol_drawer_v2)

drawer.onSymbolActionListener =
    SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
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
    SymbolEntry(0L, "=>", SymbolAction.InsertText(" => "))
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
                    shortAction = SymbolAction.InsertText("fun ")
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

The default repository uses the isolated SharedPreferences namespace `symbol_drawer_v2` and key `groups_json_v2`. New values use the versioned V2 typed-action envelope, preserving stable IDs, icons, independent short/long actions and legacy action metadata. Existing V1 compact (`n`/`d`, `a`/`b`/`c`/`d`/`e`) values and the previous unversioned V2 draft array remain readable and are upgraded on the next successful save. The legacy `advanced_symbol_prefs` store is only read during one-way migration and is never modified.

## Design notes

See [ARCHITECTURE.md](ARCHITECTURE.md) for responsibilities, gesture arbitration, performance strategy, and compatibility constraints.

## Settings and typed actions

V2 settings are represented by immutable `SymbolDrawerSettings` snapshots and
persisted through `SymbolDrawerSettingsRepository`. The JSON codec supplies
defaults for missing fields, validates enum values, and reports malformed
storage so the host can surface a recovery notice. The settings screen and
runtime application are being built on this contract.

Runtime actions and V2 persistence use the sealed, versioned `SymbolAction` model. Legacy numeric action IDs and text payloads are retained as compatibility metadata when imported; the manager's current editor exposes the V1-compatible selector, while the public V2 model also supports composite, surround-selection and extension actions. Implement `SymbolActionHost` to bridge the
actions to a concrete editor. `SymbolActionExecutor` validates IDs, payload
sizes and composite depth and returns structured execution results. Legacy
integer actions are translated only by `LegacySymbolActionAdapter` during
migration; unknown IDs remain explicit extension actions.

### Embedding the built-in settings screen

The library provides a programmatic settings screen that can be embedded in a
host Activity or Fragment. It persists settings and reports each saved snapshot:

```kotlin
val settingsScreen = SymbolDrawerSettingsScreen(this)
settingsScreen.onSettingsChanged = drawer::applySettings
settingsScreen.onError = { error ->
    // Show a host-level error message.
}
drawer.applySettings(settingsScreen.getSettings())
setContentView(settingsScreen)
```

## Built-in symbol manager

The V2 manager screen provides group CRUD/reordering/duplication, symbol
create/edit/delete/duplicate/move/reordering, search, and JSON backup import
and export. Every symbol has its own V1-style short-action and optional long-action selector. The exact V1 numeric action IDs, ordering, text parameter behavior and macro syntax are used; editing does not require hand-written JSON.

```kotlin
val managerScreen = SymbolDrawerManagerScreen(this)
managerScreen.onGroupsChanged = { groups -> drawer.submitGroups(groups) }
managerScreen.onExportBundle = { json ->
    // Optionally write the JSON to a document picked by the host.
}
managerScreen.onError = { error ->
    // Show an application-level error.
}
setContentView(managerScreen)
```

Import validates the bundle and asks for confirmation before replacing groups.
Export also copies the JSON backup to the clipboard; hosts can use
`onExportBundle` to save it to a file or cloud destination.

The settings screen also supports exporting its versioned JSON settings snapshot
and importing a validated snapshot. Export is copied to the clipboard and
reported through `onExportJson`; import validates and persists before notifying
the host through `onSettingsChanged`.

## JitPack distribution

The V2 Android library is published as a separate Maven artifact through JitPack.
The repository is public, so consumers can resolve the artifact from the public
JitPack Maven endpoint without a personal access token.

Add the repository and dependency to the consuming project's Gradle configuration:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:v0.25.0")
}
```

Replace `v0.25.0` with a published release tag. To publish a release, open **Actions → Publish V2 library to JitPack → Run workflow**
in GitHub, select the branch to release, and enter a version such as `0.25.0`
(without the `v` prefix). The workflow normalizes it to tag `v0.25.0`, runs
the V2 unit tests, builds the release AAR and sources JAR, validates the local
Maven publication, creates and pushes the Git tag, and waits for JitPack to
expose the POM and AAR. Re-running the same version against the same commit is
safe; an existing tag pointing to another commit is rejected.

No Maven credentials, signing keys, or GitHub Packages secrets are required.
The workflow uses the repository's `GITHUB_TOKEN` permission to create the tag;
JitPack builds the public Git tag and serves the Maven artifact. Normal Android
CI only builds and tests the library and APK; it does not run Maven publication
tasks or upload releases.

The publication coordinate is:

`com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:<tag>`

