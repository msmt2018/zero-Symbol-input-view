# Compose V2 API 开发与集成手册（权威版本）

> 本手册只描述 `zero-symbol-input-view-v2-compose` 当前分支中已经存在的原生 Jetpack Compose API。所有代码示例都给出完整 import。若某个能力仍处于迁移阶段，会明确标记为“尚未提供 Compose 公共 API”，不会用旧 View API 伪装成 Compose API。

## 1. 模块、依赖和包名

模块目录：`zero-symbol-input-view-v2-compose`

同仓库依赖：

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2-compose"))
}
```

发布坐标由 Compose 模块的 Maven Publication 定义：
`com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2-compose:<published-version>`。
只有已发布 Tag 才能作为依赖版本，不能把分支名当成已发布版本。

当前 Compose 符号控件的主要包：

| 用途 | 完整包名 |
|---|---|
| Drawer、State、Defaults、扩展 | `com.zerostudio.idea.widget.editor.symbolinput.v2.compose` |
| Compose 管理页 | `com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager` |
| Compose 设置页 | `com.zerostudio.idea.widget.editor.symbolinput.v2.compose.settings` |
| 符号数据模型 | `com.zerostudio.idea.widget.editor.symbolinput.v2.model` |
| 动作执行与编辑器适配 | `com.zerostudio.idea.widget.drawer.action` |
| 分组仓储 | `com.zerostudio.idea.widget.editor.symbolinput.v2.data` |
| 设置模型与仓储 | `com.zerostudio.idea.widget.editor.symbolinput.v2.settings` |

不要从旧 View 模块文档复制 `SymbolDrawerView`、`SymbolDrawerView.OnSymbolActionListener`、`reloadGroups()`、`setRepository()`、`bindToDrawer()` 等调用到 Compose 项目。

## 2. 最小可运行接入：符号抽屉

### 2.1 完整 import

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

### 2.2 准备数据并显示控件

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

说明：
- `SymbolDrawer` 是 Composable，不创建 View，不需要手动 attach/detach。
- `rememberSymbolDrawerState()` 在 Composition 中保存抽屉展开状态和选中的稳定分组 ID。
- `groups` 是不可变数据快照；内容改变时传入新 List，不要原地修改原集合。
- 符号点击回调只把符号交给宿主；动作执行由宿主编辑器适配器负责。
- 长按没有单独动作时，控件内部选择长按动作或回退到短按动作；宿主示例也应保持同一策略。
- `onOpenManager`、`onOpenSettings` 是宿主导航回调，不会自动替宿主创建 Activity/Screen。

## 3. 接入编辑器动作引擎

### 3.1 API 类型和 import

```kotlin
import com.zerostudio.idea.widget.drawer.action.SymbolActionExecutor
import com.zerostudio.idea.widget.drawer.action.SymbolActionHost
import com.zerostudio.idea.widget.drawer.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
```

`SymbolActionHost` 是宿主必须实现的编辑器能力接口：

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

上述接口中使用的模型类型位于 `com.zerostudio.idea.widget.editor.symbolinput.v2.model`。完整 import：

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
```

### 3.2 在 Compose 中执行动作

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

`SymbolActionExecutor.execute(action, host)` 是同步调用，运行在线程调用者所在的线程。必须遵守编辑器自身的线程约束。结果类型包括：
- `Executed`：动作成功。
- `Unsupported`：宿主不支持该动作。
- `Invalid`：动作数据或复合动作超出验证约束。
- `Cancelled`：协作取消。
- `Failed`：宿主执行抛出异常。
- `PartialFailure`：复合动作部分完成后失败。

不要把“回调已经触发”当作编辑成功；必须检查 `SymbolActionResult`。

### 3.3 使用通用文本文档适配器

如果编辑器能提供文本快照、UTF-16 偏移、选区、光标和替换操作，可实现 `SymbolTextDocument`，然后复用 `SymbolTextDocumentActionHost` 的通用插入、替换、包围选区、光标移动、删除和大小写转换行为。

```kotlin
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocument
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocumentActionHost
import com.zerostudio.idea.widget.drawer.action.SymbolTextClipboard

// MyEditorDocumentAdapter 由宿主实现 SymbolTextDocument 的全部抽象属性和方法。
val actionHost = SymbolTextDocumentActionHost(
    document = MyEditorDocumentAdapter(editor),
    clipboard = SymbolTextClipboard { document, operation ->
        // 在宿主中实现真实剪贴板能力
        editorClipboard.perform(document, operation)
    },
    openManagerAction = { navigator.openSymbolManager(); true },
    extensionAction = { id, payload -> editorExtensions.execute(id, payload) }
)
```

`SymbolTextDocument` 的所有 offset 均为 JVM String 使用的 UTF-16 offset。实现必须保证 `replace` 成功后文档文本和选区状态一致。剪贴板、管理页导航和宿主自定义扩展仍由应用注入。上面 `MyEditorDocumentAdapter`、`editorClipboard`、`navigator`、`editorExtensions` 是宿主示意对象，不是库内置类。

## 4. 扩展注册框架

### 4.1 当前扩展类别

所有扩展类型都在：

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.*
```

当前源码提供以下 8 类扩展：

| 注册方法 | 扩展类型 | 消费位置 |
|---|---|---|
| `registerAction` | `ComposeSymbolDrawerActionExtension` | 符号动作分发 |
| `registerItemRenderer` | `SymbolDrawerItemRenderer` | 符号格子自定义渲染 |
| `registerGroupIndicator` | `SymbolDrawerGroupIndicator` | 分组指示器 |
| `registerGroupProvider` | `ComposeSymbolDrawerGroupProvider` | 动态提供分组 |
| `registerTheme` | `ComposeSymbolDrawerThemeExtension` | Material 3 ColorScheme |
| `registerEvent` | `ComposeSymbolDrawerEventExtension` | 设置/分组/选择/展开/符号事件 |
| `registerSettingsTab` | `ComposeSymbolDrawerSettingsTabExtension` | 设置页 Tab 内容 |
| `registerSetting` | `ComposeSymbolDrawerSettingExtension` | 设置页项目内容 |

所有扩展 ID 必须唯一且符合 `[A-Za-z0-9][A-Za-z0-9_.:/-]{0,255}`。优先级按高到低排序，同优先级按 ID 排序。注册同 ID 的新扩展会替换旧值；旧注册句柄不能误删后来注册的新值。

### 4.2 注册表和 Composition 自动清理

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

上例中的 `myActionExtension` 等实例需要由宿主按下面的接口实现。注册句柄由 `DisposableEffect` 在组合退出时统一关闭；调用者不需要创建 View 生命周期监听器。也可以通过不可变的 `ComposeSymbolDrawerExtensions` 参数直接传入静态扩展快照，不使用动态注册表。

### 4.3 自定义动作扩展

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolActionHandler
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolDrawerActionExtension

val myActionExtension = ComposeSymbolDrawerActionExtension(
    id = "my-editor.format",
    title = "Format",
    priority = 100,
    handler = ComposeSymbolActionHandler { action, symbol ->
        // 返回 true 表示该扩展已处理；false 表示继续交给宿主点击回调。
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

`SymbolAction` 的 import：

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
```

### 4.4 自定义符号格子

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

渲染器收到点击和长按回调，应将它们绑定到实际交互表面。上面仅展示类型和参数；生产实现应使用 Compose Foundation 的 `combinedClickable`，并提供语义和触控目标。

### 4.5 自定义分组指示器

```kotlin
import androidx.compose.foundation.layout.Row
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

此示例还需要以下 import：

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
```

### 4.6 动态分组 Provider

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

控件在 Composition 的 effect 中加载 Provider；取消组合时协程会被取消。宿主提供的 `groups` 对同 ID 分组具有优先权，Provider 用于补充缺失 ID。Provider 异常不会阻止宿主分组和其他 Provider 工作。

### 4.7 主题、事件和设置扩展

- 主题：实现 `ComposeSymbolDrawerThemeExtension.resolve(): ComposeDrawerTheme`，通过 `ComposeDrawerTheme(colorScheme = ...)` 提供 Material 3 `ColorScheme`。
- 事件：实现 `ComposeSymbolDrawerEventHandler.onEvent(event)`，注册 `ComposeSymbolDrawerEventExtension`。事件包括 `SettingsChanged`、`GroupsChanged`、`GroupSelected`、`ExpansionChanged`、`SymbolInvoked`。
- 设置 Tab：实现 `ComposeSymbolDrawerSettingsTabExtension.Content(settings, onSettingsChange, modifier)`。
- 设置项：实现 `ComposeSymbolDrawerSettingExtension.Content(settings, onSettingsChange, modifier)`。

这些接口均为源码中真实存在的类型。它们不提供 `onAttached`、`onDetached`、`onViewRecycled` 等 UI 生命周期回调。

## 5. 管理页面和设置页面

### 5.1 管理页面 Route

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

Route 当前包含分组增删改、排序、符号增删改、批量复制/移动、JSON 导入导出和 SAF 文件选择。Repository 由宿主提供。

### 5.2 设置页面 Route

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

设置状态只有在 Repository 保存成功后才发布新值；失败通过状态中的 `lastError` 反映，并由 Route 显示错误。

## 6. Repository、模型和 JSON

### 分组 Repository

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

### 关键模型

- `SymbolGroup(id: String, title: String, symbols: List<SymbolEntry>)`
- `SymbolEntry(id: Long, label: String, shortAction: SymbolAction, longAction: SymbolAction?, icon: String?, ...)`
- `SymbolAction` 是 sealed interface，包含插入、替换、包围选区、移动光标、删除、转换选区、编辑器命令、剪贴板、打开管理器、复合动作和扩展动作。

稳定 ID 必须跨刷新保持不变。分组 ID 非空且唯一；符号 ID 在仓储中应保持唯一且非零。

### JSON 互通

Compose 版本保留 V2 typed action 和分组 bundle 的 JSON codec/legacy migration。导入旧格式应经过 legacy adapter/migration，不要把旧 View 控件对象或 Activity 状态序列化进 JSON。管理 Route 的 `importJson` 会先完整解码，再由 manager 一次性提交数据快照。

## 7. 状态、重组和零生命周期约定

- Screen、Drawer、Renderer、Indicator 都是 Composable；不要求手动创建/启动/停止/销毁。
- UI 状态使用 `rememberSymbolDrawerState` 或页面对应的 `remember...State`。
- 异步 Provider 在 Composition effect 中运行；离开组合自动取消。
- 动态扩展注册使用 Composition effect 管理注册句柄；不要暴露 View attach/detach 生命周期。
- 库不要求宿主继承 Activity/Fragment/ViewModel，也不要求公共 UI API 接收 LifecycleOwner。
- Repository 和编辑器适配器属于宿主数据/平台层，仍由宿主按自己的数据和编辑器生命周期管理；这不等于 UI 控件生命周期由宿主管理。

## 8. 当前 Compose 迁移边界（重要）

本模块仍处于全面迁移过程中。当前可以按本手册使用的原生 Compose API 包括：
- `compose.SymbolDrawer`、`SymbolDrawerState`、`rememberSymbolDrawerState`
- Compose 扩展接口和 `ComposeSymbolDrawerExtensionRegistry`
- `compose.manager.SymbolDrawerManagerRoute/Screen`
- `compose.settings.SymbolDrawerSettingsRoute/Screen`
- 编辑器中立的 typed action、`SymbolActionHost`、`SymbolActionExecutor`、`SymbolTextDocument`

以下旧 View 实现尚未完成 Compose 替换，不能把它们当成 Compose 公共 API：
- `SymbolDrawerView` 及其 Listener、`reloadGroups()` 等实例方法
- 旧 View 的 Manager/Settings Screen、Adapter、Indicator
- 旧 `DrawerContainer`、`DrawerController`、View panel/page host
- 旧 View 扩展注册 API
- 旧 Activity/LifecycleOwner Picker 桥接

新公共 Generic Drawer facade 仍有历史 typealias 的迁移工作；在它们完成原生 Compose 重写前，不应在新 Compose 集成代码中导入这些旧 facade。后续完成迁移后，本手册会按实际源码更新，不提前承诺尚不存在的类。

## 9. 文档与源码一致性规则

每次公共 API 变更必须同步：
1. 中文与英文的类型名、包名、注册方法、行为和错误语义。
2. 每个代码块的完整 import。
3. API 首次出现时的完整类名；后续才可使用简称。
4. 标注 API 状态：已实现、实验性/内部、迁移中、规划中。
5. 代码示例必须对应当前分支源码；宿主示意类必须明确标注为宿主自定义，不得伪装成库类。
6. 旧 View API 与 Compose API 分章说明，禁止混合示例。

旧编号文档 01–16 和英文对应页目前源自 View 版本，部分内容仅可作为历史 View API 参考，不是本 Compose 模块的接入契约。请以本手册和当前源码为准。
