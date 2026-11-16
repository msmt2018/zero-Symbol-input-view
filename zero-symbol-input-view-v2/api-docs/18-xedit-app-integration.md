# 18 — xedit-app 4.1-v2 集成实录

本文根据 `msmt2018/xedit-app` 的 main 分支当前源码编写，并与实际接入改动对应。它不是假设性的通用示例。

## 1. 先确认正确的宿主页面

xedit-app 的 Manifest 启动 Activity 是：

```kotlin
x.code.app.activity.MainActivity
```

它负责启动、存储权限检查和跳转到编辑器，不持有编辑器 View。实际编辑器页面是：

```kotlin
x.code.app.activity.EditorActivity
```

其布局为 `activity_editor.xml`，编辑器控件为 `binding.editor`，类型是项目自有的 `x.code.app.view.HighlightTextView`。旧版横向快捷键列表是 `binding.shortcutKeyView`。

因此，符号控件必须集成在 `EditorActivity`，不能集成到启动/权限页面 `MainActivity`。

## 2. Maven 依赖

xedit-app 的 app 模块：

```kotlin
dependencies {
    implementation("com.github.msmt2018:zero-Symbol-input-view:4.1-v2")
}
```

根目录 `settings.gradle.kts` 已经通过 `dependencyResolutionManagement.repositories` 配置 JitPack，并启用 `FAIL_ON_PROJECT_REPOS`。不要在 app 模块再次声明仓库。

该坐标是 JitPack 的仓库级聚合 artifact。多模块项目的根坐标与单独模块坐标不同；这里按你指定的 `4.1-v2` 根项目坐标接入。参见 [JitPack 多模块项目说明](https://docs.jitpack.io/building/#multi-module-projects)。

## 3. 实际布局

在 `app/src/main/res/layout/activity_editor.xml` 的编辑器内容区域，把原有快捷键 RecyclerView 与 V2 控件放在同一个底部纵向容器。旧快捷键功能继续保留：

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
        android:layout_height="wrap_content"
        android:overScrollMode="always" />

    <com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
        android:id="@+id/symbol_drawer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</LinearLayout>
```

不要移除旧的 `shortcut_key_view`：它仍由现有 `addShortcutKey()` 初始化。V2 控件是新增的独立符号抽屉，不替换原有编辑器控件。

## 4. 必要 import

以下是该接入路径使用的公开类型及真实包路径：

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
```

注意：`CursorTarget`、`DeleteKind`、`SelectionTransform` 和 `ClipboardOperation` 位于 `model` 包，不是 `action` 包。

## 5. EditorActivity 动作适配

xedit-app 当前已验证的编辑器写入 API 是：

```kotlin
binding.editor.insert(text)
```

先把 V2 的插入动作接入现有编辑器。没有实现的 Host 能力明确返回 `false`：

```kotlin
private fun editorSymbolActionHost() =
    object : SymbolActionHost {
        override fun insertText(template: String): Boolean {
            binding.editor.insert(template)
            return true
        }

        override fun replaceSelection(text: String): Boolean = false
        override fun surroundSelection(prefix: String, suffix: String): Boolean = false
        override fun moveCursor(target: CursorTarget): Boolean = false
        override fun delete(kind: DeleteKind): Boolean = false
        override fun transformSelection(transform: SelectionTransform): Boolean = false
        override fun executeEditorCommand(commandId: String, payload: String?): Boolean = false
        override fun performClipboard(operation: ClipboardOperation): Boolean = false
        override fun openManager(): Boolean = false
        override fun executeExtension(extensionId: String, payload: String?): Boolean = false
    }
```

在 `EditorActivity.onCreate()` 中绑定：

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

    onOpenManagerListener = {
        // 接入宿主自己的符号管理页面导航。
    }

    reloadGroups()
}
```

`executeAction` 是 `SymbolDrawerView` 的真实公开方法，会使用该 Drawer 自己的扩展注册表包装 Host，再调用 V2 原有 Executor。不要另建一套平行动作执行器。

## 6. 编辑器底部区域尺寸

符号抽屉可以展开/折叠，因此编辑器底部边距必须跟随整个底部容器的实际高度变化，不能只在 Activity 创建时读取旧快捷键栏高度：

```kotlin
val updateEditorBottomInset = {
    binding.editor.setMarginLayout(bottom = binding.bottomToolbars.height)
}
binding.bottomToolbars.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
    updateEditorBottomInset()
}
binding.bottomToolbars.post { updateEditorBottomInset() }
```

## 7. 生命周期与能力边界

Activity/Fragment 的 View 结束时应解除回调：

```kotlin
binding.symbolDrawer.onSymbolActionListener = null
binding.symbolDrawer.onOpenManagerListener = null
binding.symbolDrawer.onOpenSettingsListener = null
```

当前 xedit-app 适配只确认并接通了文本插入。以下能力尚未由此适配器实现：

- 替换选区、包围选区；
- 光标移动；
- 删除和选区转换；
- 剪贴板；
- 编辑器命令与扩展动作；
- 符号管理页面导航。

这些方法返回 `false` 是有意的能力声明。后续应先检查 `HighlightTextView` 及其父类 `EditorView` 的真实 API，再逐项实现并测试；不要使用伪造成功返回值。

## 8. 验收清单

- [ ] JitPack 依赖能够解析并成功构建。
- [ ] 原快捷键栏仍正常工作。
- [ ] V2 默认分组与符号网格正常显示。
- [ ] 短按和长按插入分别使用对应动作。
- [ ] 抽屉展开/折叠时，编辑器底部边距同步更新。
- [ ] Activity 销毁时释放回调。
- [ ] 未支持动作返回 Unsupported/false，不修改编辑器内容。
- [ ] 真实设备验证触摸冲突、键盘 Insets、旋转和大屏布局。
