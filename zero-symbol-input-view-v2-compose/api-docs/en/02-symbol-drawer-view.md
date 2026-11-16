# 02 — SymbolDrawerView API

Package: `com.zerostudio.idea.widget.editor.symbolinput.v2`.

## Responsibility

`SymbolDrawerView` is an Android ViewGroup for symbol input. It composes the group indicator, ViewPager2, symbol page adapter, drawer interaction controller, group repository and settings repository. It is editor-neutral: actions are sent to the host.

## Constructor

```kotlin
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    config: model.DrawerConfig = model.DrawerConfig()
)
```

Use a themed Activity/Fragment Context. Note that `model.DrawerConfig` is not the same type as the generic Drawer's `drawer.config.DrawerConfig`.

## Callbacks

- `onSymbolActionListener`: receives `(SymbolEntry, isLongPress)`. The host chooses `shortAction` or `longAction` and executes it.
- `onSymbolUsedListener`: usage notification after the action has entered the host callback path. It does not confirm a successful text edit; record success from `SymbolActionResult`.
- `onOpenManagerListener`: handles the built-in open-manager action.
- `onOpenSettingsListener`: handles settings navigation. If null, the built-in settings screen is shown in a dialog.
- `onGroupChangedListener`: receives the selected index and current group (which may be null). Data submission may also trigger it.

## Data methods

- `setRepository(repository, reload = true)`: replace the repository; optionally reload immediately.
- `submitGroups(groups, preserveSelectedGroup = true, onCommitted = null)`: submit a validated defensive snapshot and preserve selection by stable group ID where possible.
- `reloadGroups()`: reload from the current repository.
- `saveGroups()`: persist the current snapshot.
- `getGroups()`: return the current group snapshot.

Group IDs must be unique. Symbol IDs are checked within each group by the View submission boundary; use globally unique nonzero IDs for storage and manager operations.

## Paging and expansion

- `getCurrentGroupIndex()`
- `selectGroup(index, smoothScroll = true)`
- `isExpanded()`
- `setExpanded(expanded, animate = true)`

## Settings and lifecycle

- `applySettings(settings)`, `getSettings()`, `refreshHostTheme()`
- `onHostResume()`: refresh host-dependent state when returning from a host screen.
- Attach/detach manages internal listeners; detach cancels gesture/animation work.
- Saved state preserves selected group identity and expansion fraction.

## Gesture contract

Horizontal movement belongs to ViewPager2; vertical movement may be claimed by the drawer after axis arbitration and child-scroll checks. Do not intercept all MOVE events in a parent layout. Keep callbacks fast and do I/O outside gesture callbacks.
