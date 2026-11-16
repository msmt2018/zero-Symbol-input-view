# Compose Library — Full Native Refactor Plan

## Objective and non-negotiable boundary

`zero-symbol-input-view-v2-compose` is to become a genuinely independent, fully reimplemented Jetpack Compose library. This is not a View-to-Compose wrapper migration. All UI surfaces, layout/measurement, gestures, paging, rendering, manager/settings workflows, dialogs, accessibility, theme integration, state holders and extension presentation APIs are to be designed and implemented with Compose UI, Foundation, Material 3 and Animation APIs.

The sibling `zero-symbol-input-view-v2` module is immutable throughout this work. No source, resources, Gradle files, tests or documentation in that module may be changed. The Compose artifact must not depend on that module or on View-only UI artifacts. No `AndroidView`, `AndroidViewBinding`, legacy widget fallback, or public API accepting/returning View types is permitted.

Allowed reuse is limited to UI-independent behavior: data models, JSON schemas/codecs, validation, stable IDs, persistence formats, editor-neutral action contracts, and pure Kotlin algorithms. Where an existing contract mixes Android platform or View APIs into its domain, it must be separated before reuse.

## Source audit at baseline HEAD

Audited branch: `feat/compose-symbol-drawer-v2`  
Baseline: `703118357a4b9daa0598d6cf38c497f844104521`  
Compose module inventory: **207 tracked files** (including docs, Gradle, resources, main/test/androidTest sources and licenses).

### Main source inventory and migration disposition

| Existing area | Baseline finding | Required disposition |
|---|---|---|
| `SymbolDrawerView.kt` | 52 KB ViewGroup integrating gesture, height, ViewPager2, indicator, settings and lifecycle | Delete after behavior/state migration; replace with Compose public entry point and state/controller |
| `ui/SymbolGroupIndicator.kt` | View/Canvas/Paint/Animator/child-widget implementation | Reimplement every indicator style as Compose renderer; then delete |
| `ui/SymbolPageAdapter.kt` | RecyclerView/ViewPager2 adapters and grid implementation | Replace with Foundation pager + LazyVerticalGrid; delete |
| `manager/SymbolDrawerManagerScreen.kt` | ~90 KB legacy manager screen | Rebuild manager workflows using Compose screen/state/components; delete |
| `settings/SymbolDrawerSettingsScreen.kt` | ~45 KB legacy settings UI | Rebuild all settings categories and controls in Material 3 Compose; delete |
| `extension/SymbolDrawerExtensionRegistry.kt` | Legacy extension contracts import View, MotionEvent, Context and legacy UI models | Extract platform-neutral action/lifecycle contracts where valid; replace presentation extensions with Compose slots/renderers; delete View-specific registry |
| `drawer/DrawerContainer.kt` | ~51 KB legacy generic drawer implementation; requires full source-level classification before reuse | Retain only pure policies/contracts; replace UI/controller integration with Compose-native drawer surface |
| `settings/JsonDocumentPicker.kt` | ActivityResultRegistry/Context-based picker bridge | Replace with Compose route-owned `rememberLauncherForActivityResult`; keep bounded stream I/O as a platform adapter |
| `data/SymbolGroupRepository.kt` | Repository interfaces and SharedPreferences implementation in one file | Split platform-neutral repository contracts from Android storage adapter |
| `settings/SymbolDrawerSettingsRepository.kt` | Repository contract, JSON codec and SharedPreferences adapter in one file | Split contract, codec and Android storage adapter |
| `data/*Codec.kt`, `action/*`, `model/*` | Existing typed actions, legacy import, versioned JSON and validation | Preserve behavior and compatibility; remove Android/View coupling from reusable domain code |
| `adaptive/*`, `state/DrawerGeometry.kt`, `SymbolDrawerDragPolicy.kt` | Mixture of pure layout policies and platform/window observation | Keep pure resolvers; reimplement window observation and layout in Compose |
| `compose/*`, `drawer/compose/*` | Initial native Compose implementations already exist | Treat as prototypes/foundation, audit semantics and rebuild where they inherit legacy assumptions |
| `src/main/res/values*/strings.xml` | Legacy UI strings coexist with Compose strings | Migrate required strings to Compose resource strategy; remove obsolete resources only after UI deletion |
| `src/androidTest/.../v2/*` | Many tests target legacy View contracts and host Activity | Replace with Compose UI behavior tests; retain only tests for platform-neutral contracts |
| `build.gradle.kts` | Still declares RecyclerView, ViewPager2, Material Views and legacy activity dependencies | Remove View-only dependencies after source graph is clean; retain only required Android/Compose integrations |

### Existing Compose foundations

The baseline already contains Compose implementations for the symbol drawer, adaptive layout, built-in group indicator, extension snapshot registry, manager screen/route/state, settings screen/route/state, generic drawer container, and several Compose tests. These are useful prototypes, not proof of complete migration. They must be reviewed against the complete feature contract and may be redesigned rather than mechanically preserved.

### Known build/runtime debt

- The module still compiles legacy View classes under `src/main`.
- Gradle declares `androidx.recyclerview:recyclerview`, `androidx.viewpager2:viewpager2` and `com.google.android.material:material`.
- The old generic drawer and legacy extension registry expose View-centric implementation concepts.
- Domain repository interfaces are coupled to Android storage classes by file organization.
- Existing instrumented tests include View-specific behavior and must not be mistaken for Compose UI coverage.
- Existing Compose UI package boundary check only covers two Compose package roots; it does not certify the whole artifact.

## Target architecture

```
public Compose API
  SymbolDrawer / rememberSymbolDrawerState
  ManagerRoute / SettingsRoute
  composable slots: drawerHandle, indicator, symbolCell, overlays, manager/settings extensions
        |
Compose presentation
  DrawerSurface + nested-scroll/drag arbitration
  Compose Pager + LazyVerticalGrid
  Material3 manager/settings/dialogs
  semantics, focus, keyboard, haptics and motion
        |
UI state and use cases
  immutable UI state + events + reducers
  group/symbol commands + settings commands
  extension registry snapshots + lifecycle-aware delivery
        |
platform-neutral domain
  SymbolGroup/SymbolEntry/SymbolAction
  validation + action contracts + JSON codecs + migration
  repository interfaces + pure layout/gesture policies
        |
Android adapters (no View UI)
  SharedPreferences/DataStore adapter (chosen deliberately)
  ContentResolver/SAF I/O behind Compose Activity Result launchers
```

The Compose module is a separately consumable artifact. Public APIs may expose Compose types and platform-neutral models, but must not expose legacy View types or require the View library.

## Execution phases and concrete deliverables

### Phase 0 — Baseline and guardrails
- Record branch/HEAD, full tracked-file inventory and current CI state.
- Keep every commit path-scoped to `zero-symbol-input-view-v2-compose/`.
- Add a full-module source/dependency audit; keep the PR Draft.
- Re-verify the sibling View module tree/blob identity after each structural milestone.

### Phase 1 — Domain and persistence separation
- Split group/settings repository interfaces from Android SharedPreferences implementations.
- Keep JSON codecs and schema migrations independent of Compose UI.
- Audit all `model`, `action`, `data`, `adaptive`, `drawer/core` classes for Android UI imports and hidden View references.
- Add contract tests for repository behavior, corrupt-data backup, stable IDs and old JSON round trips.
- Do not change serialized field names, enum identifiers, legacy action IDs or migration semantics without explicit compatibility tests.

### Phase 2 — Drawer state and interaction engine
- Define immutable public configuration and saveable state contracts.
- Implement vertical expansion, drag/fling settling, horizontal pager gesture arbitration, nested scroll, anchor policy and indicator reveal in Compose.
- Ensure pointer input does not compete with horizontal paging; define deterministic gesture ownership and cancellation.
- Preserve selected group by stable ID across repository refreshes.
- Cover reduced motion, haptics, touch targets, process recreation and state restoration.

### Phase 3 — Pager, grid and symbol rendering
- Implement Compose pager and LazyVerticalGrid, stable keys, bounded prefetch/cache, content updates and large-group performance.
- Implement symbol short/long press, selected/pressed semantics, full-label display and configurable density.
- Provide renderer slots without retaining View objects.
- Add UI tests for paging, grid geometry, rapid updates and large datasets.

### Phase 4 — Group indicator system
- Rebuild the indicator as a first-class Compose component API, not a tab background/highlight option.
- Treat each group as a managed page; the indicator is a replaceable navigation presentation.
- Reimplement every supported indicator style with Compose Canvas/layout/animation and correct selected-state semantics.
- Define custom indicator slot/renderer API, sizing, scrolling, accessibility and settings affordance behavior.
- Add screenshot/semantics/interaction tests per style family.

### Phase 5 — Manager experience
- Rebuild group list, add/rename/delete/duplicate/reorder, symbol CRUD, multi-select, copy/move, action editor and import/export flows.
- Use Compose state and Material 3 dialogs/sheets/menus; no legacy manager reuse.
- Preserve validation, ID uniqueness and atomic repository writes.
- Add end-to-end Compose tests for every mutation and failure path.

### Phase 6 — Settings experience and storage
- Rebuild all settings sections and controls in Compose Material 3.
- Preserve settings schema, defaults, bounds, reset/recovery semantics and remembered expansion/full-label settings.
- Use Compose Activity Result launchers for SAF; perform bounded streaming I/O off the main thread.
- Add round-trip, reset, malformed backup, oversized import and cancellation tests.

### Phase 7 — Compose extension framework
- Replace legacy extension registry with Compose-native slots/renderers for symbol cells, indicators, drawer regions, manager/settings tabs and custom settings rows.
- Keep action execution editor-neutral and separate from presentation extensions.
- Define deterministic ordering, duplicate-ID policy, registration handles, observer close semantics, lifecycle events and error isolation.
- Add concurrency, reentrancy, cancellation and composition disposal tests.

### Phase 8 — Remove migration baseline
- Remove old View widget, View indicator, ViewPager2/RecyclerView adapters, legacy manager/settings screens, View-bound extension registry and obsolete picker.
- Remove View-only resources and dependencies.
- Remove or rewrite View-specific instrumented tests in this module.
- Verify all remaining main/test/androidTest sources and public signatures contain no forbidden View UI types.
- Verify resolved runtime dependency graph has no RecyclerView, ViewPager2, Material Views or View-library artifact.

### Phase 9 — Release gates
- Build Compose debug and release variants independently.
- Run all unit tests and Compose instrumentation/UI tests.
- Run the original View library build/tests unchanged.
- Validate legacy V1 and V2 JSON fixtures and action behavior.
- Verify public API documentation and sample integration from a Compose-only consumer.
- Verify Maven/JitPack publication metadata and dependency graph.
- Only after all gates pass may PR #55 leave Draft.

## Working rules

1. No broad copy from the View module into the Compose module as an implementation shortcut.
2. No Android View wrapper, bridge widget, fallback or hidden View dependency.
3. No deletion of domain logic until its behavior has been ported and tests prove parity.
4. Every migration commit must be scoped to this Compose module.
5. A source grep is not enough: inspect public API signatures and resolved Gradle dependencies.
6. Report implementation, static audit, build, unit tests, instrumentation and CI as separate statuses.
