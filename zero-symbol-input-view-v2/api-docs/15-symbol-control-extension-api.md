# 15 — 符号控件扩展 API 实战参考

本篇只讲如何扩展仓库中**现有的 SymbolDrawerView、SymbolPageAdapter、分组指示器、管理页和设置页**。扩展不是重新实现一套工具栏：原控件仍拥有分页、触摸仲裁、网格尺寸、动作分发、数据和回收流程。

## 1. 入口与公共包

```kotlin
import com.zerostudio.idea.widget.drawer.SymbolDrawerView
import com.zerostudio.idea.widget.drawer.extension.*
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolActionHost
```

符号模型和部分产品 API 仍位于 `com.zerostudio.idea.widget.editor.symbolinput.v2.model` 等 V2 产品包。通用 Drawer 公共 facade 与符号输入产品命名空间是不同层级。

## 2. 扩展注册表的生命周期

每个 `SymbolDrawerView` 有独立的 `extensions` 注册表。它是运行时 session 容器，不会序列化 View、lambda 或 handler。注册方法返回 `AutoCloseable`：

```kotlin
class EditorFragment : Fragment() {
    private var actionRegistration: AutoCloseable? = null
    private var itemRegistration: AutoCloseable? = null

    override fun onViewCreated(view: View, state: Bundle?) {
        val drawer = view.findViewById<SymbolDrawerView>(R.id.symbolDrawer)
        actionRegistration = drawer.extensions.registerAction(/* extension */)
        itemRegistration = drawer.extensions.registerItemView(/* renderer */)
    }

    override fun onDestroyView() {
        actionRegistration?.close()
        itemRegistration?.close()
        actionRegistration = null
        itemRegistration = null
        super.onDestroyView()
    }
}
```

如果注册对象属于编辑器 Session 而非 Fragment View，应由 Session owner 持有句柄，并在 Session 结束时关闭。不要在静态单例中保存 Activity、Drawer、View 或编辑器对象。进程重建后必须重新注册可执行回调。

注册表支持 snapshot/observe/revision。观察者收到快照，不应直接修改内部集合；observe 返回的句柄也必须释放。

## 3. 自定义动作：注册、保存、执行

动作扩展定义：

- `id`：稳定唯一 ID，建议反向域名。
- `title` / `description`：动作选择器展示信息。
- `payloadHint`：可选参数提示。
- `priority`：列表顺序，数值越小越靠前。
- `handler`：执行回调，接收 editor-neutral host 和 payload。

```kotlin
val actionHandle = drawer.extensions.registerAction(
    SymbolDrawerActionExtension(
        id = "com.example.editor.formatSelection",
        title = "Format selection",
        description = "Format selected source using the host formatter",
        payloadHint = "Optional formatter profile",
        priority = 50,
        handler = SymbolActionHandler { host, payload ->
            host.executeEditorCommand("com.example.editor.formatSelection", payload)
        }
    )
)
```

符号中保存的是可序列化动作引用，而不是 lambda：

```kotlin
val action = SymbolAction.Extension(
    extensionId = "com.example.editor.formatSelection",
    payload = "compact"
)
```

管理页必须使用同一个 drawer session registry：

```kotlin
val manager = SymbolDrawerManagerScreen(
    context = context,
    extensionRegistry = drawer.extensions
)
```

这样自定义动作才会进入短按/长按动作选择器。运行时执行：

```kotlin
val result = drawer.executeAction(action, editorNeutralHost)
```

也可用 `drawer.bindActionHost(host)` 装饰原 Host，再交给既有 `SymbolActionExecutor`。扩展 handler 返回 false 表示已匹配但拒绝执行，不会再次调用 fallback；没有注册 handler 时才会回退到宿主的 `executeExtension`。不要在 handler 和 fallback 中重复执行同一动作。

## 4. 原有编辑器动作边界

`SymbolActionHost` 是同步接口。宿主负责把调用映射到当前获得焦点的文档/编辑器：

- insertText / replaceSelection / surroundSelection
- moveCursor / delete / transformSelection
- executeEditorCommand / performClipboard
- openManager / executeExtension

接口调用运行在调用线程，不会自动切主线程。若编辑器只允许主线程操作，必须在主线程调用 Executor；不要把动作投递到异步队列后立即返回 true。

`SymbolTextDocument` 是可选的更细粒度文档适配接口，使用 UTF-16 offset、左闭右开 range 和 0-based line/column。实现它后可使用 `SymbolTextDocumentActionHost` 复用通用插入、选区、光标、删除和内置文本命令。剪贴板、管理器导航和编辑器专属命令仍由宿主注入。

## 5. 自定义符号单元格渲染

使用 `registerItemView` 注册 renderer。它由现有 `SymbolPageAdapter` 消费，不会创建新的 RecyclerView 或符号页面。

```kotlin
val rendererHandle = drawer.extensions.registerItemView(
    object : SymbolDrawerItemViewExtension {
        override val id = "com.example.drawer.operator-cell"
        override val title = "Operator cell"
        override val priority = 50

        override fun accepts(item: LegacySymbolItem): Boolean =
            item.display.startsWith("operator:")

        override fun createView(
            context: Context,
            item: LegacySymbolItem,
            state: SymbolDrawerItemViewState
        ): View = TextView(context)

        override fun bindView(
            view: View,
            item: LegacySymbolItem,
            state: SymbolDrawerItemViewState
        ) {
            (view as TextView).apply {
                text = item.display.removePrefix("operator:")
                isEnabled = state.enabled
            }
        }

        override fun onClick(
            view: View,
            item: LegacySymbolItem,
            state: SymbolDrawerItemViewState
        ) {
            // Optional visual feedback only.
            // Original action dispatch remains owned by SymbolDrawerView.
        }

        override fun onViewRecycled(view: View, item: LegacySymbolItem?) {
            // Release renderer-owned resources.
        }
    }
)
```

规则：

1. `accepts` 按 priority、id 顺序检查；第一个接受项的 renderer 生效。
2. `createView` 返回未挂载的新 View。
3. `bindView` 必须完整绑定当前 item/state，不要依赖上一个 ViewHolder 的残留状态。
4. 原控件仍拥有 cell 尺寸、触摸、点击、长按、滚动、无障碍和回收。
5. 没有 renderer 接受或 renderer 创建/绑定失败时，回退原生单元格。
6. `onTouch` 是观察回调，不要 retain MotionEvent 或擅自拦截触摸。
7. `onViewAttachedToWindow` / `onViewDetachedFromWindow` / `onViewRecycled` 用于资源生命周期；扩展注销后可见项会重新绑定。

`SymbolDrawerItemViewState` 提供当前启用状态、按压/选中状态及主题等显示快照；以当前源码声明为准，不要自行缓存跨绑定状态。

## 6. 自定义分组指示器（替换真正的指示器）

分组指示器是**分组页面导航 UI**，不是给符号 Tab 换背景。原有 ViewPager2 仍是唯一分页状态所有者。

```kotlin
val indicatorHandle = drawer.extensions.registerIndicator(
    object : SymbolGroupIndicatorExtension {
        override val id = "com.example.drawer.segmented"
        override val title = "Segmented navigation"
        override val priority = 100

        override fun create(
            context: Context,
            initialState: SymbolGroupIndicatorState
        ): SymbolGroupIndicatorComponent =
            MyIndicator(context, initialState)
    }
)

val applied = drawer.setCustomGroupIndicator("com.example.drawer.segmented")
if (!applied) {
    // Unknown ID or creation/initialization failed; built-in indicator remains.
}
```

组件职责：

- 提供 `view`。
- 接收 `submitGroups(groups)`。
- 接收 `setSelectedIndex(index)`。
- 接收 `applySettings(settings)` 和可选 `applyTheme(theme)`。
- 用户选择时调用 `initialState.onSelectGroup(index)`。
- 在 `onDispose()` 释放动画和监听器。

不得在自定义指示器内部创建第二个 ViewPager、另一个 group repository 或独立 selectedIndex 真值。新组件初始化/挂载失败时保留原指示器；运行时回调失败会报告扩展错误并回退内置指示器。注销/替换扩展后，attached drawer 会更新当前组件。

恢复内置指示器：

```kotlin
drawer.useBuiltInGroupIndicator()
```

当前自定义 indicator ID 是运行时选择状态，尚未作为内置设置字段持久化；需要跨进程恢复时由宿主保存 ID，并在注册后重新调用 setCustomGroupIndicator。

## 7. 自定义设置 Tab 和设置行

完整 Tab：

```kotlin
drawer.extensions.registerSettingsTab(
    object : SymbolDrawerSettingsTabExtension {
        override val id = "com.example.editor.advanced"
        override val title = "Editor"
        override val priority = 80

        override fun createView(
            context: Context,
            scope: SymbolDrawerSettingsScope
        ): View = createEditorSettings(context, scope)
    }
)
```

插入既有 Tab 的设置行：

```kotlin
drawer.extensions.registerSetting(
    object : SymbolDrawerSettingExtension {
        override val id = "com.example.editor.autoPair"
        override val tabId = "interaction"
        override val title = "Auto pair"
        override val priority = 30

        override fun createView(
            context: Context,
            scope: SymbolDrawerSettingsScope
        ): View = createAutoPairPreference(context, scope)
    }
)
```

设置页需绑定相同 registry：

```kotlin
val settingsView = SymbolDrawerSettingsScreen(
    context = context,
    extensionRegistry = drawer.extensions
)
```

`scope.current` 是当前 V2 设置快照；`scope.update { old -> old.copy(...) }` 走现有设置更新路径。扩展自己的专有字段应由宿主存储，不要擅自塞入 V2 固定 JSON schema。当前已接入的内置 tabId 包括 appearance、layout、tablet、interaction、performance-search、data；扩展自定义 Tab 使用自己的稳定 ID。

## 8. 主题扩展

```kotlin
drawer.extensions.registerTheme(
    object : SymbolDrawerThemeExtension {
        override val id = "com.example.editor.midnight"
        override val title = "Midnight"

        override fun resolve(
            context: Context,
            settings: SymbolDrawerSettings
        ) = SymbolDrawerTheme(
            primary = 0xFF80CBC4.toInt(),
            onPrimary = 0xFF00201D.toInt(),
            surface = 0xFF101820.toInt(),
            onSurface = 0xFFE0F2F1.toInt(),
            secondaryText = 0xFF9BB7B3.toInt(),
            outline = 0xFF526B67.toInt()
        )
    }
)

drawer.setThemeExtension("com.example.editor.midnight")
// Restore host-derived theme:
drawer.setThemeExtension(null)
```

颜色使用 Android ARGB Int。当前 provider 已应用于抽屉外壳和分组指示器，自定义指示器也接收 theme snapshot。不要宣称 manager/settings 全部颜色、所有 cell 的所有样式都由该 provider 完全接管；完整覆盖范围以源码实际消费者为准。

## 9. Legacy group provider 与数据互通

若宿主已有旧版 `model.legacy.SymbolGroup` / `SymbolItem`，可注册 `SymbolDrawerLegacyGroupProvider`：

```kotlin
val providerHandle = drawer.extensions.registerGroupProvider(provider)
val loaded = drawer.reloadLegacyGroupsFromExtensions(persist = true)
```

provider 输出经过 `LegacySymbolDataCodec` 进入现有数据管线，持久化仍由控件的 `SymbolGroupRepository` 负责。不要把 provider 当作第二个常驻 repository。也可通过 `submitLegacyGroups(groups)` 提交旧模型数据。

## 10. V1 数字动作 ID 兼容

```kotlin
val legacyActionHandle = drawer.extensions.registerLegacyAction(1201) { host, payload ->
    host.executeEditorCommand("com.example.editor.legacyCommand", payload)
}
```

数字 ID 在注册边界保留原值；payload 从旧模型的 shortText/longText 传递。新扩展优先使用稳定字符串 ID 与 `SymbolAction.Extension`。不要为了兼容而重写原有动作执行链或把所有动作转换成另一套 ID 系统。

## 11. 内置功能可见性

使用稳定常量，不要在宿主散落硬编码字符串：

```kotlin
drawer.setBuiltInFeatureVisible(SymbolDrawerBuiltInFeatures.SEARCH, false)
val showSearch = drawer.isBuiltInFeatureVisible(
    SymbolDrawerBuiltInFeatures.SEARCH,
    defaultVisible = true
)
```

当前已接入的消费者包括 SEARCH、FAVORITES、RECENT、SETTINGS、MANAGER。隐藏只改变运行时可见性，不删除功能实现或用户数据。注册表的 visibility snapshot 可由宿主保存并在 Drawer 创建后恢复；注册表自身不承诺跨进程持久化。

## 12. 注册表观察与扩展快照

`snapshot()` 返回当前贡献快照，`revision()` 可用于缓存失效/比较，`observe(emitCurrent, listener)` 注册变更监听并返回关闭句柄。快照包含动作、指示器、设置 Tab/行、主题和内置可见性覆盖。

注册表是同步保护的运行时集合，但 View factory、action handler 和 UI 更新必须遵循宿主线程要求。不要在回调中做阻塞 I/O。观察者异常会被注册表隔离，但不会通过 `onExtensionErrorListener` 上报；请在自己的 observer 中记录诊断信息。动作执行同步运行于调用线程，Host 抛出的 `Exception` 会成为 `SymbolActionResult.Failed`，JVM `Error` 不会被吞掉。

## 13. 错误语义

| 场景 | 预期处理 |
|---|---|
| 重复 ID 注册 | 新注册替换同 ID 当前贡献；旧 handle close 不得误删新项 |
| 未知动作 handler | 回退宿主原始 executeExtension |
| handler 返回 false | 视为已匹配但拒绝，不再重复 fallback |
| renderer 不接受 item | 继续查找下一项/回退原生 cell |
| renderer create/bind 失败 | 隔离错误并回退原生 cell |
| indicator ID 不存在/创建失败 | 返回 false，保留当前内置指示器 |
| indicator 回调抛异常 | 报告扩展错误并回退内置指示器 |
| registry observer 抛异常 | 隔离异常，不中断其它观察者；由 observer 自行记录，不进入扩展错误监听器 |
| 扩展注销 | 关闭句柄；原控件刷新/重新绑定其真实消费者 |
| Activity/Fragment View 销毁 | 释放注册句柄、observer、listener 和 View 资源 |

## 14. 扩展开发原则

- 扩展已有控件，不复制控件。
- ViewPager2 是唯一分页状态所有者。
- 原控件适配器拥有触摸、点击、长按、滚动、尺寸、回收和无障碍流程。
- 扩展动作只存稳定 ID + payload，不能持久化函数对象。
- 数据提供器进入原 repository/codec 管线，不创建平行数据源。
- 自定义设置不要污染固定设置 schema。
- 生命周期句柄必须由明确 owner 管理。
- 对扩展异常做隔离，但不要吞掉宿主必须知道的动作失败。
