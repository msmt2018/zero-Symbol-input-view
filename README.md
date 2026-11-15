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


## Remote Maven SDK (JitPack)

You can consume V2 as a remote Android library without cloning this repository or copying the module into your project. The published artifact is hosted by JitPack.

Add JitPack to the repositories used for dependency resolution (with centralized repositories, configure this in the root `settings.gradle.kts`, not only in the app module):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then add the V2 SDK dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:<published-tag>")
}
```

Replace `<published-tag>` with a Git tag whose JitPack build and artifacts have been verified (for example, `vX.Y.Z`). The version is the tag name, including its leading `v`; do not use an unpublished branch name as a release version. The public artifact requires no Maven credentials or GitHub token. The exact coordinates and full remote/local integration instructions are documented in the [V2 module guide](zero-symbol-input-view-v2/README.md#2-jitpack-remote-maven-import).

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

## JSON data and interoperability

The symbol data model is designed for exchange between the host editor, external symbol managers and backup files—not just for use inside this View.

- **V1 compact JSON:** supports the legacy symbol-toolbar structure. The top-level value is a group array; groups use `n` (name) and `d` (entries), while entries use `a/b/c/d/e` for short-action ID, display text, short-action payload, long-action ID and long-action payload.
- **V2 full bundle:** a versioned JSON envelope that preserves stable group/symbol IDs, icons and typed actions for complete backups and V2-to-V2 exchange.
- **Compatibility boundary:** V1 can represent only actions in the legacy numeric action catalog. Composite, some Selection and arbitrary Extension actions cannot be losslessly represented. Export must reject unsupported data rather than silently dropping it.
- **Host-defined actions:** persist a stable action ID and payload; runtime handlers/lambdas are registered by the host and are not serialized.

Legacy compact JSON example:

```json
[
  {
    "n": "Common",
    "d": [
      { "a": 0, "b": "Brackets", "c": "()", "d": 0, "e": "[]" }
    ]
  }
]
```

See [models and actions](zero-symbol-input-view-v2/api-docs/03-models-and-actions.md) and [repository, management and migration](zero-symbol-input-view-v2/api-docs/05-data-and-management.md) for field definitions, validation, export failure semantics and the full V2 bundle API. MT Manager interoperability is limited to the documented compact structure; other variants require a real sample before compatibility can be claimed.

## Integration and API documentation

- [V2 module integration guide](zero-symbol-input-view-v2/README.md): local module, JitPack, Activity/Fragment, editor adapters, management/settings and JSON exchange.
- [API documentation index](zero-symbol-input-view-v2/api-docs/README.md): public APIs, models, action runtime, drawer interaction and extension framework.
- [Extension framework](zero-symbol-input-view-v2/api-docs/12-extension-framework.md) and [symbol-control extension guide](zero-symbol-input-view-v2/api-docs/15-symbol-control-extension-api.md): extend the existing control instead of creating a parallel implementation.
- [Testing, release and troubleshooting](zero-symbol-input-view-v2/api-docs/16-testing-release-troubleshooting.md): repository-maintainer engineering reference.

## Compatibility and integration boundaries

- V2 core does not import a specific editor engine.
- Host adapters implement the capabilities they support; unsupported operations must return false rather than pretending success.
- Composite actions validate the full action tree before execution, but they are not automatically transactional. Atomic rollback requires host transaction support.
- The generic `DrawerContainer` and symbol-specific `SymbolDrawerView` have distinct roles and configuration types.
- Kotlin typealias facade availability does not imply Java ABI compatibility.
- Window Insets, foldables, nested scrolling, touch arbitration and accessibility must be validated in the target host layout/device matrix.

## License

### License identifier

Original V2 project source files are marked with the following SPDX expression:

```text
SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
```

The SPDX operator `AND` is intentional: the three listed licenses apply cumulatively to the same covered work. It does not mean that a recipient may select only one of them. Recipients must satisfy the applicable conditions of all three licenses when exercising rights in covered material.

### What the licenses require

1. **GNU Affero General Public License v3.0 only (AGPL-3.0-only).** Provides rights to use, study, modify and redistribute the covered work. In particular, when a modified version is used to provide users access to it over a computer network, AGPLv3 section 13 requires offering those users the Corresponding Source of that modified version under the AGPL terms.
2. **GNU General Public License v3.0 only (GPL-3.0-only).** Provides rights to use, study, modify and redistribute the covered work. Conveying object code triggers the applicable GPLv3 requirements, including providing Corresponding Source or a valid offer, preserving notices, and complying with the license conditions.
3. **Apache License 2.0 (Apache-2.0).** Grants copyright and applicable patent licenses, subject to its conditions, including providing a copy of the license, retaining relevant copyright/patent/trademark/attribution notices, marking modified files, and respecting its patent-termination provision.

This section is a practical summary, not legal advice and not a substitute for the complete license texts. If the cumulative licensing expression is not intended, obtain legal review and change the SPDX expression consistently before release.

### Complete license texts

- [GNU AGPL v3.0](zero-symbol-input-view-v2/LICENSE-AGPL-3.0-only.txt) · [Official text](https://www.gnu.org/licenses/agpl-3.0.html)
- [GNU GPL v3.0](zero-symbol-input-view-v2/LICENSE-GPL-3.0-only.txt) · [Official text](https://www.gnu.org/licenses/gpl-3.0.html)
- [Apache License 2.0](zero-symbol-input-view-v2/LICENSE-Apache-2.0.txt) · [Official text](https://www.apache.org/licenses/LICENSE-2.0)

### Scope and third-party material

This licensing notice applies to original V2 project-authored files in `zero-symbol-input-view-v2/` that carry the SPDX identifier above, and to contributions whose copyright holders have authorized this licensing. It does not relicense the entire repository, the legacy V1 module, third-party dependencies, vendored/imported code, or files carrying separate upstream notices.

The repository-root `LICENSE` remains applicable to repository material outside the specifically identified V2 files. Preserve all third-party copyright and license notices. A file-level notice does not override a more specific upstream license.

**Copyright (C) 2026 android_zero**  
Author: android_zero · [GitHub](https://github.com/msmt2018) · WeChat: `android_zero`
