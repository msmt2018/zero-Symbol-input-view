# Compose library architecture and migration contract

This repository intentionally ships **two independent Android libraries**:

- `zero-symbol-input-view-v2`: the existing Android View implementation.
- `zero-symbol-input-view-v2-compose`: a separately consumable, native Jetpack Compose implementation.

The Compose library is not a Compose wrapper around the View library. It must not depend on the View library, embed its widgets, or use View UI as a fallback. The View library remains intact and continues to serve View-based consumers.

## Non-negotiable Compose boundary

Every user-interface surface in this module must be implemented with the AndroidX Jetpack Compose SDK:

- Layout and measurement: Compose UI and Foundation layouts.
- Material styling and controls: Material 3 Compose components and tokens.
- Icons: Compose-compatible Material / vector composables, with a documented Compose-native extension point for custom icons.
- Drawer surface, anchors, gestures and nested gesture arbitration: Compose Foundation gestures and Compose state.
- Motion: Compose Animation APIs, respecting reduced-motion settings.
- Group navigation and paging: Compose-native pager / lazy layouts.
- Symbol grid and cells: LazyVerticalGrid and composable item renderers.
- Manager, settings, import/export screens and dialogs: Material 3 Compose.
- Theme, typography, shapes, colors and dynamic color integration: Material 3 Compose.
- Accessibility, semantics, focus, keyboard and touch targets: Compose semantics and input APIs.
- Public customization: composable slots, composable renderers, immutable state and event callbacks.

Forbidden in the Compose UI implementation:

- `AndroidView` / `AndroidViewBinding` wrappers around old drawer UI.
- `SymbolDrawerView`, old View group indicators, RecyclerView adapters, ViewPager2 adapters, old View manager/settings screens, or any other legacy drawer View widget.
- `android.view.View`, `android.view.ViewGroup`, `android.widget.*`, View-based Canvas / Drawable rendering, RecyclerView, ViewPager2, or Material Components for Views as UI building blocks.
- A runtime fallback that silently swaps the Compose UI for the View implementation.

Android platform APIs that are not UI widgets may still be used where appropriate (for example Context, Uri, ContentResolver, Activity Result APIs, lifecycle integration and persistence). Prefer Compose-native host integration such as `rememberLauncherForActivityResult` for document picking.

## What may be shared with the View library

Only UI-independent contracts and behavior may be shared or ported between the libraries:

- Symbol/group/action data models and validation rules.
- JSON schemas, codecs, import/export compatibility and migrations.
- Persistence formats and repository contracts.
- Editor-neutral action descriptions and execution protocols.
- Pure Kotlin policies and algorithms that do not expose View types.

Sharing a data format does not mean sharing a UI class or UI extension contract. The Compose artifact must remain independently usable without bringing in the View artifact or View-only dependencies.

## Compose public API design

Compose-specific extension APIs must use Compose idioms:

- `@Composable` slots for drawer regions, group indicators, symbol cells, manager/settings content and custom controls.
- Composable renderer interfaces or function types for custom symbol / indicator presentation.
- Immutable configuration and state holders, with explicit event callbacks.
- Stable keys and lifecycle-aware state observation.
- Material 3 color, typography, shape and component conventions.
- No API parameter, return type, callback or registry entry may require a legacy View class.

The generic drawer API under `com.zerostudio.idea.widget.drawer` is also a native Compose API. It must remain editor-neutral and must not acquire a dependency on the symbol drawer View implementation.

## Current migration status

The initial Compose module was created by copying the V2 module as a migration baseline. That baseline still contains legacy View source files and View-only dependencies. Their presence is technical debt, **not an accepted final architecture**.

The existing Compose entry points (drawer, manager, settings, group indicator and generic drawer) are the native implementation foundation. The remaining work is to extract UI-independent contracts, replace every copied View-bound screen / adapter / extension / lifecycle integration with Compose-native equivalents, then remove obsolete View sources, resources and dependencies from this artifact.

Do not describe the artifact as fully native Compose until the source audit and dependency audit both pass.

## Required completion gates

1. Both libraries build independently; the Compose artifact has no dependency on the View artifact.
2. All public drawer, manager, settings, indicator, action presentation and extension APIs are Compose-native.
3. No forbidden View UI imports, classes, adapters, wrappers or resources remain in the Compose artifact.
4. View-only dependencies (including RecyclerView, ViewPager2 and Material Components for Views) are absent from the Compose runtime dependency graph.
5. Existing V1 / V2 JSON data remains importable and round-trips without loss.
6. Action execution remains editor-neutral and does not retain host editor objects.
7. Drawer gestures, horizontal paging, anchor settling, adaptive layouts, accessibility, keyboard/focus, reduced motion and save/restore are covered by Compose UI tests.
8. Manager/settings workflows, including import/export, editing, reordering and reset, are covered by tests.
9. Run the full original View library test/build suite to prove that its behavior was not changed by Compose migration.
10. Keep the migration PR in Draft until all gates and CI pass.
