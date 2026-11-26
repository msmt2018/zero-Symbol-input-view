# Zero Symbol Drawer V2 — Compose Architecture

## Migration contract

The `zero-symbol-input-view-v2-compose` module is the Compose implementation. The
legacy `zero-symbol-input-view-v2` module remains a separate compatibility
reference and is not part of this module's runtime UI.

The Compose module must not implement UI through Android Views or bind page
state to Android's traditional page lifecycle APIs:

- No `android.view.View`, `ViewGroup`, `android.widget`, or View-based
  Material components in production UI.
- No `AndroidView` / `AndroidViewBinding` interop as a substitute for
  migrating a control.
- No RecyclerView, ViewPager2, Fragment, Activity, ViewModel,
  LifecycleOwner, LifecycleRegistry, or SavedStateRegistry integration in
  feature source.
- UI state is owned by Composition and explicit state holders. Animation and
  gesture work is driven by Compose APIs and caller-provided composition scopes.
- Domain, persistence, action execution, geometry, and policy code remain
  independent of page lifecycle and should not import Compose UI unless the
  type is specifically a Compose extension slot.

"Lifecycle-neutral" means no dependency on Android Activity/Fragment/ViewModel
or LifecycleOwner page orchestration. Compose still has Composition,
recomposition, and effect lifetimes; it is not literally lifecycle-free.

## Bottom-up dependency layers

Refactors must move from lower layers upward. Do not implement a new UI
component against an unstable framework API and then reshape the framework
around that component.

### Layer 0 — editor-neutral text and action contracts

Location: `com.zerostudio.idea.widget.drawer.action`

Responsibilities:

- UTF-16 document offsets, selection, cursor, line mapping and edit semantics.
- Atomic edit/rollback contract where the host supports it.
- Action host, cancellation, execution result and bounded action execution.
- Action registry and extension dispatch.
- No editor-specific class, Android View, Activity, Fragment or lifecycle owner.

This layer is the host integration boundary. Platform clipboard or storage
adapters may be supplied by the application, but the action engine itself stays
UI-neutral.

### Layer 1 — domain models and validation

Location: `editor.symbolinput.v2.model` and action model packages.

Responsibilities:

- Stable group/symbol IDs and typed short/long actions.
- Immutable snapshots and strict input validation.
- Composite action depth/node/payload limits.
- Compatibility metadata for importing legacy data without retaining legacy UI.

### Layer 2 — persistence and migration

Location: `editor.symbolinput.v2.data` and settings repository packages.

Responsibilities:

- Repository interfaces and versioned codecs.
- Atomic replacement and one-way legacy-data import.
- Defensive copies at repository boundaries.
- Persistence failures must not publish an unpersisted in-memory state.
- No UI or page lifecycle dependency.

### Layer 3 — drawer core state, geometry and interaction policy

Location: `editor.symbolinput.v2.drawer.core` and `state`.

Responsibilities:

- Anchor resolution, drawer state, transition generation and stale-frame
  rejection.
- Drag, fling and nested-scroll arbitration.
- Pixel geometry and density validation.
- State holders expose observable snapshots; animation execution belongs to
  the Compose host.
- Explicit thread-confinement contract; no hidden Activity or lifecycle owner.

### Layer 4 — adaptive layout, navigation, panels, styles and extensions

Locations: `adaptive`, `drawer.navigation`, `drawer.panel`,
`drawer.style`, and `extension`.

Responsibilities:

- Window measurements represented as immutable data.
- Stable page IDs and selection synchronization.
- Content slots expressed as `@Composable` contracts.
- Extension registration returns closeable handles; registry owns no UI
  lifecycle and does not silently create host components.
- Adaptive decisions remain policy objects; actual placement is Compose layout.

### Layer 5 — Compose UI and screens

Locations: `ui`, `drawer`, `SymbolDrawerView.kt`, `manager`, and
`settings`.

Responsibilities:

- Compose Foundation and Material 3 only.
- Lazy grids/lists and Compose Pager for virtualized content.
- State is passed through stable immutable models and explicit callbacks.
- No Activity/Fragment/ViewModel/LifecycleOwner dependency.
- File/document operations are isolated behind host callbacks or platform
  adapters; they do not introduce View-based UI.

## Runtime composition

```text
SymbolDrawer (Composable)
├── DrawerContainer (Composable shell)
│   ├── DrawerStateHolder (Composition-observable state)
│   ├── DrawerInteractionController (gesture policy)
│   └── AdaptiveContentLayout (Compose layout)
├── SymbolGroupIndicator (Compose indicator)
├── DrawerPageHost (Compose HorizontalPager)
│   └── SymbolPageAdapter (Compose LazyVerticalGrid renderer)
└── Symbol action dispatch
    └── SymbolActionExecutor
        └── SymbolActionHost / SymbolTextDocument adapter
```

The generic drawer shell and the symbol-input product are separate API
surfaces. Public facade packages may use Kotlin `typealias` for source
compatibility; a typealias does not create a Java-visible class. Physical
package relocation and Java facade classes must be handled as separate,
explicit compatibility changes.

## State and ownership rules

- Repository owns persisted data; manager commands validate and persist before
  publishing a new snapshot.
- Drawer state holder owns continuous height, drag remainder, anchor target,
  and animation generation.
- Compose host owns animation scheduling and passes generation-tagged frames
  back to the state holder.
- Navigation state uses stable page IDs, not list indices as identity.
- Extension registry owns registrations and revisioned snapshots, not screen
  lifetimes. Hosts close registration handles when their own feature/session
  ends.
- No state holder may retain Activity, Fragment, View, LifecycleOwner, or a
  coroutine scope implicitly.

## Build boundary

The module's `check` task enforces both source and resolved production
runtime dependency boundaries. The migration inventory is diagnostic; it must
not be confused with the strict gate. Runtime dependency validation resolves
both debug and release runtime classpaths so a transitive Android View or
Lifecycle dependency cannot silently re-enter the artifact.

## Migration status

This document describes the target architecture and current source ownership.
It does not claim that every copied compatibility file, every public API
facade, every extension consumer, or every documentation example has already
been physically migrated. Remaining work must be recorded against concrete
source paths and completed bottom-up without altering the legacy module.
