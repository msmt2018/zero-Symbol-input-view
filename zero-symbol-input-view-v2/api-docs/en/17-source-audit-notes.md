# 17 — View Artifact Source Audit Notes

This page records the API and package boundaries of `zero-symbol-input-view-v2`. It is a source-level reference, not a claim that every device interaction has been executed.

## Artifact scope

Target: `zero-symbol-input-view-v2` (Android View library).

- Symbol product entry: `com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView`.
- Symbol models: `com.zerostudio.idea.widget.editor.symbolinput.v2.model`.
- Generic Drawer: `com.zerostudio.idea.widget.drawer.*`.
- Editor-neutral actions: `com.zerostudio.idea.widget.drawer.action`.

## Package relocation

The generic Drawer implementation sources now declare their canonical packages under `com.zerostudio.idea.widget.drawer`, including core, config, navigation, panel, style and adapter packages. The previous `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` path is not the source package for the relocated implementation.

The symbol toolbar remains an Android View API. Do not infer that moving generic Drawer sources makes `SymbolDrawerView` a Compose component.

## View API boundaries

- `DrawerContainer` is an Android `FrameLayout` / `NestedScrollingParent3`.
- `DrawerPanelContent` creates Android Views and receives panel visibility/attach callbacks.
- `DrawerPageHost` manages View-based page content.
- The symbol toolbar uses ViewPager2 and RecyclerView-backed pages.
- View attach/detach and saved-state behavior are owned by the Android View host.

## Kotlin and Java API compatibility

Kotlin `typealias` declarations provide source aliases only; they do not emit independent JVM classes. Java consumers must import actual JVM declarations. A package path in Kotlin documentation is not by itself proof that a corresponding JVM class exists.

## Editor-neutral action contract

The action host is synchronous and does not switch threads. Composite actions are sequential and can partially complete. UTF-16 offsets and end-exclusive ranges are used by the generic document API. Atomic rollback depends on the host's actual transaction implementation.

## Validation boundaries

Static source review and JVM tests do not prove real device gesture, nested-scroll, Insets, accessibility or Fragment View cleanup behavior. Those require Android build and device/instrumentation validation. Report the exact module/job and distinguish View instrumentation results from Compose tests.
