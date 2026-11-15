# 12 — 扩展注册框架 API

公开扩展 API 包：`com.zerostudio.idea.widget.drawer.extension`（实现类型由原控件内部包提供，公开别名保持稳定）。控件入口：`com.zerostudio.idea.widget.drawer.SymbolDrawerView`。

本页描述 V2 的运行时扩展注册机制。扩展 API 的目标是让宿主添加自己的动作、指示器、设置 Tab、设置项和主题，并控制内置功能的可见性，而不需要修改 V2 源码。

推荐从公共包导入，不要在新接入代码中引用历史实现包：

```kotlin
import com.zerostudio.idea.widget.drawer.SymbolDrawerView
import com.zerostudio.idea.widget.drawer.extension.*
import com.zerostudio.idea.widget.drawer.action.SymbolAction
import com.zerostudio.idea.widget.drawer.action.SymbolActionId
import com.zerostudio.idea.widget.drawer.action.SymbolActionRegistry
import com.zerostudio.idea.widget.drawer.action.SymbolActionHandler
```

当前公共扩展类型通过 Kotlin `typealias` 暴露；这是 Kotlin 源码级兼容入口，不会生成可供 Java 直接导入的独立类。若需要 Java ABI 稳定性，后续必须把公共 API 声明物理迁移为真实 public 类型，而不能把 typealias 误当作 Java API。

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

### SymbolDrawerView.bindActionHost(host) / executeAction(action, host, cancellation)

这两个方法属于 `SymbolDrawerView`，不是注册表成员。`drawer.bindActionHost(host)` 返回 registry-backed editor-neutral Host；不会保存或持有传入的宿主对象。适合需要继续调用原有 `SymbolActionExecutor` 的集成方式。

`drawer.executeAction(action, host, cancellation)` 是控件提供的完整便捷入口：它使用原有 `SymbolActionExecutor`，并自动将 Host 包装为当前 Drawer session 的 registry-backed Host。扩展 handler、V1 数字 ID handler 和宿主原有 fallback 因而处于同一条执行链中。宿主仍需在原有符号回调中决定何时调用此方法；控件不持有编辑器对象，也不会擅自替换 `onSymbolActionListener`。

```kotlin
val result = drawer.executeAction(
    SymbolAction.Extension("com.example.format", payload),
    editorNeutralHost
)
```

取消令牌可作为第三个参数传入。扩展 handler 返回 `false` 时视为已匹配但拒绝执行，不会再调用 fallback；没有注册 handler 时才回退原 Host。

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

指示器组件只负责 UI 呈现。不得自己创建另一套符号页或持久化 group selection。宿主指示器容器应把 View 放入原指示器槽位，并在 groups/settings/page selection 改变时调用对应方法。控件在替换指示器前先创建并初始化新组件；新组件初始化或挂载失败时保留原指示器。组件的 `submitGroups`、`setSelectedIndex`、`applySettings`、`applyTheme` 回调抛异常时会报告扩展错误并恢复内置指示器；`onDispose` 抛异常不会阻断控件切换。组件离开窗口时应释放 animator、listener 等资源。

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

`SymbolDrawerSettingsTabExtension.id` 和 `SymbolDrawerSettingExtension.tabId` 是设置页面内部使用的 Tab 标识，应与目标设置容器中的 Tab ID 一致。它们与 `SymbolDrawerBuiltInFeatures` 的内置功能可见性 ID 属于不同命名空间，不能混用。设置扩展可以使用宿主自定义 ID；建议宿主定义常量，例如：

- 内置设置页的实际 `tabId` 为：`appearance`、`layout`、`tablet`、`interaction`、`performance-search`、`data`。
- 自定义设置 Tab 使用扩展自身的 `id`；该 Tab 内的设置项以相同 `tabId` 归属。

不要将示例名称当作内置 Tab ID；宿主可以自行扩展其它 Tab，但必须自行提供对应的设置页面。

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

Theme provider 应尽量保持纯解析行为，不要在 resolve 中修改 Window、写入持久化数据或持有 Activity。当前 `SymbolDrawerView.setThemeExtension` 已将 token 传入原有 `SymbolPageAdapter`：网格 surface、原生符号文字和设置图标会在主题变化后实时刷新；自定义单元格通过 `SymbolDrawerItemViewState.theme` 收到同一快照。抽屉外壳、内置分组指示器和自定义分组指示器也使用该快照。Manager 和 Settings 的完整 token 覆盖尚未完成，仍使用宿主主题解析器。

## 6. 内置功能隐藏策略

公共包 `com.zerostudio.idea.widget.drawer.extension` 提供 `SymbolDrawerBuiltInFeatures`，避免宿主重复手写 feature ID：

```kotlin
import com.zerostudio.idea.widget.drawer.extension.SymbolDrawerBuiltInFeatures

drawer.setBuiltInFeatureVisible(SymbolDrawerBuiltInFeatures.SEARCH, false)
val visible = drawer.isBuiltInFeatureVisible(SymbolDrawerBuiltInFeatures.SEARCH)
```

当前已接入原控件 UI 的稳定 ID：

| 常量 | ID | 原控件消费者 |
|---|---|---|
| `SEARCH` | `builtin.search` | Manager 搜索按钮与搜索状态 |
| `FAVORITES` | `builtin.favorites` | Manager 收藏筛选菜单 |
| `RECENT` | `builtin.recent` | Manager 最近筛选菜单 |
| `SETTINGS` | `builtin.settings` | 原分组指示器设置按钮 |
| `MANAGER` | `builtin.manager` | 原内置打开管理器动作 |

隐藏是逻辑可见性，不等于删除实现。被隐藏功能的数据和兼容性代码仍然保留；再次设为 true 即可恢复。以上 ID 是公共配置契约，发布后必须跨版本保持稳定。尚未接入原控件真实消费者的功能，不应仅为了预留而伪装成已支持的内置 ID。

## 7. 生命周期与线程安全

- 注册表内部集合操作使用同步保护。
- `snapshot()` 和各类 List getter 返回按 `priority`、`id` 排序的快照；Kotlin `Map`/`List` 暴露为只读接口，但不是深层不可变对象，扩展对象本身仍由注册者负责线程安全。
- `observe(emitCurrent = true)` 会在注册监听的调用线程同步发送初始快照；后续变更通知也在触发注册/注销/可见性修改的线程同步分发，不会自动切换到主线程。UI 观察者应自行 `post`/切换到主线程后更新 View。
- 注册表会隔离观察者抛出的异常，避免阻断注册/注销和其它观察者；当前实现不会把这类 observer 异常转发给 `onExtensionErrorListener`。需要诊断 observer 的宿主应在自己的回调内部记录异常。
- View factory 和 action handler 由宿主在正确线程调用；注册表不会自动切换线程。`SymbolActionExecutor` 同步调用 Host/handler，捕获 `Exception` 并返回 `SymbolActionResult.Failed`；`Error` 等 JVM 致命错误不会被转换，仍会向外传播。
- View / Activity / Editor 引用不应放进静态单例。
- 注册句柄应与创建它的 Editor session 或 Fragment view 生命周期一致。
- 注册表清空后，旧句柄 `close()` 是安全的；但清空后不应继续使用旧 session。`clear()` 会清除当前所有贡献和 action handler，并发出的变更通知会让已 attach 的 Drawer/设置页刷新。
- 注册的 ID 是持久化引用，不是可执行代码。进程重启后必须重新注册相同 ID 的 handler/provider。

## 8. 当前集成边界

当前内置消费路径：
- 自定义动作：`SymbolDrawerManagerScreen(extensionRegistry = drawer.extensions)` 将动作加入短按/长按动作选择器，并保存为 `SymbolAction.Extension`。
- 自定义指示器：`SymbolDrawerView.setCustomGroupIndicator(id)` 将组件放入真实指示器槽位；注册被移除/替换时，attached drawer 会恢复或重建组件。
- 自定义设置 Tab/设置项：`SymbolDrawerSettingsScreen(extensionRegistry = drawer.extensions)` 自动追加扩展 Tab，并按内置 tabId 注入设置项；View attached 时观察注册变化。
- 自定义主题：`SymbolDrawerView.setThemeExtension(id)` 应用抽屉外壳和分组指示器颜色，custom indicator 接收 theme 回调。
- `builtin.settings`：由 `SymbolDrawerView` 控制原内置设置入口。
- `builtin.manager`：由 `SymbolDrawerView` 控制原内置管理器动作入口。
- `builtin.search`：由 `SymbolDrawerManagerScreen` 控制原搜索按钮；隐藏时同时关闭搜索栏并清除搜索状态。
- `builtin.favorites` / `builtin.recent`：由 `SymbolDrawerManagerScreen` 控制原有筛选菜单项；隐藏当前正在使用的筛选时自动回退到“全部”。
- 管理页面可通过 `liveDrawer` 观察实时抽屉注册表；未绑定 live drawer 时，也可通过 `extensionRegistry` 单独观察同一会话注册表。观察者随 Manager View attach/detach 生命周期注册和释放。

仍未完全接入的范围：
- 主题 token 尚未覆盖符号单元格、Manager 和 Settings 的所有颜色。
- 自定义指示器 ID 目前由 `setCustomGroupIndicator` 运行时选择，尚未写入 `SymbolDrawerSettings` 或提供内置选择器。
- 批量管理等尚未接入的内置入口需要在确认原控件真实 UI/动作调用点后逐项接入，不能仅添加没有消费者的 feature ID。
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

默认情况下，扩展不改变触摸仲裁。适配器的触摸观察回调始终返回给原 View/RecyclerView/ViewPager2 流程，不得在回调中保存 MotionEvent。手势因滚动阈值、`ACTION_CANCEL`、多指触摸或 ViewHolder 回收而取消时，扩展会收到 `pressed=false` 的 `onStateChanged`；长按派发结束后也会复位 pressed 状态。若要启用长按扩展回调，覆盖 `wantsLongPress(item)`；该回调与 legacy `longAction` 共用同一个长按时序，不会另建手势识别器。

### 9.2 Legacy 分组提供器

`SymbolDrawerLegacyGroupProvider.loadGroups()` 返回的就是原有 `model.legacy.SymbolGroup` 列表。通过控件的 suspend API 加载：

```kotlin
val providerHandle = drawer.extensions.registerGroupProvider(provider)
val groups = drawer.reloadLegacyGroupsFromExtensions(persist = true)
```

提供器由 `priority`、`id` 排序。结果通过现有 `LegacySymbolDataCodec` 进入真实控件数据管线；持久化仍由控件原有 `SymbolGroupRepository` 负责。不要把 Provider 当成另一个常驻数据仓库。

### 9.3 生命周期回调

`SymbolDrawerLifecycleExtension` 可观察原控件的 attach/detach、设置变化、分组变化、当前分组变化、抽屉展开进度和符号调用。回调接收原始 Legacy 分组/符号记录及原有 `SymbolDrawerSettings`。所有回调由控件隔离扩展异常；扩展不得在回调中阻塞 UI 线程。

同一 `id` 的生命周期扩展被替换时，控件会先对旧实例调用 `onDetached`，再对新实例调用 `onAttached`。比较采用实例身份而不是 `equals()`，因此即使扩展实现是字段相同的数据类，也不会跳过资源切换。

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


### 公共 API 入口

应用代码优先从以下稳定包导入，不需要依赖内部实现包：

```kotlin
import com.zerostudio.idea.widget.drawer.SymbolDrawerView
import com.zerostudio.idea.widget.drawer.extension.SymbolDrawerItemViewExtension
import com.zerostudio.idea.widget.drawer.extension.SymbolDrawerExtensionRegistry
```

`SymbolDrawerView.extensions` 返回当前控件会话自己的注册表。所有扩展仍由原控件和原 `SymbolPageAdapter` 消费；公开包 facade 只是稳定 API 入口，不创建第二套控件或数据模型。
