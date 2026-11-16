# Compose V2 API Development and Integration Guide (Canonical)

> This guide documents only native Jetpack Compose APIs that exist in the current `zero-symbol-input-view-v2-compose` branch. Every integration example includes its imports. APIs still being migrated are explicitly marked; legacy View APIs are never presented as Compose APIs.

## 1. Module, dependency, and packages

Module: `zero-symbol-input-view-v2-compose`

In-repository dependency:

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2-compose"))
}
```

The Maven publication is configured as:
`com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2-compose:<published-version>`.
Use an actually published Git tag, never a branch name as a released version.

| Purpose | Package |
|---|---|
| Drawer, state, defaults, extensions | `com.zerostudio.idea.widget.editor.symbolinput.v2.compose` |
| Compose manager route | `com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager` |
| Compose settings route | `com.zerostudio.idea.widget.editor.symbolinput.v2.compose.settings` |
| Symbol models | `com.zerostudio.idea.widget.editor.symbolinput.v2.model` |
| Action execution/editor adapter | `com.zerostudio.idea.widget.drawer.action` |
| Group repository | `com.zerostudio.idea.widget.editor.symbolinput.v2.data` |
| Settings model/repository | `com.zerostudio.idea.widget.editor.symbolinput.v2.settings` |

Do not copy View-module calls such as `SymbolDrawerView.OnSymbolActionListener`, `reloadGroups()`, `setRepository()`, or `bindToDrawer()` into Compose integrations.

## 2. Minimal integration: SymbolDrawer

### 2.1 Complete imports

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.rememberSymbolDrawerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
```

### 2.2 Define data and render the drawer

```kotlin
private val editorGroups = listOf(
    SymbolGroup(
        id = "kotlin",
        title = "Kotlin",
        symbols = listOf(
            SymbolEntry(
                id = 1001L,
                label = "fun",
                shortAction = SymbolAction.InsertText("fun ")
            ),
            SymbolEntry(
                id = 1002L,
                label = "->",
                shortAction = SymbolAction.InsertText(" -> "),
                longAction = SymbolAction.InsertText(" => ")
            )
        )
    ),
    SymbolGroup(
        id = "punctuation",
        title = "Punctuation",
        symbols = listOf(
            SymbolEntry(
                id = 2001L,
                label = "()",
                shortAction = SymbolAction.InsertText("()")
            )
        )
    )
)

@Composable
fun EditorSymbolToolbar(
    onInsertText: (String) -> Unit,
    onOpenManager: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberSymbolDrawerState()

    SymbolDrawer(
        groups = editorGroups,
        state = drawerState,
        modifier = modifier,
        settings = SymbolDrawerSettings(),
        onSymbolClick = { entry ->
            val action = entry.shortAction
            if (action is SymbolAction.InsertText) onInsertText(action.template)
        },
        onSymbolLongClick = { entry ->
            val action = entry.longAction ?: entry.shortAction
            if (action is SymbolAction.InsertText) onInsertText(action.template)
        },
        onOpenManager = onOpenManager,
        onOpenSettings = onOpenSettings
    )
}
```

Notes:
- `SymbolDrawer` is a Composable. It does not create a View or require manual attach/detach.
- `rememberSymbolDrawerState()` retains expansion state and the selected stable group ID in Composition.
- Treat `groups` as an immutable snapshot. Submit a new list when data changes; do not mutate a caller-owned list in place.
- Symbol callbacks notify the host. The host editor adapter is responsible for executing actions.
- If a symbol has no long-press action, use the short action as the fallback.
- `onOpenManager` and `onOpenSettings` are host navigation callbacks; the library does not create host Activities or Screens for you.

## 3. Connect the editor action engine

### 3.1 API types and imports

```kotlin
import com.zerostudio.idea.widget.drawer.action.SymbolActionExecutor
import com.zerostudio.idea.widget.drawer.action.SymbolActionHost
import com.zerostudio.idea.widget.drawer.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
```

The application implements `SymbolActionHost`:

```kotlin
interface SymbolActionHost {
    fun insertText(template: String): Boolean
    fun replaceSelection(text: String): Boolean
    fun surroundSelection(prefix: String, suffix: String): Boolean
    fun moveCursor(target: CursorTarget): Boolean
    fun delete(kind: DeleteKind): Boolean
    fun transformSelection(transform: SelectionTransform): Boolean
    fun executeEditorCommand(commandId: String, payload: String?): Boolean
    fun performClipboard(operation: ClipboardOperation): Boolean
    fun openManager(): Boolean
    fun executeExtension(extensionId: String, payload: String?): Boolean
}
```

The model imports used by this interface are:

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
```

### 3.2 Execute actions from Compose

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.drawer.action.SymbolActionExecutor
import com.zerostudio.idea.widget.drawer.action.SymbolActionHost
import com.zerostudio.idea.widget.drawer.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.rememberSymbolDrawerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

@Composable
fun EditorToolbar(
    groups: List<SymbolGroup>,
    actionHost: SymbolActionHost,
    onActionResult: (SymbolEntry, SymbolActionResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberSymbolDrawerState()
    val executor = remember { SymbolActionExecutor() }

    fun execute(entry: SymbolEntry, longPress: Boolean) {
        val action = if (longPress) entry.longAction ?: entry.shortAction
                     else entry.shortAction
        val result = executor.execute(action, actionHost)
        onActionResult(entry, result)
    }

    SymbolDrawer(
        groups = groups,
        state = state,
        modifier = modifier,
        onSymbolClick = { execute(it, false) },
        onSymbolLongClick = { execute(it, true) }
    )
}
```

`SymbolActionExecutor.execute(action, host)` is synchronous and runs on the caller's thread. Respect the editor's threading requirements. Results include:
- `Executed`: action completed.
- `Unsupported`: host does not support the action.
- `Invalid`: invalid action data or composite-action limits.
- `Cancelled`: cooperative cancellation.
- `Failed`: host execution threw an exception.
- `PartialFailure`: a composite action partially completed before failing.

Do not treat callback delivery as proof of a successful edit; inspect `SymbolActionResult`.

### 3.3 Reuse the editor-neutral text adapter

If an editor can expose a text document with UTF-16 offsets, selection, cursor and replacement operations, implement `SymbolTextDocument` and reuse `SymbolTextDocumentActionHost` for common insertion, replacement, selection wrapping, cursor movement, deletion and case transformation.

```kotlin
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocument
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocumentActionHost
import com.zerostudio.idea.widget.drawer.action.SymbolTextClipboard

// MyEditorDocumentAdapter implements every abstract SymbolTextDocument member.
val actionHost = SymbolTextDocumentActionHost(
    document = MyEditorDocumentAdapter(editor),
    clipboard = SymbolTextClipboard { document, operation ->
        editorClipboard.perform(document, operation)
    },
    openManagerAction = { navigator.openSymbolManager(); true },
    extensionAction = { id, payload -> editorExtensions.execute(id, payload) }
)
```

All offsets in `SymbolTextDocument` are UTF-16 offsets, matching JVM String indexing. Implementations must keep document text and selection coherent after a successful `replace`. Clipboard, manager navigation and host-specific extensions are injected by the app. `MyEditorDocumentAdapter`, `editorClipboard`, `navigator`, and `editorExtensions` above are host-owned examples, not library classes.

## 4. Extension registration framework

### 4.1 Current extension categories

All extension types are in:

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.*
```

Eight extension categories currently exist:

| Registration | Extension type | Consumer |
|---|---|---|
| `registerAction` | `ComposeSymbolDrawerActionExtension` | Symbol action dispatch |
| `registerItemRenderer` | `SymbolDrawerItemRenderer` | Symbol cell renderer |
| `registerGroupIndicator` | `SymbolDrawerGroupIndicator` | Group indicator |
| `registerGroupProvider` | `ComposeSymbolDrawerGroupProvider` | Dynamic groups |
| `registerTheme` | `ComposeSymbolDrawerThemeExtension` | Material 3 ColorScheme |
| `registerEvent` | `ComposeSymbolDrawerEventExtension` | Settings/group/selection/expansion/symbol events |
| `registerSettingsTab` | `ComposeSymbolDrawerSettingsTabExtension` | Settings tab content |
| `registerSetting` | `ComposeSymbolDrawerSettingExtension` | Settings item content |

IDs must be unique and match `[A-Za-z0-9][A-Za-z0-9_.:/-]{0,255}`. Contributions are ordered by descending priority, then ID. Registering a new contribution with the same ID replaces the old value; an old registration handle cannot accidentally remove a newer replacement.

### 4.2 Registry with Composition-managed cleanup

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolDrawerExtensionRegistry
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.rememberSymbolDrawerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

@Composable
fun ExtensibleSymbolToolbar(groups: List<SymbolGroup>) {
    val registry = remember { ComposeSymbolDrawerExtensionRegistry() }

    DisposableEffect(registry) {
        val actionRegistration = registry.registerAction(myActionExtension)
        val rendererRegistration = registry.registerItemRenderer(myItemRenderer)
        val indicatorRegistration = registry.registerGroupIndicator(myGroupIndicator)
        val providerRegistration = registry.registerGroupProvider(myGroupProvider)
        val themeRegistration = registry.registerTheme(myThemeExtension)
        val eventRegistration = registry.registerEvent(myEventExtension)
        val tabRegistration = registry.registerSettingsTab(mySettingsTab)
        val settingRegistration = registry.registerSetting(mySetting)

        onDispose {
            settingRegistration.close()
            tabRegistration.close()
            eventRegistration.close()
            themeRegistration.close()
            providerRegistration.close()
            indicatorRegistration.close()
            rendererRegistration.close()
            actionRegistration.close()
        }
    }

    SymbolDrawer(
        groups = groups,
        state = rememberSymbolDrawerState(),
        extensionRegistry = registry,
        onSymbolClick = {},
        onSymbolLongClick = {}
    )
}
```

The `my...` values are host-defined implementations described below. Registration handles are closed by `DisposableEffect` when the contribution leaves Composition. No View lifecycle listener is required. For static contributions, pass an immutable `ComposeSymbolDrawerExtensions` snapshot instead of using a mutable registry.

### 4.3 Custom action extension

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolActionHandler
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolDrawerActionExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction

val myActionExtension = ComposeSymbolDrawerActionExtension(
    id = "my-editor.format",
    title = "Format",
    priority = 100,
    handler = ComposeSymbolActionHandler { action, symbol ->
        if (action is SymbolAction.EditorCommand &&
            action.commandId == "my-editor.format"
        ) {
            editorFormatter.format(symbol)
            true
        } else {
            false
        }
    }
)
```

### 4.4 Custom symbol cell renderer

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.SymbolDrawerItemRenderer
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry

object MySymbolRenderer : SymbolDrawerItemRenderer {
    override val id = "my-editor.symbol-renderer"
    override val priority = 50

    override fun accepts(symbol: SymbolEntry): Boolean =
        symbol.icon?.startsWith("my:") == true

    @Composable
    override fun Content(
        symbol: SymbolEntry,
        selected: Boolean,
        modifier: Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Column(modifier = modifier) {
            Text(symbol.label, color = MaterialTheme.colorScheme.primary)
            if (selected) Text("Selected", style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

A production renderer should connect the supplied callbacks to its interactive surface (for example, with Foundation `combinedClickable`) and provide accessibility semantics and adequate touch targets.

### 4.5 Custom group indicator

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.SymbolDrawerGroupIndicator
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

object MyGroupIndicator : SymbolDrawerGroupIndicator {
    override val id = "my-editor.group-indicator"

    @Composable
    override fun Content(
        groups: List<SymbolGroup>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier
    ) {
        Row(modifier = modifier) {
            groups.forEachIndexed { index, group ->
                Text(
                    text = group.title,
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { onSelect(index) }
                )
            }
        }
    }
}
```

### 4.6 Dynamic group provider

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolDrawerGroupProvider
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

class MyGroupProvider(
    private val repository: MyGroupSource
) : ComposeSymbolDrawerGroupProvider {
    override val id = "my-editor.generated-groups"
    override val priority = 10

    override suspend fun loadGroups(): List<SymbolGroup> =
        repository.loadSymbolGroups()
}
```

Providers are loaded from a Composition effect and cancelled when the effect leaves Composition. Host-supplied groups take precedence for duplicate IDs; providers fill missing IDs. A provider failure does not prevent host groups or other providers from being used.

### 4.7 Theme, events and settings extensions

- Theme: implement `ComposeSymbolDrawerThemeExtension.resolve(): ComposeDrawerTheme` and return a Material 3 `ColorScheme` through `ComposeDrawerTheme(colorScheme = ...)`.
- Events: implement `ComposeSymbolDrawerEventHandler.onEvent(event)` and register a `ComposeSymbolDrawerEventExtension`. Events are `SettingsChanged`, `GroupsChanged`, `GroupSelected`, `ExpansionChanged`, and `SymbolInvoked`.
- Settings tab: implement `ComposeSymbolDrawerSettingsTabExtension.Content(settings, onSettingsChange, modifier)`.
- Setting item: implement `ComposeSymbolDrawerSettingExtension.Content(settings, onSettingsChange, modifier)`.

These are real source declarations. They do not expose `onAttached`, `onDetached`, `onViewRecycled`, or other UI lifecycle callbacks.

## 5. Manager and settings routes

### 5.1 Manager route

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager.SymbolDrawerManagerRoute
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository

@Composable
fun MySymbolManager(
    repository: SymbolGroupRepository,
    modifier: Modifier = Modifier
) {
    SymbolDrawerManagerRoute(
        repository = repository,
        modifier = modifier
    )
}
```

The route currently includes group CRUD/reordering, symbol CRUD, bulk copy/move, JSON import/export and SAF document selection. The host supplies the repository.

### 5.2 Settings route

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.settings.SymbolDrawerSettingsRoute
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository

@Composable
fun MySymbolSettings(
    repository: SymbolDrawerSettingsRepository,
    modifier: Modifier = Modifier
) {
    SymbolDrawerSettingsRoute(
        repository = repository,
        modifier = modifier
    )
}
```

The settings state publishes a new value only after repository persistence succeeds. A failed write is exposed through `lastError` and shown by the route.

## 6. Repository, models and JSON

### Group repository

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

class MySymbolGroupRepository : SymbolGroupRepository {
    override fun loadGroups(): List<SymbolGroup> = loadFromMyDatabase()

    override fun saveGroups(groups: List<SymbolGroup>) {
        saveToMyDatabase(groups)
    }
}
```

### Core models

- `SymbolGroup(id: String, title: String, symbols: List<SymbolEntry>)`
- `SymbolEntry(id: Long, label: String, shortAction: SymbolAction, longAction: SymbolAction?, icon: String?, ...)`
- `SymbolAction` is a sealed interface covering insert, replace, surround selection, cursor movement, deletion, selection transforms, editor commands, clipboard, manager navigation, composite actions and extension actions.

Stable IDs must survive refreshes. Group IDs must be nonblank and unique; symbol IDs should remain unique and nonzero in persisted repositories.

### JSON interoperability

The Compose module retains the V2 typed-action and group-bundle JSON codecs plus legacy migration adapters. Import legacy data through the migration/adapter path; never serialize View objects or Activity state into JSON. The manager route decodes the complete bundle before committing one data snapshot.

## 7. State, recomposition and lifecycle-free UI

- Screens, drawers, renderers and indicators are Composables; callers do not manually create/start/stop/destroy them.
- Use `rememberSymbolDrawerState` or the page-specific `remember...State` helpers for UI state.
- Async providers run in Composition effects and are cancelled when the effect leaves Composition.
- Dynamic extension registration handles are managed by Composition effects; no View attach/detach callbacks are exposed.
- The public UI API does not require Activity/Fragment/ViewModel inheritance or a host-provided LifecycleOwner.
- Repositories and editor adapters belong to the host data/platform layer and remain under the host's data/editor ownership. This does not mean the host manually manages UI widget lifetimes.

## 8. Current Compose migration boundary

The module is still undergoing a complete migration. The native Compose APIs currently documented here include:
- `compose.SymbolDrawer`, `SymbolDrawerState`, `rememberSymbolDrawerState`
- Compose extension contracts and `ComposeSymbolDrawerExtensionRegistry`
- `compose.manager.SymbolDrawerManagerRoute/Screen`
- `compose.settings.SymbolDrawerSettingsRoute/Screen`
- Editor-neutral typed actions, `SymbolActionHost`, `SymbolActionExecutor`, and `SymbolTextDocument`

The following legacy View implementations have not yet been replaced and are not Compose public APIs:
- `SymbolDrawerView`, its listeners and instance methods such as `reloadGroups()`
- View-based manager/settings screens, adapters and indicators
- Legacy `DrawerContainer`, `DrawerController`, View panel/page hosts
- Legacy View extension registry
- Activity/LifecycleOwner-based picker bridges

The new public Generic Drawer facades still have historical typealias migration work. Do not import those legacy facades in new Compose integrations until their native Compose replacements are complete. Documentation will be updated against actual source; it will not promise classes that do not exist.

## 9. Documentation/source parity rules

Every public API change must update Chinese and English documentation together:
1. Identical type names, package names, registration methods, behavior and failure semantics.
2. Complete imports for every copyable code block.
3. Fully qualified class names on first introduction.
4. Clear status labels: implemented, experimental/internal, migrating, planned.
5. Every code sample must match current branch source. Host-owned example classes must be labeled as host code, never implied to be library classes.
6. Legacy View APIs and Compose APIs must be documented separately.

The old numbered pages 01–16 and their English counterparts were inherited from the View version. Some are historical View API references only and are not the Compose module's integration contract. Use this guide and current source as the authority.
