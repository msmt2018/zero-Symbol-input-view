# Zero Symbol Input View V2

<p align="center">

**A reusable symbol-input drawer and editor-neutral action framework for Android.**

[简体中文](README_CN.md) · [API Documentation](zero-symbol-input-view-v2/api-docs/README.md) · [V2 Module Guide](zero-symbol-input-view-v2/README.md)

</p>

---

## Overview

Zero Symbol Input View V2 is an Android library for code editors, text editors and IDEs. It provides a real grouped symbol drawer—not just a static row of shortcut buttons—with independent vertical drawer interaction, horizontal group paging, symbol management, settings, persistence and an extensible action system.

V2 is the primary integration path in this repository. The legacy V1 module remains for compatibility and data migration. New integrations should use V2.

### What it provides

- **Grouped symbol pages:** each symbol group owns a page; horizontal paging is independent from vertical drawer expansion.
- **Real drawer interaction:** drag, fling, settle, collapsed/expanded states and gesture-axis arbitration.
- **Configurable grid:** columns, visible collapsed rows, cell dimensions, spacing, touch targets and expanded height.
- **Group navigation:** replaceable navigation indicator presentation while the original ViewPager2 remains the single page-state owner.
- **Editor-neutral actions:** typed actions for insertion, selection, cursor, deletion, clipboard, editor commands and extensions.
- **Management and settings:** groups, symbols, favorites, recent items, batch operations, appearance, layout, interaction and data backup.
- **Extensibility:** action handlers, custom cell renderers, group indicators, settings tabs/rows, themes, legacy group providers and lifecycle hooks.
- **Adaptive layout:** compact/medium/expanded window handling and large-screen docking options.
- **Data safety:** versioned JSON, validation, canonicalization, recovery handling and explicit V1 import/migration.

## Architecture

```text
Host Application
├── Editor / Text Document
├── Editor Adapter
│   ├── SymbolActionHost
│   └── SymbolTextDocument (optional)
└── Zero Symbol Input View V2
    ├── SymbolDrawerView
    │   ├── Drawer interaction + geometry
    │   ├── ViewPager2 group pages
    │   ├── Group indicator slot
    │   └── SymbolPageAdapter / grid
    ├── SymbolAction + SymbolActionExecutor
    ├── SymbolDrawerExtensionRegistry
    ├── Manager + Settings screens
    └── Repository + JSON codecs
```

**The library does not depend on a particular editor core.** The host supplies an adapter for the currently focused document. Sora, a custom editor, EditText or another editor can use the same V2 library without adding editor-specific dependencies to the V2 core.

The repository also contains a separate generic Drawer framework. It is not a replacement symbol toolbar: `DrawerContainer` is a function-neutral container, while `SymbolDrawerView` retains its own symbol grid, ViewPager2 and symbol interaction pipeline.

## Quick integration

Add the module:

```kotlin
// settings.gradle.kts
include(":zero-symbol-input-view-v2")

// app/build.gradle.kts
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

Add the view to a layout:

```xml
<com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
    android:id="@+id/symbolDrawer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Connect the host action adapter:

```kotlin
val actionExecutor = SymbolActionExecutor()
val actionHost: SymbolActionHost = createMyEditorActionHost()

drawer.onSymbolActionListener =
    SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
        val action = if (isLongPress) symbol.longAction else symbol.shortAction
        if (action != null) {
            val result = drawer.executeAction(action, actionHost)
            handleSymbolActionResult(result)
        }
    }

drawer.onOpenManagerListener = { openManagerScreen() }
drawer.onOpenSettingsListener = { openSettingsScreen() }
drawer.reloadGroups()
```

The sample adapter factory and editor implementation are host-owned. The action API is synchronous: call it on the thread required by your editor.

## Extension API

Extensions attach to the **existing** drawer instance. They do not create a second toolbar, page model, gesture recognizer or persistence layer.

```kotlin
val registration = drawer.extensions.registerAction(
    SymbolDrawerActionExtension(
        id = "com.example.editor.format",
        title = "Format selection",
        description = "Format the current selection",
        handler = SymbolActionHandler { host, payload ->
            host.executeEditorCommand("com.example.editor.format", payload)
        }
    )
)

// Persist SymbolAction.Extension(id, payload), not the callback.
// Close registration with its owning editor/Fragment session.
```

Available extension families:

| Extension | Purpose |
|---|---|
| Actions | Register runtime handlers addressable by stable ID + payload |
| Item view renderer | Customize cells through the existing SymbolPageAdapter |
| Group indicator | Replace the real group navigation indicator |
| Settings tab / row | Add settings UI to the existing settings screen |
| Theme | Provide ARGB theme tokens to connected drawer surfaces |
| Legacy group provider | Feed existing V1 model records through the existing codec |
| Lifecycle observer | Observe attach, settings, groups, selection and symbol use |
| Built-in visibility | Hide/show supported built-in entry points |

All registrations return `AutoCloseable`. The host must release handles with the owning Fragment View or editor session. Registered callbacks are runtime-only and must be registered again after process recreation.

## Public API package direction

New Kotlin integrations should prefer the generic drawer namespace:

```kotlin
com.zerostudio.idea.widget.drawer
com.zerostudio.idea.widget.drawer.core
com.zerostudio.idea.widget.drawer.navigation
com.zerostudio.idea.widget.drawer.panel
com.zerostudio.idea.widget.drawer.style
com.zerostudio.idea.widget.drawer.adapter
com.zerostudio.idea.widget.drawer.extension
com.zerostudio.idea.widget.drawer.action
```

The symbol-input product APIs remain under:

```kotlin
com.zerostudio.idea.widget.editor.symbolinput.v2
```

Some new public package entry points are currently Kotlin typealiases to existing implementation types. They are Kotlin source-level facades, not separate Java-visible classes or a completed physical relocation. See [public package and compatibility notes](zero-symbol-input-view-v2/api-docs/13-public-package-and-editor-neutral-actions.md).

## Documentation

| Guide | Description |
|---|---|
| [V2 Module README](zero-symbol-input-view-v2/README.md) | Dependency setup, JitPack, integration and migration |
| [API documentation index](zero-symbol-input-view-v2/api-docs/README.md) | Full API reference and navigation |
| [SymbolDrawerView API](zero-symbol-input-view-v2/api-docs/02-symbol-drawer-view.md) | Symbol control methods, callbacks and state |
| [Action runtime](zero-symbol-input-view-v2/api-docs/04-action-runtime.md) | Host, executor, registry, result and cancellation |
| [Extension framework](zero-symbol-input-view-v2/api-docs/12-extension-framework.md) | Registry and extension contracts |
| [Public Drawer API reference](zero-symbol-input-view-v2/api-docs/14-drawer-public-api-reference.md) | Generic Drawer public API |
| [Symbol control extension guide](zero-symbol-input-view-v2/api-docs/15-symbol-control-extension-api.md) | End-to-end extension development |
| [Adaptive window API](zero-symbol-input-view-v2/api-docs/09-adaptive-window.md) | Window and large-screen adaptation |

## Build and test

```bash
./gradlew :zero-symbol-input-view-v2:assembleRelease
./gradlew :zero-symbol-input-view-v2:test
./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Instrumentation tests require a connected device or emulator. Verify current CI workflow results before treating a commit as release-ready.

## Compatibility and integration boundaries

- V2 core does not import a specific editor engine.
- Host adapters implement the capabilities they support; unsupported operations must return false rather than pretending success.
- Composite actions validate the full action tree before execution, but they are not automatically transactional. Atomic rollback requires host transaction support.
- The generic `DrawerContainer` and symbol-specific `SymbolDrawerView` have distinct roles and configuration types.
- Kotlin typealias facade availability does not imply Java ABI compatibility.
- Window Insets, foldables, nested scrolling, touch arbitration and accessibility must be validated in the target host layout/device matrix.

## Licensing

V2 project-authored source declares:

```text
AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
```

The SPDX `AND` means the terms apply cumulatively. It is not an option to select only one license. Read the full license texts and third-party notices before using, modifying or redistributing the project.
