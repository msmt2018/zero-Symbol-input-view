# Zero Symbol Input View V2

**A grouped symbol-input drawer and extensible editor-action system for Android.**

Zero Symbol Input View V2 is a standalone Android UI library for code editors, text editors, and IDEs. It provides a configurable symbol toolbar with grouped pages, drawer gestures, management and settings screens, persistent data, and a typed action contract. The V2 UI is decoupled from any specific editor implementation, allowing the host application to connect Sora CodeEditor or another editor through an adapter.

> V2 is the primary development and integration path in this repository. The application has migrated to V2. The V1 module remains in the repository for legacy compatibility and data migration; new integrations should target V2.

---

## Contents

- [Project Goals](#project-goals)
- [V2 Features](#v2-features)
- [Architecture](#architecture)
- [Module Structure](#module-structure)
- [Integration](#integration)
- [Data Model and Actions](#data-model-and-actions)
- [Management, Settings, and Backup](#management-settings-and-backup)
- [V1 Data Migration](#v1-data-migration)
- [Build and Test](#build-and-test)
- [Licensing and Third-Party Notices](#licensing-and-third-party-notices)

## Project Goals

V2 is more than a row of shortcut characters at the bottom of an editor. It is a reusable symbol-input subsystem designed to:

- Organize symbols into groups, with one page per group.
- Separate vertical drawer expansion/collapse from horizontal group paging.
- Keep symbol data independent from editor-specific command execution.
- Expose a clear host-adapter boundary for Sora CodeEditor and other editors.
- Provide first-party group/symbol management, runtime settings, persistence, and JSON backup workflows.
- Adapt content layout to phones, tablets, split-screen, and resizable windows.

## V2 Features

### 1. Symbol Drawer and Group Paging

Main view: `SymbolDrawerView`

- Data-driven symbol pages backed by groups.
- Native horizontal paging and page settling through ViewPager2.
- Independent vertical drawer drag, expansion, collapse, and fling settling.
- Gesture-axis arbitration so vertical drawer handling does not steal horizontal page swipes.
- Configurable columns, collapsed row count, cell dimensions, spacing, touch target, and maximum expanded height.
- Optional restoration of the selected group and expanded/collapsed state.
- Short-press and long-press symbol actions, haptics, and optional auto-collapse after an action.
- Group indicator presentation is independent of the underlying group/page model.

### 2. Group Indicators

In V2, a group indicator is the navigation presentation for the group pages. It is not merely a selected-background style applied to symbol tabs.

The indicator communicates the active group, page count, and navigation state. The actual pages remain owned by the separate paging container. Available style families include text tabs, text, pills, dots, underline, elastic/fluid effects, ruler and dial styles, nodes, cards, thumbnail previews, numeric counters, and navigation rails.

### 3. Symbols and Actions

Each symbol has a stable ID, display label, short-press action, and optional long-press action. V2 represents behavior with typed action models rather than forcing every operation into one numeric action ID.

Built-in action models include:

- Insert text, replace selection, and surround selected text.
- Cursor movement and deletion.
- Selection case transformations.
- Editor commands and extension actions.
- Clipboard copy, cut, and paste.
- Composite actions that execute multiple child actions in order.
- Opening the symbol manager.

Actions are executed by a host-provided `SymbolActionHost`. The library does not assume that the host uses Sora and does not own a particular editor instance.

### 4. Symbol Manager

Main screen: `SymbolDrawerManagerScreen`

- Add, rename, duplicate, delete, and reorder groups.
- Add, edit, copy, move, delete, and reorder symbols.
- Batch selection and batch operations.
- Search, favorites, recent items, and filtering.
- Inline preview and editing workflows.
- Data changes are committed through the manager model and repository rather than mutating persistent data directly from UI widgets.

### 5. Settings

Main screen: `SymbolDrawerSettingsScreen`

Settings are organized by category:

| Category | Main options |
|---|---|
| Appearance | System theme adaptation, group indicator, group titles, symbol labels, full symbol labels, text size, action hints |
| Layout | Columns, collapsed rows, cell height, horizontal/vertical spacing, indicator height, maximum expanded height, minimum touch target |
| Large screens | Automatic/bottom/side layout preference and side docking edge |
| Interaction | Long-press timeout, fling threshold, auto-collapse, remembered expansion state, animation and haptics |
| Performance and search | Page cache, adjacent-group prefetch, search action payloads |
| Data | Group/settings JSON import and export |

The **Show full symbol labels** setting keeps titles on one line without ellipsis truncation. The grid adapts its column count and text sizing to the available width and label length.

### 6. Adaptive Windows

V2 separates window information, content arrangement, grid sizing, and drawer geometry. When the window size class changes, the library adjusts internal content width, grid, and spacing without treating tablet or split-screen adaptation as a change to the drawer's interaction model.

The layout system provides compact, medium, and expanded window calculations, together with large-screen side-docking preferences.

### 7. Persistence and Data Safety

- Group data is accessed through the `SymbolGroupRepository` abstraction.
- The default implementation uses a dedicated V2 SharedPreferences store.
- JSON storage is versioned, validated, and canonicalized when read.
- When stored data is malformed, the original JSON and error reason are preserved before recovery can replace the active data.
- Group and symbol IDs are checked for uniqueness.
- Settings use a separate versioned repository.
- JSON backup/restore workflows are provided for group bundles and settings.

---

## Architecture

```text
Host App / Editor
       |
       +-- SymbolDrawerView
       |      |
       |      +-- Drawer interaction & geometry
       |      +-- ViewPager2 group pages
       |      +-- Group indicator renderer
       |      +-- Symbol page/grid adapter
       |
       +-- SymbolActionHost  <---- SymbolActionExecutor
       |                            |
       |                            +-- Typed SymbolAction
       |                            +-- Composite action validation
       |                            +-- Result / cancellation
       |
       +-- SymbolDrawerManagerScreen
       |      |
       |      +-- SymbolGroupManager
       |      +-- SymbolGroupRepository
       |
       +-- SymbolDrawerSettingsScreen
              |
              +-- Settings repository
              +-- JSON import/export

Persistence
  +-- Versioned symbol-group JSON
  +-- Versioned settings JSON
  +-- Legacy V1 migration adapter
```

### Architectural Boundaries

- **View layer** handles rendering, touch dispatch, and page state; it does not execute editor-specific commands.
- **Model layer** uses immutable `SymbolGroup`, `SymbolEntry`, and typed `SymbolAction` models.
- **Repository layer** handles storage, versioned encoding, validation, and migration.
- **Action layer** handles validation, execution order, cancellation, and result reporting.
- **Host layer** adapts editor APIs, extension commands, and navigation supplied by the integrating application.

## Module Structure

V2 is an independent Android Library module:

```text
zero-symbol-input-view-v2/
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/
    │   │   ├── SymbolDrawerView.kt
    │   │   ├── action/       # Action models, registry, executor, host contract
    │   │   ├── adaptive/     # Window and content adaptation
    │   │   ├── data/         # Repository, JSON, defaults, V1 migration
    │   │   ├── drawer/       # Drawer state, gestures, navigation, panels, styles
    │   │   ├── manager/      # Group and symbol management UI
    │   │   ├── model/        # V2 data models
    │   │   ├── settings/     # Settings model, repository, UI, file picker
    │   │   ├── state/        # Drawer geometry and state
    │   │   └── ui/           # Group indicators, symbol pages, host theme colors
    │   └── res/
    ├── src/test/             # JVM unit tests
    └── src/androidTest/      # Android view and interaction tests
```

Main package:

```text
com.zerostudio.idea.widget.editor.symbolinput.v2
```

Main entry points:

- `SymbolDrawerView`
- `SymbolDrawerManagerScreen`
- `SymbolDrawerSettingsScreen`
- `SymbolGroupRepository` / `SharedPreferencesSymbolGroupRepository`
- `SymbolGroupManager`
- `SymbolActionHost` / `SymbolActionExecutor`
- `SymbolActionRegistry`

---

## Integration

### 1. Add the module to a Gradle project

The repository root `settings.gradle.kts` already includes:

```kotlin
include(":zero-symbol-input-view-v2")
```

Add the dependency to the host module:

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

If you copy the V2 module into another Android project, include it in that project's `settings.gradle.kts` and make sure the project has the required repositories configured, including Google Maven and Maven Central.

### 2. Create the drawer

XML:

```xml
<com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
    android:id="@+id/symbolDrawer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Or Kotlin:

```kotlin
val drawer = SymbolDrawerView(context)
container.addView(
    drawer,
    ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
)
```

### 3. Connect the action host

The drawer forwards symbol actions to the host application. This example shows the integration boundary; implement the actual editor operations in your editor adapter:

```kotlin
drawer.onSymbolActionListener =
    SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
        val action = if (isLongPress) {
            symbol.longAction
        } else {
            symbol.shortAction
        } ?: return@OnSymbolActionListener

        // Dispatch through the app's SymbolActionHost / SymbolActionExecutor.
        // Run on the thread required by the editor and handle Unsupported,
        // Failed, and other execution results.
        editorActionAdapter.execute(action)
    }

drawer.onOpenManagerListener = {
    // The host decides how to present the manager screen.
}
```

The host implements the `SymbolActionHost` contract for text insertion, selection replacement, cursor movement, deletion, clipboard operations, editor commands, and extension actions. Return `false` for capabilities that are unavailable; the executor reports an explicit result.

### 4. Provide groups and data

```kotlin
val groups = listOf(
    SymbolGroup(
        id = "common",
        title = "Common",
        symbols = listOf(
            SymbolEntry(
                id = 1L,
                label = "->",
                shortAction = SymbolAction.InsertText("->")
            )
        )
    )
)

drawer.submitGroups(groups)
drawer.saveGroups()

// Select a group by index; the second argument controls smooth scrolling.
drawer.selectGroup(index = 0, smoothScroll = true)

// Expand or collapse.
drawer.setExpanded(expanded = true, animate = true)
```

The default repository is `SharedPreferencesSymbolGroupRepository`. You can implement `SymbolGroupRepository` and pass it to `drawer.setRepository(repository)` to use a custom data layer.

### 5. Add manager and settings screens

Both screens are Views, so the host is not required to use a particular Activity or navigation framework:

```kotlin
val manager = SymbolDrawerManagerScreen(context).apply {
    onGroupsChanged = { updatedGroups ->
        drawer.submitGroups(updatedGroups)
    }
}

val settings = SymbolDrawerSettingsScreen(context).apply {
    onSettingsChanged = { snapshot ->
        drawer.applySettings(snapshot)
    }
}
```

The host can place these views in an Activity, Fragment, Dialog, or its own navigation container. They can be used independently or opened from the drawer's manager/settings entries.

### 6. Import V2 from JitPack

You can consume V2 without cloning this repository or including its module source. Add JitPack to the root `settings.gradle.kts` repository list:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then add the published artifact (replace the version with an existing successful release tag):

```kotlin
dependencies {
    implementation("com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:v0.25.0")
}
```

The complete Maven coordinate, Groovy DSL variant, publication workflow, and release-tag rules are documented in [the V2 module README](zero-symbol-input-view-v2/README.md#2-jitpack-remote-maven-import).

### 7. Extending the existing symbol drawer

The extension API is implemented by the drawer's own `SymbolDrawerExtensionRegistry` and consumed directly by `SymbolDrawerView`, `SymbolGroupIndicator`, `SymbolPageAdapter`, and the existing settings screen. There is no second toolbar control, parallel view hierarchy, or independent toolbar persistence layer.

Public symbol data uses the original `model.legacy.SymbolGroup` / `SymbolItem` contract where legacy symbol records are exposed. Settings use `SymbolDrawerSettings`; runtime registrations are recreated by the host when a drawer session is created and are not serialized into a parallel document.

## Build and Test

Use the Gradle Wrapper from the repository root:

```bash
# Build the V2 release AAR
./gradlew :zero-symbol-input-view-v2:assembleRelease

# Run V2 JVM unit tests
./gradlew :zero-symbol-input-view-v2:test

# Run V2 Android instrumentation tests (requires a connected device or emulator)
./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest

# Build the integrated sample application
./gradlew :app:assembleDebug
```

Instrumentation coverage includes drawer layout, anchors/state, gesture arbitration, group paging, manager UI, settings, data migration, and lifecycle behavior. Use the actual GitHub Actions run for the current device/API matrix status.

---

## Licensing and Third-Party Notices

The SPDX license expression for V2 project-authored code is:

```text
AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
```

The `AND` operator means that all listed license terms apply cumulatively; it does not mean that recipients may choose just one. Read and comply with the applicable full license texts before using, modifying, distributing, or providing network services based on the code. Full texts are included in the V2 module and repository license files.

This repository also contains upstream projects and third-party components. Their code, resources, and dependencies remain subject to their respective original licenses and copyright notices. The V2 license statement does not replace or override upstream terms.

- [GNU AGPL v3.0](https://www.gnu.org/licenses/agpl-3.0.html)
- [GNU GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.html)
- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

[简体中文](README_CN.md)
