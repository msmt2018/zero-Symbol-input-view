# 12 — 扩展注册框架 API

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.extension`

本页描述 V2 的运行时扩展注册机制。扩展 API 的目标是让宿主添加自己的动作、指示器、设置 Tab、设置项和主题，并控制内置功能的可见性，而不需要修改 V2 源码。

## 1. SymbolDrawerExtensionRegistry

每个 `SymbolDrawerView` 持有独立实例：

```kotlin
val registry: SymbolDrawerExtensionRegistry = drawer.extensions
```

注册表是 session-scoped、线程安全的运行时容器。它持有 provider、handler 和 View factory，不会将 lambda 或 View 写入 JSON。宿主必须在 Activity/Fragment/Editor session 生命周期内管理注册句柄，并在重建后重新注册。

### registerAction(extension): AutoCloseable

注册自定义动作贡献。动作通过稳定字符串 ID 标识；将该 ID 存入 `SymbolAction.Extension(extensionId, payload)`。

- 同 ID 再注册会替换旧贡献。
- 返回句柄关闭时，仅当该句柄仍拥有当前注册时才移除，旧句柄不会误删后注册的替代项。
- 动作 handler 也会同步注册到 `actionHandlers`。
- 自定义动作列表通过 `actions()` 获取。

### registerIndicator(extension): AutoCloseable

注册自定义分组指示器 provider。provider 通过 `indicators()` 进入扩展指示器列表。`SymbolDrawerView.setCustomGroupIndicator(id)` 会将组件放入真实指示器槽位，并同步 groups、page selection 和 settings。

### registerSettingsTab(extension): AutoCloseable

注册一个完整的自定义设置 Tab。`SymbolDrawerSettingsScreen` 在传入同一 session registry 时会自动追加该 Tab，并管理其页面 View。

### registerSetting(extension): AutoCloseable

注册一个自定义设置项。`SymbolDrawerSettingsScreen` 会按 `tabId` 将其插入对应内置设置页。

### registerTheme(extension): AutoCloseable

注册主题 Provider。通过 `themes()` 获取；`SymbolDrawerView.setThemeExtension(id)` 可将主题 token 应用到抽屉外壳和内置分组指示器。自定义指示器也会收到 `applyTheme` 回调。

### actions() / indicators() / settingsTabs() / settingsItems() / themes()

返回按 `priority`、再按 `id` 排序的快照。每次调用返回新的 List；不要把返回列表当作可修改的注册表。

### setBuiltInVisible(featureId, visible)

设置某个内置 feature ID 的运行时可见性。此操作只改变配置状态，不删除内置类、动作实现或持久化数据。

### isBuiltInVisible(featureId, defaultVisible = true)

读取内置 feature 的可见性。未显式设置时返回 defaultVisible。

### builtInVisibilitySnapshot()

返回不可变 Map 快照。注册表本身不负责跨进程持久化；宿主可保存该 Map，并在创建 Drawer 后恢复。

### wrapActionHost(delegate)

返回 `RegistryBackedSymbolActionHost` 装饰器。对 `SymbolAction.Extension`：
1. 先查找 V1 兼容数字动作注册。
2. 再查找 extension namespace 的 handler。
3. 找不到时调用 delegate.executeExtension fallback。
4. handler 已注册但返回 false 时不会再次调用 fallback，避免同一动作执行两次。

`SymbolDrawerView.bindActionHost(host)` 是该方法的快捷入口。

### clear()

清空该注册表的所有动作、指示器、设置 Tab、设置项、主题和可见性覆盖，并清空动作 handler registry。只在该 Drawer/session 确定不再使用扩展时调用。若使用独立 registration handles，仍建议逐一 close，以便管理自己的生命周期。

## 2. SymbolDrawerActionExtension

描述一个自定义动作：

| 属性 | 类型 | 说明 |
|---|---|---|
| id | String | 稳定扩展 ID，必须符合 ID 校验规则 |
| title | String | 非空展示名称 |
| description | String | 说明 |
| payloadHint | String? | 管理界面可显示的参数提示 |
| priority | Int | 列表排序权重，数值越小越靠前 |
| handler | SymbolActionHandler<SymbolActionHost> | 执行函数 |

### ID 规则

ID 长度为 1..256，只允许 ASCII 字母、数字以及 `_`、`.`、`:`、`/`、`-`，首字符必须为字母或数字。建议使用反向域名或产品命名空间，例如 `com.example.editor.formatSelection`。

### SymbolActionHandler<C>

```kotlin
fun interface SymbolActionHandler<C> {
    fun execute(context: C, payload: String?): Boolean
}
```

返回 true 表示执行成功；false 表示当前上下文拒绝或不支持。异常由调用方/动作执行流程捕获并转为失败结果。

## 3. SymbolGroupIndicatorExtension

自定义指示器 provider：

- `id`：稳定 ID。
- `title`：显示名称。
- `description`：说明。
- `priority`：列表排序。
- `create(context, initialState)`：创建一个指示器组件。

### SymbolGroupIndicatorState

- `groups`：创建时的分组快照。
- `selectedIndex`：当前选中页。
- `onSelectGroup(index)`：请求切换到指定组。必须使用有效 index；最终分页状态仍由 SymbolDrawerView 管理。

### SymbolGroupIndicatorComponent

- `view`：需要插入指示器区域的 View。
- `submitGroups(groups)`：更新分组快照。
- `setSelectedIndex(index)`：同步选中状态。
- `applySettings(settings)`：应用通用设置快照。
- `applyTheme(theme)`：可选回调，接收当前 drawer theme token；默认实现为空操作。
- `onDispose()`：可选释放钩子；组件被替换或移除时调用，默认实现为空操作。

指示器组件只负责 UI 呈现。不得自己创建另一套符号页或持久化 group selection。宿主指示器容器应把 View 放入原指示器槽位，并在 groups/settings/page selection 改变时调用对应方法。组件离开窗口时应释放 animator、listener 等资源。

## 4. 设置扩展

### SymbolDrawerSettingsTabExtension

- `id`：Tab 稳定 ID。
- `title`：Tab 标题。
- `priority`：排序。
- `createView(context, scope))：返回完整设置页 View。

### SymbolDrawerSettingExtension

- `id`：设置项稳定 ID。
- `tabId`：目标设置 Tab ID。
- `title`：设置项名称。
- `priority`：在目标 Tab 内排序。
- `createView(context, scope))：创建设置项 View。

### SymbolDrawerSettingsScope

- `current`：当前 `SymbolDrawerSettings` 快照。
- `update(transform))：把新快照提交给宿主设置管理器。

扩展设置可以只使用 V2 Settings 中已有字段，也可以把自己的字段保存在宿主存储中。不要把第三方扩展的任意 JSON 塞进 V2 固定 schema，避免版本升级时发生字段冲突。

### 设置 Tab ID 约定

当前已接入的内置 ID：`builtin.settings`（隐藏内置指示器的设置按钮）和 `builtin.manager`（阻止内置打开管理器动作）。其它 ID 可由宿主定义，并由宿主自己的 UI 查询 `isBuiltInVisible`。建议宿主定义常量，例如：

- `appearance`
- `layout`
- `interaction`
- `performance`
- `data`
- `editor`

注册项的 `tabId` 必须与宿主设置容器使用的 ID 一致。注册表负责贡献发现与排序；具体设置 UI 容器负责把返回的 View 放入对应页面。

## 5. 主题扩展

### SymbolDrawerThemeExtension

- `id`：稳定主题 ID。
- `title`：主题名称。
- `priority`：列表排序。
- `resolve(context, settings))：根据 Android Context 和当前设置返回颜色 token。

### SymbolDrawerTheme

所有颜色均为 Android ARGB Int：

- `primary`、`onPrimary`
- `surface`、`onSurface`
- `secondaryText`、`outline`
- `drawerBackground`，默认等于 surface。
- `selectedCellBackground`，默认等于 primary。

Theme provider 应尽量保持纯解析行为，不要在 resolve 中修改 Window、写入持久化数据或持有 Activity。宿主可将颜色 token 应用到其它表面；当前 `SymbolDrawerView.setThemeExtension` 已覆盖抽屉外壳和内置分组指示器，符号网格、Manager、Settings 的全量主题覆盖仍在后续接入。

## 6. 内置功能隐藏策略

`setBuiltInVisible` 采用稳定 feature ID。建议宿主集中定义 ID 常量，例如：

```kotlin
object DrawerFeatureIds {
    const val SEARCH = "builtin.search"
    const val FAVORITES = "builtin.favorites"
    const val RECENT = "builtin.recent"
    const val BATCH_MANAGEMENT = "builtin.batchManagement"
    const val SETTINGS = "builtin.settings"
}
```

使用：

```kotlin
drawer.extensions.setBuiltInVisible(DrawerFeatureIds.SEARCH, false)
val visible = drawer.extensions.isBuiltInVisible(DrawerFeatureIds.SEARCH)
```

隐藏是逻辑可见性，不等于删除实现。被隐藏功能的数据和兼容性代码仍然保留；再次设为 true 即可恢复。feature ID 是宿主定义的配置键，必须跨版本保持稳定。

## 7. 生命周期与线程安全

- 注册表内部集合操作使用同步保护。
- List getter 返回快照，调用方可在 UI 线程消费。
- View factory 和 action handler 由宿主在正确线程调用；注册表不会自动切换线程。
- View / Activity / Editor 引用不应放进静态单例。
- 注册句柄应与创建它的 Editor session 或 Fragment view 生命周期一致。
- 注册表清空后，旧句柄 close 是安全的；但清空后不应继续使用旧 session。
- 注册的 ID 是持久化引用，不是可执行代码。进程重启后必须重新注册相同 ID 的 handler/provider。

## 8. 当前集成边界

当前内置消费路径：
- 自定义动作：`SymbolDrawerManagerScreen(extensionRegistry = drawer.extensions)` 将动作加入短按/长按动作选择器，并保存为 `SymbolAction.Extension`。
- 自定义指示器：`SymbolDrawerView.setCustomGroupIndicator(id)` 将组件放入真实指示器槽位；注册被移除/替换时，attached drawer 会恢复或重建组件。
- 自定义设置 Tab/设置项：`SymbolDrawerSettingsScreen(extensionRegistry = drawer.extensions)` 自动追加扩展 Tab，并按内置 tabId 注入设置项；View attached 时观察注册变化。
- 自定义主题：`SymbolDrawerView.setThemeExtension(id)` 应用抽屉外壳和分组指示器颜色，custom indicator 接收 theme 回调。
- 内置功能可见性：`builtin.settings` 隐藏内置设置按钮；`builtin.manager` 阻止内置打开管理器动作。

仍未完全接入的范围：
- 主题 token 尚未覆盖符号单元格、Manager 和 Settings 的所有颜色。
- 自定义指示器 ID 目前由 `setCustomGroupIndicator` 运行时选择，尚未写入 `SymbolDrawerSettings` 或提供内置选择器。
- 其它内置功能 ID（搜索、收藏、最近使用、批量管理等）仍需对应 UI 入口逐项读取可见性。
- 注册表不会替宿主 Activity 创建导航、管理器或窗口；扩展 View 的生命周期仍由承载它的 UI 控制。

这些边界避免扩展注册表越权修改 Activity 或替换 V2 的 ViewPager2 分页状态。

## 9. 原控件符号单元格、Legacy 数据和生命周期扩展

以下扩展均由现有 `SymbolDrawerView.extensions` 注册，并由原有页面适配器、分页器和动作宿主消费。它们不创建独立符号列表、独立分页器或第二套符号数据模型。

### 9.1 自定义单元格渲染

公开入口位于 `com.zerostudio.idea.widget.drawer.extension`：

- `SymbolDrawerItemViewExtension`
- `SymbolDrawerItemViewState`

符号内容参数始终是原有 `model.legacy.SymbolItem`。自定义 View 只替换当前网格单元格的内容；单元格尺寸、RecyclerView 复用、短按/长按动作分发和分页滚动仍由原控件管理。

```kotlin
val rendererHandle = drawer.extensions.registerItemView(
    object : SymbolDrawerItemViewExtension {
        override val id = "com.example.drawer.operator-cell"
        override val title = "Operator cell"
        override val priority = 50

        override fun accepts(item: SymbolItem): Boolean =
            item.display.startsWith("operator:")

        override fun createView(
            context: Context,
            item: SymbolItem,
            state: SymbolDrawerItemViewState
        ): View = createOperatorCell(context)

        override fun bindView(
            view: View,
            item: SymbolItem,
            state: SymbolDrawerItemViewState
        ) {
            bindOperatorCell(view, item.display, state.enabled)
        }

        override fun onTouch(
            view: View,
            item: SymbolItem,
            state: SymbolDrawerItemViewState,
            event: MotionEvent
        ) {
            // 观察事件即可；不要 retain MotionEvent 或消费/拦截它。
        }

        override fun onClick(
            view: View,
            item: SymbolItem,
            state: SymbolDrawerItemViewState
        ) {
            // 额外 UI 反馈；原有短按动作仍由 Drawer 的 action host 执行。
        }

        override fun onViewRecycled(view: View, item: SymbolItem?) {
            releaseOperatorCell(view)
        }
    }
)
```

`accepts` 按注册优先级、ID 顺序查找，第一个接受该 item 的扩展生效。没有扩展接受、`createView` 失败或 `bindView` 抛出异常时，控件回退到原生单元格。替换/注销扩展后，页面会重新绑定可见项。ViewHolder 进入/离开窗口时分别调用 `onViewAttachedToWindow` / `onViewDetachedFromWindow`；替换 renderer 或 RecyclerView 回收时先执行 detach，再调用 `onViewRecycled`。这些方法均有默认空实现，旧扩展无需修改即可兼容。

默认情况下，扩展不改变触摸仲裁。适配器的触摸观察回调始终返回给原 View/RecyclerView/ViewPager2 流程，不得在回调中保存 MotionEvent。若要启用长按扩展回调，覆盖 `wantsLongPress(item)`；该回调与 legacy `longAction` 共用同一个长按时序，不会另建手势识别器。

### 9.2 Legacy 分组提供器

`SymbolDrawerLegacyGroupProvider.loadGroups()` 返回的就是原有 `model.legacy.SymbolGroup` 列表。通过控件的 suspend API 加载：

```kotlin
val providerHandle = drawer.extensions.registerGroupProvider(provider)
val groups = drawer.reloadLegacyGroupsFromExtensions(persist = true)
```

提供器由 `priority`、`id` 排序。结果通过现有 `LegacySymbolDataCodec` 进入真实控件数据管线；持久化仍由控件原有 `SymbolGroupRepository` 负责。不要把 Provider 当成另一个常驻数据仓库。

### 9.3 生命周期回调

`SymbolDrawerLifecycleExtension` 可观察原控件的 attach/detach、设置变化、分组变化、当前分组变化、抽屉展开进度和符号调用。回调接收原始 Legacy 分组/符号记录及原有 `SymbolDrawerSettings`。所有回调由控件隔离扩展异常；扩展不得在回调中阻塞 UI 线程。

### 9.4 Legacy 数字动作注册

对于 `SymbolItem.shortAction` / `longAction` 中保存的数字动作 ID，使用：

```kotlin
val actionHandle = drawer.extensions.registerLegacyAction(1201) { host, payload ->
    host.executeEditorCommand("com.example.command", payload)
}
```

payload 从原有 `shortText` / `longText` 传递，不需要转换成新的字符串动作模型。动作执行仍通过原有 `SymbolActionHost` 和 `SymbolActionRegistry`；未注册的动作按原宿主 fallback 处理。

### 9.5 注册句柄

每个注册方法都返回 `AutoCloseable`。句柄应绑定到宿主 Fragment 的 viewLifecycleOwner 或编辑器会话；View renderer 在注销后会由原页面适配器重新绑定，扩展自行释放其 View 持有的资源。不要在静态对象中持有 Activity、Drawer 或 View。

## 10. 设置扩展视图与主题的生命周期

设置扩展使用同一个 `SymbolDrawerSettings` 快照和设置仓库。通过 `SymbolDrawerSettingsScope.update` 修改设置时，仍由原设置页面校验、保存并通知宿主；扩展不应自行创建第二份设置持久化模型。

`SymbolDrawerSettingsTabExtension` 与 `SymbolDrawerSettingExtension` 均提供可选的 `onViewDisposed(view)`。设置页面在重新渲染或从窗口分离时调用该方法。扩展应在其中释放自己创建的 View 所持有的监听器、协程、订阅和动画；该回调可能因页面重建而多次发生，每个 View 实例只会对应一次释放调用。

```kotlin
object MyDrawerSettings : SymbolDrawerSettingExtension {
    override val id = "com.example.drawer.settings"
    override val tabId = "appearance"
    override val title = "自定义显示"

    override fun createView(
        context: Context,
        scope: SymbolDrawerSettingsScope
    ): View = createSettingsView(context, scope)

    override fun onViewDisposed(view: View) {
        // 解除该 View 注册的观察者并停止动画。
    }
}
```

主题提供器返回的 `SymbolDrawerTheme` 会沿真实 `SymbolDrawerView -> SymbolPageAdapter -> SymbolDrawerItemViewState` 路径传递。默认网格背景/文字和自定义 renderer 均可读取同一主题快照。主题变更会触发现有单元格重新绑定；扩展不需要自行观察一个平行主题仓库。


### Extension failure diagnostics

SymbolDrawerView.onExtensionErrorListener (also available through drawer.extensions.onExtensionErrorListener) receives an extension ID and its thrown error. Group-provider failures do not stop later providers from loading. Custom item renderer callbacks (accepts, createView, bindView, wantsLongPress, touch, click, long press and recycle) are isolated and reported through the same diagnostic sink. The host diagnostic callback is itself isolated; it must not be used as a control-flow mechanism.

### 单元格 View 窗口生命周期

自定义单元格扩展可选择实现以下可选回调：

- `onViewAttachedToWindow(view, item)`：扩展 View 随原有 RecyclerView ViewHolder 进入窗口。
- `onViewDetachedFromWindow(view, item)`：View 离开窗口，或 renderer 被替换前调用。
- `onViewRecycled(view, item)`：ViewHolder 回收/释放 renderer 时调用。

这些回调由原 `SymbolPageAdapter` 内部的符号子 Adapter 直接转发，不创建额外 RecyclerView。所有回调均有默认空实现。扩展应在 detach 时暂停仅在可见期间运行的动画/观察，在 recycle 时释放 View 实例持有的资源。若运行时替换 renderer，旧 renderer 会先收到 detach，再收到 recycle；新 renderer 在绑定完成且 View 已 attach 时收到 attach。
