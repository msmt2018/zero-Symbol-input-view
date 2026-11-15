# 01 — Quick Start and Integration

## What the library provides

V2 is an Android View library for a symbol-input drawer, grouped paging, typed actions, management UI, settings, persistence and a reusable generic Drawer API. It does not depend on a specific editor engine. The host application adapts actions to its own editor.

## Gradle

Same repository:
```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

JitPack:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:<published-tag>")
}
```
Use an existing Git tag whose JitPack build succeeded. A branch name is not a release.

## Add the View

```xml
<com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
    android:id="@+id/symbol_drawer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

The View owns its internal group indicator, ViewPager2 and symbol-page adapter. Do not create a competing pager for the same groups.

## Submit data

```kotlin
val groups = listOf(
    SymbolGroup(
        id = "kotlin",
        title = "Kotlin",
        symbols = listOf(
            SymbolEntry(
                id = 1001L,
                label = "fun",
                shortAction = SymbolAction.InsertText("fun ")
            )
        )
    )
)
drawer.submitGroups(groups)
drawer.saveGroups()
```

Group IDs must be nonblank and unique. Symbol IDs must be unique within each group for submission; manager/storage operations require IDs to be globally unique and nonzero. Submit a new snapshot instead of mutating a previously submitted list.

## Connect an editor

```kotlin
drawer.onSymbolActionListener =
    SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
        val action = if (isLongPress) symbol.longAction else symbol.shortAction
        if (action != null) {
            when (val result = SymbolActionExecutor().execute(action, editorHost)) {
                SymbolActionResult.Executed -> Unit
                is SymbolActionResult.Unsupported -> showUnsupported()
                is SymbolActionResult.Invalid -> showError(result.reason)
                is SymbolActionResult.Failed -> report(result.cause)
                is SymbolActionResult.Cancelled -> Unit
                is SymbolActionResult.PartialFailure -> reportPartial(result)
            }
        }
    }
```

Implement `SymbolActionHost` for the active editor. The callback only reports a selected symbol; it does not prove that the editor accepted the edit.

## Management and settings entry points

```kotlin
drawer.onOpenManagerListener = { navigateToSymbolManager() }
drawer.onOpenSettingsListener = { navigateToSymbolSettings() }
```

If no settings listener is supplied, the View displays its built-in settings screen in a dialog. Manager and settings navigation are separate callbacks.

## Common methods

```kotlin
drawer.reloadGroups()
drawer.setExpanded(true, animate = true)
drawer.setExpanded(false, animate = true)
drawer.selectGroup(0, smoothScroll = true)
val index = drawer.getCurrentGroupIndex()
val expanded = drawer.isExpanded()
val groups = drawer.getGroups()
```

Call View and animation methods on the main thread.

## Lifecycle

Set listeners before enabling interaction. Release host-owned listeners, extension handles and manager bindings when the Fragment View or editor session ends. Do not retain an Activity Context or View after destruction. Validate rotation, process recreation, empty/corrupt data, write failures, paging and long-press behavior in the host application.
