# 18 — xedit-app 4.1-v2 Integration

This guide is based on the current `main` sources of `msmt2018/xedit-app` and the actual V2 integration changes.

## 1. Choose the real editor Activity

The manifest launcher `x.code.app.activity.MainActivity` handles storage permission and navigation. It does not own the editor.

The actual editor screen is `x.code.app.activity.EditorActivity`. Its editor view is `binding.editor` (`x.code.app.view.HighlightTextView`), and the existing shortcut row is `binding.shortcutKeyView`. Integrate the symbol drawer in `activity_editor.xml` and `EditorActivity`, not in the launcher screen.

## 2. Dependency and repository

In `app/build.gradle.kts`:

```kotlin
implementation("com.github.msmt2018:zero-Symbol-input-view:4.1-v2")
```

The root `settings.gradle.kts` already configures JitPack under `dependencyResolutionManagement.repositories` and uses `FAIL_ON_PROJECT_REPOS`. Do not add a second repository declaration to the app module.

This is the repository-level aggregate artifact coordinate. JitPack uses a different coordinate form for individual modules; this integration follows the requested root coordinate and `4.1-v2` tag. See the [JitPack multi-module guide](https://docs.jitpack.io/building/#multi-module-projects).

## 3. Layout

Keep the existing shortcut RecyclerView and place it with the V2 view in a vertical bottom container:

```xml
<LinearLayout
    android:id="@+id/bottom_toolbars"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom"
    android:orientation="vertical">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/shortcut_key_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
        android:id="@+id/symbol_drawer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</LinearLayout>
```

## 4. Public imports

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
```

The four operation types are in the `model` package, not the `action` package.

## 5. Host adapter

The existing verified insertion API is `binding.editor.insert(text)`. The current integration intentionally implements only text insertion; unsupported capabilities return false:

```kotlin
private fun editorSymbolActionHost() =
    object : SymbolActionHost {
        override fun insertText(template: String): Boolean {
            binding.editor.insert(template)
            return true
        }

        override fun replaceSelection(text: String) = false
        override fun surroundSelection(prefix: String, suffix: String) = false
        override fun moveCursor(target: CursorTarget) = false
        override fun delete(kind: DeleteKind) = false
        override fun transformSelection(transform: SelectionTransform) = false
        override fun executeEditorCommand(commandId: String, payload: String?) = false
        override fun performClipboard(operation: ClipboardOperation) = false
        override fun openManager() = false
        override fun executeExtension(extensionId: String, payload: String?) = false
    }
```

Bind the view in `EditorActivity.onCreate()`:

```kotlin
binding.symbolDrawer.apply {
    onSymbolActionListener =
        SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
            val action = if (isLongPress) symbol.longAction else symbol.shortAction
            if (action != null) {
                val result = executeAction(action, editorSymbolActionHost())
                if (result !is SymbolActionResult.Executed) {
                    Log.w(LOG_TAG, "Symbol action was not executed: $result")
                }
            }
        }
    reloadGroups()
}
```

`executeAction` is the actual public method on `SymbolDrawerView`. It uses the drawer's session extension registry and the original V2 executor. Do not create a parallel action engine.

## 6. Track the expanded drawer height

The editor's bottom inset must track the entire bottom container as the drawer expands and collapses:

```kotlin
val updateEditorBottomInset = {
    binding.editor.setMarginLayout(bottom = binding.bottomToolbars.height)
}
binding.bottomToolbars.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
    updateEditorBottomInset()
}
binding.bottomToolbars.post { updateEditorBottomInset() }
```

## 7. Lifecycle and current limits

Clear the view callbacks when the Activity is destroyed:

```kotlin
binding.symbolDrawer.onSymbolActionListener = null
binding.symbolDrawer.onOpenManagerListener = null
binding.symbolDrawer.onOpenSettingsListener = null
```

Only insertion is currently wired to the xedit-app editor. Selection replacement, cursor movement, deletion, clipboard, editor commands, extension actions and manager navigation remain future adapter work. Do not return success for operations that the host has not implemented. Inspect the real `HighlightTextView` / `EditorView` API before adding each capability.

## 8. Verification checklist

- Resolve the JitPack dependency and build the app.
- Keep the original shortcut row working.
- Verify default symbol groups, short press and long press.
- Verify the editor inset follows drawer expansion and collapse.
- Verify unsupported actions do not modify the document.
- Test keyboard Insets, rotation, nested scrolling and large-screen layouts on devices.
