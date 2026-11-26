# 17 — Complete Compose API Developer Reference (KDoc / JavaDoc)

> This reference is derived from the current production sources under `zero-symbol-input-view-v2-compose/src/main`. Kotlin source is authoritative for signatures, visibility and defaults. Internal implementation details are not promoted to stable public contracts.

## 1. Documentation conventions

Public Kotlin declarations should use KDoc so Dokka can consume them. Java interoperability notes use JavaDoc terminology. Document purpose, ownership, input constraints and side effects. Add `@param`, `@return`, `@throws` and `@see` where applicable; do not add meaningless tags.

```kotlin
/**
 * Creates a composition-owned state holder for the symbol drawer.
 *
 * The state survives recomposition while its call site retains the same
 * Composition identity. It is not a ViewModel or persistence mechanism.
 *
 * @param initialPage zero-based initial page; negative values normalize to zero.
 * @param initiallyExpanded whether the first resolved geometry starts expanded.
 * @return state remembered by the current Composition.
 */
@Composable
fun rememberSymbolDrawerState(
    initialPage: Int = 0,
    initiallyExpanded: Boolean = false
): SymbolDrawerState
```

Distinguish public API, internal implementation, Kotlin-only typealiases (no standalone JVM/Java class), and Composable slots (not View factories).

## 2. Module and package map

| Layer | Canonical package | Main API/source responsibility |
|---|---|---|
| Symbol drawer UI | `com.zerostudio.idea.widget.editor.symbolinput.v2` | `SymbolDrawer`, `SymbolDrawerState`, remember factory, drag policy |
| Generic Drawer facade | `com.zerostudio.idea.widget.drawer` | `DrawerContainer`, state and Kotlin facade aliases |
| Drawer configuration | `drawer.config` | `DrawerConfig`, `DrawerPresentationMode` |
| Drawer state/control | `drawer.core` | anchors, state holder, controller, snapshots, drag and nested-scroll policies |
| Drawer navigation | `drawer.navigation` | page model, navigation config/controller/state engine and page host |
| Drawer panels | `drawer.panel` | Composable panel slots and host |
| Drawer styling | `drawer.style` | color, dimension, typography, motion, shape and theme tokens |
| Drawer presets | `drawer.preset` | reusable presets |
| Drawer extensions/theme | `drawer.extension` | extension API and symbol drawer theme contracts |
| Editor-neutral actions | `drawer.action` | document, editing, host, registry and execution engine |
| Symbol models | `editor.symbolinput.v2.model` | groups, entries, typed actions and symbol-grid config |
| Symbol actions | `editor.symbolinput.v2.action` | built-ins, JSON codec and executor/host aliases |
| Data/migration | `editor.symbolinput.v2.data` | repositories, manager, codecs and legacy migration |
| Settings | `editor.symbolinput.v2.settings` | settings snapshot/repository/codec, Compose screen and JSON IO |
| Adaptive layout | `editor.symbolinput.v2.adaptive` | window snapshots, dimension/grid/layout resolvers and docking |
| Manager | `editor.symbolinput.v2.manager` | Compose manager screen, action editor and usage state |
| UI helpers | `editor.symbolinput.v2.ui` | group indicator, symbol page, page adapter, theme colors/defaults |
| Geometry | `editor.symbolinput.v2.state` | drawer geometry model |

## 3. Symbol drawer API

### `SymbolDrawerState`

Composition-remembered state. Public members:
- `selectedPage: Int`: zero-based selected page; mutation is requested through `selectPage`.
- `expanded: Boolean`: derived from the current anchor/height snapshot.
- `expand()`, `collapse()`, `toggle()`: request anchor transitions.
- `selectPage(index: Int)`: selects a page; negative values normalize to zero.

It does not retain Activity, Context, View, LifecycleOwner, Adapter or host callbacks. It is not process-death persistence.

### `rememberSymbolDrawerState`

Composable factory. `initialPage` is zero-based; `initiallyExpanded` selects the initial anchor after geometry is resolved. State identity follows the call site's Composition identity.

### `SymbolDrawer`

Composable symbol-input surface.

| Parameter | Contract |
|---|---|
| `groups` | Current immutable group snapshot; IDs should be stable |
| `modifier` | Host-owned Compose modifier |
| `state` | Drawer state; remembered at the call site by default |
| `settings` | Immutable settings snapshot |
| `config` | Layout configuration, defaulted from settings |
| `onSymbolClick` | Required short-click callback |
| `onSymbolLongClick` | Optional long-click callback |
| `onOpenManager` | Optional manager-navigation request |
| `onOpenSettings` | Optional settings-navigation request |
| `usageState` | Optional usage-frequency state |

The composable renders data and emits callbacks. Do not perform blocking file IO, synchronous network requests or persistence writes in its body. The host owns manager/settings navigation.

## 4. Domain models and actions

- `SymbolGroup(id, title, symbols)`: stable group identity, title and entries.
- `SymbolEntry`: stable numeric ID, label, typed short/long actions and optional icon; legacy action fields remain for lossless V1 JSON migration.
- `SymbolAction`: sealed action family: `InsertText`, `ReplaceSelection`, `SurroundSelection`, `MoveCursor`, `Delete`, `TransformSelection`, `EditorCommand`, `Clipboard`, `OpenManager`, `Composite`, and `Extension`.
- `CursorTarget`, `DeleteKind`, `SelectionTransform`, `ClipboardOperation`: action parameter enums.
- `LegacySymbolAction`: migration-only representation of the old action schema.
- `model.DrawerConfig`: symbol grid/drawer dimensions. It is distinct from generic `drawer.config.DrawerConfig`.

The editor-neutral `drawer.action` package defines:
- `SymbolTextDocument`: host document contract. Offsets are UTF-16 code units, line/column are zero-based, and ranges are end-exclusive.
- `SymbolActionHost`: maps typed actions to editor capabilities.
- `SymbolActionExecutor` / `ActionExecutionEngine`: interprets actions synchronously on the calling thread.
- `SymbolActionResult`: distinguishes completion, unsupported/rejected operations, cancellation, invalid input and execution failure.
- `SymbolActionRegistry`: stable-ID handler registration; callers own and close registration handles.
- `SymbolTextDocumentActionHost` / `SymbolTextEditing`: document-based editing helpers.

The host owns thread switching. Composite actions execute sequentially and must not be assumed transactional unless the document adapter implements real rollback.

## 5. Generic Compose Drawer

### State and factory

`DrawerContainerState` exposes `controller`, `expanded`, `expand()`, `collapse()` and `toggle()`. Reuse one state instance for the drawer it owns; do not mirror it with another authoritative state machine.

`rememberDrawerContainerState(initiallyExpanded)` creates Composition-owned state. It is not SavedStateHandle or database persistence.

### `DrawerContainer`

Actual Composable contract:

```kotlin
@Composable
fun DrawerContainer(
    config: DrawerConfig = DrawerConfig(),
    modifier: Modifier = Modifier,
    state: DrawerContainerState = rememberDrawerContainerState(),
    title: String? = null,
    pages: List<DrawerPage> = emptyList(),
    selectedPageId: String? = null,
    onPageSelected: (DrawerPage) -> Unit = {},
    onDismiss: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
)
```

- `config`: anchors, style, panel slots, drag/nested-scroll, dismissal, accessibility, navigation and presentation mode.
- `modifier`: host layout modifiers.
- `state`: container state.
- `title`: optional title.
- `pages`: navigation pages with stable IDs.
- `selectedPageId`: externally selected page ID; null means no external selection.
- `onPageSelected`: emitted when a page is selected.
- `onDismiss`: emitted when dismissal is requested.
- `content`: host content slot with `BoxScope`.

The container is constrained by its parent Compose layout. It does not create an Activity/Window or wrap legacy Views.

### Configuration, anchors, controllers and snapshots

- `drawer.config.DrawerConfig`: generic container configuration and chainable `Builder`.
- `DrawerPresentationMode`: `BOTTOM_SHEET` or `FULL_SCREEN`.
- `DrawerAnchor` / `DrawerAnchorInput`: anchor and geometry models.
- `DrawerAnchorStrategy` and resolvers/presets: derive anchors from available space and content.
- `DrawerState`: immutable snapshot of anchor, height, drag/animation and target state.
- `DrawerStateHolder`: single mutable geometry engine and snapshot publisher.
- `DrawerController` / `DefaultDrawerController`: host-facing operation facade.
- `DrawerNestedScrollConnection` / `DrawerNestedScrollPolicy`: Compose nested-scroll arbitration.
- `rememberDrawerStateHolder`, `rememberDefaultDrawerController`, `rememberDrawerStateSnapshot`: Composition adapters.
- `SynchronousSnapshotPublisher`: snapshot publication infrastructure, not a replacement UI state holder.

Core anchor/controller code does not render UI. The Composable layer observes snapshots and owns animation/effect jobs. Avoid duplicate authoritative height/anchor state in the host.

## 6. Pages, panels, style and presets

- `DrawerPage(id, title, content, icon)`: nonblank stable ID, title, panel content and optional Composable icon.
- `DrawerPageDescriptor`: navigation metadata.
- `DrawerPageHost`: synchronizes stable page IDs with pager selection.
- `DrawerNavigationConfig`, `DrawerNavigationController`, `DrawerNavigationStateEngine`: navigation configuration and state.
- `DrawerNavigationPlacement` / `DrawerIndicatorStyle`: navigation placement and indicator style.
- `DrawerPanelContent`: `ColumnScope.() -> Unit` Composable slot; create it with `drawerPanelContent { }`.
- `DrawerPanelSlot`: peek/expanded slot identity; `DrawerPanelHost` renders the slot.
- `DrawerStyle`: color, dimension, typography, motion, shape, elevation, scrim and clipping.
- `DrawerColors`, `DrawerColorAttributes`, `DrawerDimensions`, `DrawerTypography`, `DrawerMotion`, `DrawerShape`: immutable style tokens.
- `DrawerThemeResolver`: resolves theme tokens.
- `DrawerPreset` / `DrawerPresets`: reusable presets, not mandatory global theme.
- `SymbolDrawerTheme` / `SymbolDrawerThemeExtension`: symbol drawer theme extension contract.

Composable slots have no View attach/detach or manual disposal contract.

## 7. Repositories, management, JSON and migration

- `SymbolGroupRepository`: `loadGroups` / `saveGroups` persistence contract.
- `ObservableSymbolGroupRepository`: adds `observeGroups(listener)`; close the returned `AutoCloseable`.
- `SymbolGroupManager`: validated CRUD, copy/move/reorder and batch operations above the repository.
- `SymbolGroupStorageCodec`: storage encoding/decoding.
- `SymbolGroupBundleCodec`: complete bundle import/export and validation.
- `LegacySymbolDataCodec`, `LegacySymbolActionAdapter`, `LegacySymbolActionCatalog`, `LegacySymbolActionDefinitions`: V1 JSON/action adapters.
- `LegacySymbolDataMigration` / `AndroidLegacySymbolDataMigration`: migration logic and Android storage adapter.
- `SharedPreferencesSymbolGroupRepository` / `SharedPreferencesSymbolUsageRepository`: Android preference implementations.
- `SymbolDrawerDefaultGroups` / `SymbolDrawerDefaults`: default data, not persistence.

Decode and validate before committing repository state. After a successful write, publish a new snapshot so Compose reads the current data.

## 8. Settings API

`SymbolDrawerSettings` is an immutable validated snapshot covering theme, grid, spacing, indicator, full labels, touch targets, motion, paging cache, search, haptics and large-screen docking.

- `ThemeMode`: SYSTEM / LIGHT / DARK.
- `GroupIndicatorStyle`: indicator renderer selection.
- `HapticMode`: OFF / SELECTION / ALL.
- `LargeScreenLayoutMode`: AUTO / BOTTOM / SIDE.
- `SideDockEdge`: LEFT / RIGHT.
- `SymbolDrawerSettingsRepository`: `load`, `save`, `reset`, `hasStoredSettings`.
- `SettingsLoadResult`: snapshot and invalid-data recovery flag.
- `SymbolDrawerSettingsCodec.encode/decode`: compatibility decoding with defaults for missing fields.
- `SymbolDrawerSettingsCodec.decodeStrict`: external backup import; validates schema version, exact field set and field types.
- `SharedPreferencesSymbolDrawerSettingsRepository`: Android preference implementation.
- `SymbolDrawerSettingsScreen`: Compose settings UI using snapshots and update callbacks.
- `JsonDocumentIO` / `JsonDocumentPicker`: JSON document and host file-picker integration.

Current settings schema version is 3. Strict external import and tolerant local recovery are separate contracts.

## 9. Adaptive layout and UI helpers

- `WindowAdaptiveInfo`: available window dimensions and density; do not infer split-screen layout from physical display metrics.
- `AdaptiveDimensions`: resolved dimensions.
- `AdaptiveGridResolver`: resolves grid columns and spacing from constraints and minimum cell width.
- `AdaptiveLayoutResolver` / `AdaptiveContentLayout`: layout mode and content sizing.
- `AdaptiveWindowController` / `AdaptiveWindowObserver` / `AdaptiveWindowComposition`: window observation and Composition integration.
- `ConcurrentSnapshotPublisher`: concurrent snapshot publication.
- `SymbolDrawerDockingPolicy` / `SymbolDrawerDockingLayoutResolver` / `SymbolDrawerDockingCoordinator`: bottom/side docking policy and coordination.
- `SymbolGroupIndicator` / `SymbolPage`: Compose UI components.
- `SymbolPageAdapter`: page/grid content adapter.
- `ComposeSymbolDrawerDefaults` / `HostThemeColors`: defaults and host-theme adaptation.
- `DrawerGeometry`: symbol drawer geometry model.

Resolvers should use actual parent constraints and must not depend on Activity or Window mutation.

## 10. Extension registry

`SymbolDrawerExtensionRegistry` exposes snapshots/revision, built-in visibility and registration for actions, legacy actions, indicators, item renderers, group providers, settings tabs/items and themes. Every registration returns `AutoCloseable`; the caller owns and closes it when its session/Composition ends.

Extension contracts include:
- `SymbolDrawerActionExtension`: stable ID, title, description, payload hint, priority and handler.
- `SymbolDrawerLegacyGroupProvider`: legacy group provider.
- `SymbolDrawerItemRenderer`: accepts entries and provides Composable content.
- `SymbolGroupIndicatorExtension`: receives groups, selected index and selection callback.
- `SymbolDrawerSettingsTabExtension` / `SymbolDrawerSettingExtension`: settings tab/item content.
- `SymbolDrawerSettingsScope`: current snapshot and `update(transform)`.
- `SymbolDrawerExtensionSnapshot`: immutable registry snapshot.

**Registration does not imply automatic rendering.** Whether a page consumes a contribution must be verified in that page's current source. Do not retain Activity/View/Context in extensions or register once-only side effects from a Composable body.

## 11. Errors, threading and state ownership

1. Each Compose state has one owner; do not create competing authoritative state machines.
2. The host/data layer schedules blocking repository or codec IO; the library does not choose every host Dispatcher.
3. Action execution is synchronous on the calling thread; host adapters own thread switching.
4. Validate imported data completely before persistence.
5. Callbacks are event/request boundaries; components do not navigate or persist on behalf of the host.
6. Close every `AutoCloseable` registration and observer handle.
7. Invalid ranges/schema may raise `IllegalArgumentException` or `require` failures; missing targets/conflicts may raise explicit exceptions. Exact conditions follow each implementation.

## 12. Lifecycle and platform boundary

The module does not require Activity, Fragment, ViewModel or LifecycleOwner as its UI state contract. It does not create Android Views or wrap legacy controls with AndroidView. It still runs in Compose Runtime and therefore has Composition identity, recomposition, remember scopes and effect cancellation. “No legacy View/LifecycleOwner coupling” does not mean “no lifecycle semantics”.

## 13. Compatibility policy

- Public package names, declarations, defaults, serialized fields and stable IDs are compatibility-sensitive.
- New optional parameters should have safe defaults; semantic changes require synchronized Chinese/English docs and examples.
- Kotlin typealias migration is not a Java binary-class migration.
- Legacy codecs/adapters preserve V1 JSON fields and numeric action IDs.
- Generic `drawer.config.DrawerConfig` and symbol `model.DrawerConfig` are distinct types.
- The View and Compose artifacts are not interchangeable.

## 14. Documentation audit checklist

- [ ] KDoc exists for each public class/interface/enum/object.
- [ ] Public functions describe parameters, return values, exceptions and side effects where applicable.
- [ ] Public properties describe units, nullability, mutability and state owner.
- [ ] Composables describe slots, callback timing and Composition behavior.
- [ ] Codecs describe tolerant/strict mode, schema version and error policy.
- [ ] Repositories describe persistence, observation and threading boundaries.
- [ ] Extensions describe registration-handle lifetime and actual UI consumption.
- [ ] Chinese and English API references/import examples stay synchronized.
- [ ] Dokka output excludes unintended internal/private implementation details.
