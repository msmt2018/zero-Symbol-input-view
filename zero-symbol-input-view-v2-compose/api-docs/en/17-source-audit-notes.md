# 17 — Compose Source Audit Notes and Known Boundaries

This page records the architecture boundaries and audit methods for the Compose module. It is not a device-runtime test report, and it does not treat compatibility results from the legacy View module as Compose migration results.

## Audit Scope

Target module: `zero-symbol-input-view-v2-compose/`

Audit in dependency order:

1. Editor-neutral text and action contracts.
2. Domain models, validation, and action execution.
3. Repositories, codecs, migration, and persistence.
4. Drawer anchors, geometry, state, and interaction policies.
5. Navigation, panels, adaptive layout, and extension registration.
6. Compose screens and symbol-drawer UI.

## Key Boundaries
## Generic Drawer package relocation

The generic Drawer source files now use the canonical package tree:

- `com.zerostudio.idea.widget.drawer`
- `com.zerostudio.idea.widget.drawer.core`
- `com.zerostudio.idea.widget.drawer.config`
- `com.zerostudio.idea.widget.drawer.navigation`
- `com.zerostudio.idea.widget.drawer.panel`
- `com.zerostudio.idea.widget.drawer.style`
- `com.zerostudio.idea.widget.drawer.adapter`

The former `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` namespace is not the source package. The symbol-input UI and domain packages remain under `editor.symbolinput.v2`. The View artifact exposes a View-based Drawer with these concepts; this artifact exposes Composables and Composable slots.


- Domain, data, and core state APIs must not all be annotated `@Composable` merely to serve the UI.
- Compose pages must not implement their UI with an Android View tree or wrap legacy controls with `AndroidView` / `AndroidViewBinding`.
- Compose page state must not depend on Activity, Fragment, ViewModel, or LifecycleOwner.
- Compose itself still has Composition, recomposition, remember-state scope, and effect-cancellation semantics.
- Kotlin `typealias` declarations do not generate Java ABI facade classes.
- A SharedPreferences repository may use `applicationContext` for persistence; this does not make a page dependent on an Activity lifecycle.

## Verification Methods

- Source scanning: inspect production and test sources for legacy View APIs, Activity/Fragment page state, and AndroidView wrappers.
- Dependency auditing: inspect the Compose artifact's resolved runtime classpath, not only its direct dependencies.
- Build verification: compile Compose Debug/Release, unit tests, and instrumentation tests.
- Behavioral verification: cover drawer state, edit transactions, data migration, stable page IDs, extension registration order, and error paths.

Static scanning cannot replace runtime tests. A successful compilation alone cannot prove that every interaction behaves correctly.

## Compatibility Module

`zero-symbol-input-view-v2/` is a separate legacy View module. Failures in its Android device tests must be reported separately and must not be conflated with the Compose module's source boundaries or build results.

## Editor-Neutral Action API Relocation

The generic text-action implementation is owned by `com.zerostudio.idea.widget.drawer.action`. The symbol-input package retains a JVM-visible forwarding object for source and binary compatibility; new integrations should import the public drawer package. This avoids a dependency from the reusable editor-neutral API back into the symbol-input feature namespace.


## Editor-neutral text algorithm relocation

`SymbolTextEditing`, `SymbolTextMacroExpansion`, `SymbolTextRange`, and
`SymbolTextInsertion` now have canonical implementations/types in
`drawer.action`. The old `editor.symbolinput.v2.action` JVM classes remain
as forwarding compatibility declarations with the original result signatures.
The Compose document action host uses the canonical same-package utility.
A Java compile fixture and Kotlin public-API tests exercise the new API; the
legacy Kotlin tests continue to exercise the compatibility facade.
