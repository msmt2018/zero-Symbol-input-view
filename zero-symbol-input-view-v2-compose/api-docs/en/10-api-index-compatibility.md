# 10 — Compose API index, package layout and compatibility

This page indexes the current `zero-symbol-input-view-v2-compose` artifact. Generic Drawer source is under `com.zerostudio.idea.widget.drawer`; symbol UI remains under `com.zerostudio.idea.widget.editor.symbolinput.v2`.

## 1. Compose UI entry points

| API | Package | Purpose |
|---|---|---|
| `SymbolDrawer` | `editor.symbolinput.v2` | Compose symbol drawer |
| `SymbolDrawerState`, `rememberSymbolDrawerState` | `editor.symbolinput.v2` | Composition-owned presentation state |
| `SymbolDrawerManagerScreen` | `editor.symbolinput.v2.manager` | Compose manager screen |
| `SymbolDrawerSettingsScreen` | `editor.symbolinput.v2.settings` | Compose settings screen |
| `SymbolGroupIndicator`, `SymbolPage` | `editor.symbolinput.v2.ui` | Compose UI components |

This artifact does not expose `SymbolDrawerView`.

## 2. Generic Compose Drawer

| API | Package | Purpose |
|---|---|---|
| `DrawerContainer` | `com.zerostudio.idea.widget.drawer` | Composable generic drawer |
| `DrawerContainerState`, `rememberDrawerContainerState` | same | Remembered container state |
| `DrawerConfig`, `DrawerPresentationMode` | `drawer.config` | Generic drawer configuration |
| `DrawerStateHolder`, `DrawerController` | `drawer.core` | State engine and controller |
| `DrawerAnchor`, `DrawerAnchorInput`, `DrawerAnchorStrategy` | `drawer.core` | Anchor models and policies |
| `DrawerNestedScrollPolicy`, `DrawerNestedScrollConnection` | `drawer.core` | Nested-scroll arbitration |
| `rememberDrawerStateHolder`, `rememberDefaultDrawerController`, `rememberDrawerStateSnapshot` | `drawer.core` | Composition adapters |
| `DrawerPage`, `DrawerPageHost` | `drawer.navigation` | Stable-ID pages and Composable host |
| `DrawerPanelContent`, `drawerPanelContent`, `DrawerPanelHost` | `drawer.panel` | Composable panel slots |
| `DrawerStyle`, `DrawerColors`, `DrawerDimensions`, `DrawerMotion` | `drawer.style` | Compose style tokens |

## 3. Domain, data and actions

- Models: `SymbolGroup`, `SymbolEntry`, `SymbolAction`, product `model.DrawerConfig`.
- Actions: `SymbolActionHost`, `SymbolActionExecutor`, `SymbolActionResult`, `SymbolActionRegistry`.
- Text document: `SymbolTextDocument`, `SymbolTextDocumentActionHost`, `SymbolTextEditing`.
- Data: `SymbolGroupRepository`, `ObservableSymbolGroupRepository`, `SymbolGroupManager`.
- Codecs/migration: `SymbolGroupStorageCodec`, `SymbolGroupBundleCodec`, `LegacySymbolDataCodec`, `LegacySymbolDataMigration`, Android migration adapter.
- Settings: `SymbolDrawerSettings`, `SymbolDrawerSettingsRepository`, `SymbolDrawerSettingsCodec`.

## 4. Extensions

`SymbolDrawerExtensionRegistry` registers action, legacy action, indicator, item renderer, legacy group provider, settings tab/item and theme contributions. Each registration returns an `AutoCloseable`. Contributions are runtime objects, not serialized data. Registration does not imply that every UI screen automatically consumes every extension; use the consumer behavior documented for the current screen.

## 5. Compatibility boundaries

- Generic Drawer source packages are physically located under `com.zerostudio.idea.widget.drawer`.
- Symbol-input UI and domain packages remain under `editor.symbolinput.v2`.
- Kotlin `typealias` does not emit an independent Java class.
- Compose `DrawerContainer` is a Composable; the legacy artifact's same-concept API is an Android View class.
- The two UI artifacts are not interchangeable and should not be added together.

## 6. Composition semantics

The API does not require Activity, Fragment, ViewModel or LifecycleOwner as a component state container. Composition, recomposition, remember scopes and effect cancellation still exist. Do not describe Compose as having no lifecycle at all.
