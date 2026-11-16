# 01 — 快速开始与集成

## 1. 模块定位

V2 是 Android View 组件库，提供符号输入抽屉、分组分页、动作模型、管理 UI、设置 UI、持久化和独立通用 Drawer API。它不依赖某一种编辑器实现；宿主应用负责把动作桥接到自己的编辑器。

核心包：

```kotlin
com.zerostudio.idea.widget.editor.symbolinput.v2
```

主要类型位于子包 `model`、`action`、`data`、`settings`、`drawer` 和 `adaptive`。

### 最小集成所需的完整 import

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
```

`CursorTarget`、`DeleteKind`、`SelectionTransform` 和 `ClipboardOperation` 属于 `model` 包；不要从 `action` 包导入这些类型。

## 2. Gradle 依赖

### 同仓库集成

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

### JitPack 集成

若项目使用集中式仓库管理，在根目录 `settings.gradle.kts` 的 `dependencyResolutionManagement.repositories` 中添加：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

然后在 app 模块添加：

```kotlin
dependencies {
    implementation("com.github.msmt2018:zero-Symbol-input-view:4.1-v2")
}
```

Groovy DSL 可使用 `maven { url 'https://jitpack.io' }`。若启用了 `FAIL_ON_PROJECT_REPOS`，必须在 settings 文件配置仓库。请使用已发布且 JitPack 构建成功的 Git Tag；分支源码快照不代表已发布。

## xedit-app 实际接入参考

xedit-app 的 Manifest 启动入口 `x.code.app.activity.MainActivity` 是权限/启动页面；实际编辑器位于 `x.code.app.activity.EditorActivity`，编辑器实例为 `binding.editor`（`HighlightTextView`）。因此应将 `SymbolDrawerView` 添加到 `activity_editor.xml`，而不是启动页布局。

xedit-app 已有的文本插入调用为 `binding.editor.insert(text)`。动作回调通过 `drawer.executeAction(action, host)` 执行，宿主的 `SymbolActionHost.insertText(template)` 调用该插入方法并返回 true。当前未映射的选区、光标、删除、剪贴板和自定义命令能力必须返回 false，待对应编辑器 API 验证后再开放。

底部布局应把旧 `shortcut_key_view` 和 `symbol_drawer` 放进同一个纵向容器，并用容器总高度更新编辑器底部边距，避免控件覆盖编辑器内容。

## 3. 布局中放置组件

```xml
<com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
    android:id="@+id/symbol_drawer_v2"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

也可以在代码中创建：

```kotlin
val drawer = SymbolDrawerView(context)
container.addView(
    drawer,
    ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
)
```

该 View 自己负责内部指示器、ViewPager2 和符号页面布局。宿主不需要再创建一套分页 RecyclerView 来替代其内部结构。

## 4. 准备符号数据

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
            ),
            SymbolEntry(
                id = 1002L,
                label = "=>",
                shortAction = SymbolAction.InsertText(" -> "),
                longAction = SymbolAction.InsertText(" => ")
            )
        )
    ),
    SymbolGroup(
        id = "punctuation",
        title = "标点",
        symbols = listOf(
            SymbolEntry(
                id = 2001L,
                label = "()",
                shortAction = SymbolAction.InsertText("()")
            )
        )
    )
)

drawer.submitGroups(groups)
drawer.saveGroups()
```

约束：

- 分组 ID 必须非空且唯一。
- 每个分组内部的符号 ID 必须唯一；使用 `SymbolGroupManager` 时，符号 ID 还必须在整个仓储中全局唯一。
- 持久化仓储要求符号 ID 非零，建议始终使用正数或稳定生成的 Long。
- `submitGroups` 会校验分组 ID 和每个分组内的符号 ID；不满足条件会抛出 `IllegalArgumentException`。
- 不要把可变 List 在提交后继续修改；以新快照再次提交。

## 5. 连接编辑器动作

`SymbolDrawerView` 的回调只负责把用户选择的符号交给宿主，不会自行操作 Sora、EditText 或其他编辑器。

```kotlin
drawer.onSymbolActionListener =
    SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
        val action = if (isLongPress) symbol.longAction else symbol.shortAction
        if (action != null) {
            val result = actionExecutor.execute(action, editorActionHost)
            // 根据 result 展示错误或记录不支持的动作
        }
    }
```

宿主应实现 `SymbolActionHost`，并将每个方法映射到实际编辑器能力。详见 [04 — 动作注册与执行 API](04-action-runtime.md)。

## 6. 连接管理器入口

```kotlin
drawer.onOpenManagerListener = {
    // 使用宿主自己的导航打开管理页
}
drawer.onOpenSettingsListener = {
    // 可选：宿主自己导航到设置页
}
```

若不设置 `onOpenSettingsListener`，当前符号抽屉会创建内置设置界面并显示对话框。设置入口回调和管理入口回调是两个独立职责。

## 7. 常用控制

```kotlin
drawer.reloadGroups()                 // 从当前 repository 重新读取
drawer.setExpanded(true, animate = true)
drawer.setExpanded(false, animate = true)
drawer.selectGroup(1, smoothScroll = true)
val currentIndex = drawer.getCurrentGroupIndex()
val expanded = drawer.isExpanded()
val currentGroups = drawer.getGroups()
```

这些方法均作用于当前 View 实例。调用 View 更新/动画相关方法时，应在主线程调用。

## 8. 自定义 Repository

```kotlin
class AppSymbolRepository : SymbolGroupRepository {
    override fun loadGroups(): List<SymbolGroup> {
        // 从应用数据库或自己的数据层加载快照
        return emptyList()
    }

    override fun saveGroups(groups: List<SymbolGroup>) {
        // 持久化整个快照；建议在事务中完成
    }
}

drawer.setRepository(AppSymbolRepository(), reload = true)
```

`setRepository` 替换当前实例使用的数据源。传入 `reload = false` 时不会自动刷新当前显示数据；随后可显式调用 `reloadGroups()`。

## 9. 管理界面和设置界面

```kotlin
val manager = SymbolDrawerManagerScreen(context)
manager.onGroupsChanged = drawer::submitGroups
manager.onError = { error -> /* 显示错误 */ }
manager.bindToDrawer(drawer).use {
    // 在管理页可见期间保持绑定；退出时自动解除监听
}
```

```kotlin
val settings = SymbolDrawerSettingsScreen(context)
settings.onSettingsChanged = drawer::applySettings
settings.onError = { error -> /* 显示错误 */ }
```

管理界面和设置界面都是程序化 Android View，可放入 Activity、Fragment 或宿主自己的容器。不要在 Activity 销毁后继续持有旧 Context 或 View。

## 10. 生命周期建议

- 创建 View 后设置回调，再提交数据，避免首次交互前缺少动作宿主。
- Activity/Fragment 结束时解除宿主自己注册的观察者、动作注册句柄和 Manager 绑定。
- `SymbolDrawerView` 在 attach 时注册设置变化监听，在 detach 时注销；它还会在配置变更时刷新宿主主题。
- 如果宿主从管理界面返回并修改了数据，可调用 `onHostResume()` 或 `reloadGroups()`。
- 不要在每帧或每次触摸 MOVE 时重新调用 `submitGroups`；数据更新应按业务事务批量提交。
- 如果宿主编辑器有线程限制，必须在该编辑器要求的线程调用动作执行器。

## 11. 最小集成检查表

1. 依赖指向 V2 模块/已发布版本。
2. 布局中只放一个需要的 `SymbolDrawerView`。
3. 为每个组和符号分配稳定 ID。
4. 实现并接入动作宿主。
5. 为管理、设置入口设置宿主回调（或使用内置屏幕）。
6. 验证折叠、展开、横向分页、长按、编辑器插入、配置变化和进程重建。
7. 验证自定义 Repository 的写入失败、空数据和损坏数据恢复策略。
