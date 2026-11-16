# 15 — Practical Symbol Control Extension Guide

This guide extends the existing `SymbolDrawerView`, `SymbolPageAdapter`, indicator, manager and settings surfaces. It is not a recipe for creating a second toolbar. The original control remains the owner of paging, touch arbitration, grid geometry, action dispatch, data and recycling.

## Register per drawer session

```kotlin
val handles = mutableListOf<AutoCloseable>()
handles += drawer.extensions.registerAction(/* action extension */)
handles += drawer.extensions.registerItemView(/* renderer */)
handles += drawer.extensions.registerIndicator(/* indicator */)
// Close all handles with the owner lifecycle.
```

The registry is session-scoped. Re-register runtime callbacks after process recreation. Do not keep Activity, View or editor objects in static singletons.

## Custom actions

Give each action a stable reverse-domain ID, title, description, optional payload hint and priority. Persist only `SymbolAction.Extension(id, payload)`; the handler is registered at runtime and invoked through the drawer-bound host.

## Custom symbol-cell renderer

A renderer decides whether it accepts an item, creates an unattached View, binds the full current item/state and releases its resources on recycle/dispose. The original adapter retains cell size, touch, click, long-press, scroll, accessibility and recycling ownership. If no renderer accepts an item or create/bind fails, the built-in cell is used.

Do not retain MotionEvent instances or intercept touch from an observer callback. Bind all state on every call; recycled Views must not leak previous item state.

## Custom group indicator

Register a `SymbolGroupIndicatorExtension`, then call `setCustomGroupIndicator(id)`. Restore the built-in indicator with `useBuiltInGroupIndicator()`. The custom component receives group snapshots, selected index and an `onSelectGroup` callback. ViewPager2 remains the sole owner of page navigation; never create another pager or selected-index source of truth.

## Custom settings

Register a full settings tab or a setting row. Pass the same `drawer.extensions` instance to `SymbolDrawerSettingsScreen`. Use the provided settings scope for updates to the existing V2 settings model. Store extension-specific values in host-owned storage.

## Theme provider

Register a provider returning ARGB color tokens and select it with `setThemeExtension(id)`. Passing null restores host-derived colors. Current implementation scope is limited to the drawer shell and built-in group indicator (plus the custom indicator theme callback); it does not imply full recoloring of manager/settings/cells.

## Legacy providers and numeric actions

Legacy group providers feed the existing codec/repository pipeline; they are not parallel repositories. Numeric V1 action IDs remain compatibility metadata. New extensions should use stable string IDs.

## Error and lifecycle semantics

Duplicate IDs replace the active contribution; an old handle must not remove a newer registration. Close registrations and observers when their owner ends. Renderer/indicator failures should fall back to built-in UI where documented. Registry observer failures are isolated; log them in the observer if diagnostics are required.
