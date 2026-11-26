# Zero Symbol Drawer V2 — Compose Architecture

## Migration contract

The `zero-symbol-input-view-v2-compose` module is a native Jetpack Compose implementation. The legacy `zero-symbol-input-view-v2` module is a separate Android View artifact and is not part of this module's UI runtime.

The Compose module must not implement UI through Android Views or bind page state to Android's traditional page lifecycle APIs:

- No `android.view.View`, `ViewGroup`, `android.widget`, or View-based Material components in production UI.
- No `AndroidView` / `AndroidViewBinding` interop as a substitute for migrating a control.
- No RecyclerView, ViewPager2, Fragment, Activity, ViewModel, LifecycleOwner, LifecycleRegistry, or SavedStateRegistry integration in feature UI.
- UI state is owned by Composition and explicit state holders.
- Domain, persistence, action execution, geometry and policy code remain independent of page lifecycle.

“Lifecycle-neutral” means no dependency on Activity/Fragment/ViewModel or LifecycleOwner page orchestration. Compose still has Composition, recomposition and effect lifetimes; it is not literally lifecycle-free.

## Canonical package layout

- Generic Drawer: `com.zerostudio.idea.widget.drawer`
- Drawer core: `com.zerostudio.idea.widget.drawer.core`
- Navigation: `com.zerostudio.idea.widget.drawer.navigation`
- Panels: `com.zerostudio.idea.widget.drawer.panel`
- Styles: `com.zerostudio.idea.widget.drawer.style`
- Editor-neutral actions: `com.zerostudio.idea.widget.drawer.action`
- Symbol UI: `com.zerostudio.idea.widget.editor.symbolinput.v2`
- Symbol domain/data/settings: `com.zerostudio.idea.widget.editor.symbolinput.v2.*`

The old `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` package is no longer canonical. Do not add new source or imports under that path.

## Bottom-up dependency layers

Refactors move from lower layers upward. Do not build a new UI component against an unstable framework API and then reshape the framework around that component.

### Layer 0 — editor-neutral text and action contracts

Location: `com.zerostudio.idea.widget.drawer.action`

- UTF-16 document offsets, selection, cursor, line mapping and edit semantics.
- Action host, cancellation, execution result and bounded action execution.
- Action registry and extension dispatch.
- No editor-specific class, Android View, Activity, Fragment or lifecycle owner.

### Layer 1 — domain models and validation

Location: `com.zerostudio.idea.widget.editor.symbolinput.v2.model`

- Stable group/symbol IDs and typed short/long actions.
- Immutable snapshots and strict input validation.
- Composite action depth/node/payload limits.
- Compatibility metadata for importing legacy data.

### Layer 2 — persistence and migration

Locations: `editor.symbolinput.v2.data` and settings repository packages.

- Repository interfaces and versioned codecs.
- One-way legacy-data import.
- Defensive copies at repository boundaries.
- Persistence failures must not publish an unpersisted in-memory state.
- No UI or page lifecycle dependency.

### Layer 3 — drawer core state, geometry and interaction policy

Location: `com.zerostudio.idea.widget.drawer.core`

- Anchor resolution, state transitions and stale-frame rejection.
- Drag, fling and nested-scroll arbitration.
- Pixel geometry and density validation.
- State holders publish observable snapshots; animation execution belongs to the Compose host.
- Core state engine remains independent of Compose UI.

### Layer 4 — adaptive layout, navigation, panels, styles and extensions

Locations: `adaptive`, `drawer.navigation`, `drawer.panel`, `drawer.style` and `extension`.

- Window measurements represented as immutable data.
- Stable page IDs and selection synchronization.
- Content slots expressed as `@Composable` contracts.
- Extension registration returns closeable handles; registry owns no UI lifecycle.
- Adaptive decisions remain policy objects; actual placement is Compose layout.

### Layer 5 — Compose UI and screens

Locations: `editor.symbolinput.v2`, `manager`, `settings` and Compose UI packages.

- Compose Foundation and Material 3.
- Lazy layouts and stable keys for dynamic symbol groups.
- Gesture and nested-scroll coordination through Compose APIs.
- Accessibility semantics and reduced-motion support.
- No Android View wrappers.

## Public Drawer API

`DrawerContainer` is a Composable in `com.zerostudio.idea.widget.drawer`. `DrawerPanelContent` wraps a Composable ColumnScope slot. `DrawerPageHost` uses Compose paging and stable page IDs. The separate View artifact uses the same conceptual names for Android View classes; the two artifacts are not interchangeable and should not be added together.

## Composition resource semantics

Composition-scoped state and effects are cancelled/removed as their keys or owning Composition leave scope. Use `remember`, `LaunchedEffect`, `DisposableEffect` and stable keys for the intended purpose. Do not describe these semantics as “no lifecycle”.
