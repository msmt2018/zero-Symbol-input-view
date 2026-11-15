# Zero Symbol Input View V2

**面向 Android 编辑器的分组符号输入抽屉与可扩展动作系统。**

Zero Symbol Input View V2 是一个独立的 Android UI 组件库，为代码编辑器、文本编辑器和 IDE 提供可配置的符号工具栏。V2 将抽屉交互、分组分页、符号数据、管理界面、设置和动作执行协议拆分为明确的模块，不再把符号输入与某一种编辑器实现绑定。

> 当前仓库以 V2 为主要开发和集成方向。应用侧已迁移到 V2；V1 模块仍保留在仓库中，用于旧实现兼容和数据迁移，不代表新功能应继续接入 V1。

---

## 目录

- [项目目标](#项目目标)
- [V2 核心能力](#v2-核心能力)
- [架构概览](#架构概览)
- [模块与目录](#模块与目录)
- [集成方式](#集成方式)
- [数据模型与动作系统](#数据模型与动作系统)
- [管理、设置与备份](#管理设置与备份)
- [V1 数据迁移](#v1-数据迁移)
- [构建与测试](#构建与测试)
- [许可与第三方声明](#许可与第三方声明)

## 项目目标

V2 的目标不是简单地在编辑器底部放置一排字符，而是提供完整、可复用的符号输入子系统：

- 用分组组织符号，每个分组对应一个独立页面。
- 抽屉纵向展开/收起与分组横向翻页分别处理，互不抢占手势。
- 把符号数据与编辑器动作执行解耦。
- 为宿主应用提供明确的动作适配边界，允许接入 Sora CodeEditor 或其它编辑器。
- 提供独立的分组/符号管理、运行时设置、持久化和 JSON 备份能力。
- 在手机、平板、分屏和可调整窗口中适配内容布局。

## V2 核心能力

### 1. 符号抽屉与分组分页

核心视图：`SymbolDrawerView`

- 分组数据驱动的符号页面。
- 基于 ViewPager2 的原生横向分页和页面吸附。
- 独立的纵向抽屉拖动、展开、收起和甩动结算。
- 手势方向判定，避免纵向抽屉拦截横向翻页。
- 可配置列数、折叠显示行数、单元格尺寸、间距、触摸目标和最大展开高度。
- 可保留当前分组和视图展开状态。
- 支持符号短按、长按、触觉反馈及动作后自动收起。
- 分组指示器与分页数据模型分离：切换指示器外观不会改变分组分页本身。

### 2. 分组指示器

V2 将“分组指示器”定义为分组页面导航的呈现层，而不是给每个符号 Tab 设置背景高亮。

指示器负责表达当前分组、分组数量和页面切换状态；实际页面仍由独立分页容器管理。可选样式覆盖文字标签、胶囊、圆点、下划线、弹性/流体效果、刻度/仪表效果、节点、卡片、缩略预览、数字计数和导航轨道等类别。

### 3. 符号与动作

每个符号包含稳定 ID、显示标签、短按动作和可选长按动作。动作使用 V2 类型化模型表达，不再把所有行为都压缩成单一数字 ID。

内置动作模型包括：

- 插入文本、替换选区、包裹选中文本。
- 光标移动和删除操作。
- 选区大小写转换。
- 编辑器命令和扩展动作。
- 剪贴板复制、剪切、粘贴。
- 组合动作（按顺序执行多个子动作）。
- 打开符号管理页面。

动作由宿主应用实现的 `SymbolActionHost` 执行。库本身不假定宿主一定使用 Sora，也不直接持有某个编辑器实例。

### 4. 管理中心

核心界面：`SymbolDrawerManagerScreen`

- 分组新增、重命名、复制、删除和排序。
- 符号新增、编辑、复制、移动、删除和排序。
- 批量选择及批量操作。
- 搜索、收藏、最近使用和筛选。
- 内联预览及编辑操作。
- 数据变更通过管理模型和 Repository 提交，避免 UI 直接修改持久化数据。

### 5. 设置中心

核心界面：`SymbolDrawerSettingsScreen`

设置按类别组织，覆盖：

| 类别 | 主要配置 |
|---|---|
| 外观 | 系统主题适配、分组指示器、分组标题、符号标签、符号完整显示、字号、动作提示 |
| 布局 | 每行列数、折叠行数、单元格高度、横纵间距、指示器高度、最大展开高度、最小触摸目标 |
| 大屏 | 自动/底部/侧边布局偏好、侧边停靠方向 |
| 交互 | 长按触发时间、甩动阈值、动作后自动收起、记忆展开状态、动画与触觉反馈 |
| 性能与搜索 | 页面缓存、相邻分组预加载、是否搜索动作载荷 |
| 数据 | 分组/设置 JSON 导入与导出 |

“符号完整显示”会保持标题单行完整呈现；布局会根据可用宽度和标题长度调整网格列数/文字尺寸，而不是用省略号截断标题。

### 6. 自适应窗口

V2 将窗口信息、内容排布、网格尺寸和抽屉几何分开处理。窗口尺寸类别变化时，优先调整内部内容宽度、网格和间距，不把平板/分屏适配等同于改变抽屉交互语义。

支持底部工具栏场景下的紧凑、中等和扩展窗口布局计算，并提供大屏侧边停靠策略配置。

### 7. 持久化与数据安全

- 分组数据通过 `SymbolGroupRepository` 抽象。
- 默认实现使用独立的 V2 SharedPreferences 存储。
- JSON 存储带版本信息，并在读取时校验和规范化数据。
- 检测到损坏数据时，先保留原始 JSON 和错误原因，再允许后续恢复默认数据或重新导入。
- 分组和符号 ID 会校验唯一性。
- 设置采用独立的版本化设置仓库。
- 支持分组数据包和设置数据的 JSON 备份/恢复工作流。

---

## 架构概览

```text
Host App / Editor
       |
       +-- SymbolDrawerView
       |      |
       |      +-- Drawer interaction & geometry
       |      +-- ViewPager2 group pages
       |      +-- Group indicator renderer
       |      +-- Symbol page/grid adapter
       |
       +-- SymbolActionHost  <---- SymbolActionExecutor
       |                            |
       |                            +-- Typed SymbolAction
       |                            +-- Composite action validation
       |                            +-- Result / cancellation
       |
       +-- SymbolDrawerManagerScreen
       |      |
       |      +-- SymbolGroupManager
       |      +-- SymbolGroupRepository
       |
       +-- SymbolDrawerSettingsScreen
              |
              +-- Settings repository
              +-- JSON import/export

Persistence
  +-- Versioned symbol-group JSON
  +-- Versioned settings JSON
  +-- Legacy V1 migration adapter
```

### 关键设计边界

- **View 层**：负责展示、触摸分发和页面状态，不直接执行具体编辑器命令。
- **Model 层**：使用不可变的 `SymbolGroup`、`SymbolEntry` 和类型化 `SymbolAction`。
- **Repository 层**：负责数据读写、版本编码、校验和迁移。
- **Action 层**：负责动作验证、执行顺序、取消和结果报告。
- **Host 层**：由接入方适配编辑器 API、扩展命令和页面导航。

## 模块与目录

V2 独立 Android Library 模块：

```text
zero-symbol-input-view-v2/
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/
    │   │   ├── SymbolDrawerView.kt
    │   │   ├── action/       # 动作模型、注册表、执行器与宿主协议
    │   │   ├── adaptive/     # 窗口与内容自适应
    │   │   ├── data/         # Repository、JSON、默认数据、V1 迁移
    │   │   ├── drawer/       # 抽屉状态、手势、导航、面板和样式
    │   │   ├── manager/      # 分组与符号管理界面
    │   │   ├── model/        # V2 数据模型
    │   │   ├── settings/     # 设置模型、仓库、界面和文件选择
    │   │   ├── state/        # 抽屉几何与状态
    │   │   └── ui/           # 分组指示器、符号页面与主题颜色
    │   └── res/
    ├── src/test/             # JVM 单元测试
    └── src/androidTest/      # Android 视图/交互测试
```

主要包名与 API 边界：

```text
com.zerostudio.idea.widget.drawer              # 独立抽屉 API
com.zerostudio.idea.widget.symboltoolbar.api   # 独立符号工具栏 API
com.zerostudio.idea.widget.editor.symbolinput.v2 # V2 功能实现与兼容入口（迁移完成前）
```

**重要边界：** Drawer API 只描述通用抽屉容器、锚点、手势、导航、面板、样式和抽屉扩展；Symbol Toolbar API 独立描述符号组、符号项、工具栏状态、工具栏配置、调用事件、动作宿主和工具栏扩展。符号工具栏 API 不继承、不导入、不要求创建 Drawer API 对象；抽屉仅可作为可选宿主容器。两套 API 不共享运行时状态或专属扩展注册表。旧 V2 包路径仅作为现有实现的迁移过渡位置，不应再向其中新增公共 API。

主要公开入口：

- `SymbolDrawerView`
- `SymbolDrawerManagerScreen`
- `SymbolDrawerSettingsScreen`
- `SymbolGroupRepository` / `SharedPreferencesSymbolGroupRepository`
- `SymbolGroupManager`
- `SymbolActionHost` / `SymbolActionExecutor`
- `SymbolActionRegistry`

---

## 集成方式

### 1. 作为同一 Gradle 工程中的模块

仓库根目录的 `settings.gradle.kts` 已包含：

```kotlin
include(":zero-symbol-input-view-v2")
```

宿主模块添加依赖：

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

如果将 V2 模块复制到其它 Android 工程，请同时将其加入该工程的 `settings.gradle.kts`，并确保工程已配置 Google Maven、Maven Central 等依赖仓库。

### 2. 在布局中创建抽屉

XML：

```xml
<com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
    android:id="@+id/symbolDrawer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

或 Kotlin：

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

### 3. 绑定动作宿主

抽屉通过回调把符号动作交给宿主应用。下面示例只展示接入边界；具体编辑器操作应由你的编辑器适配层实现：

```kotlin
drawer.onSymbolActionListener =
    SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
        val action = if (isLongPress) {
            symbol.longAction
        } else {
            symbol.shortAction
        } ?: return@OnSymbolActionListener

        // 将 action 交给应用自己的 SymbolActionHost / SymbolActionExecutor。
        // 在编辑器要求的线程中执行，并处理 Unsupported / Failed 等结果。
        editorActionAdapter.execute(action)
    }

drawer.onOpenManagerListener = {
    // 由宿主决定如何展示管理界面
}
```

`SymbolActionHost` 是动作执行协议。宿主需要实现文本插入、选区替换、光标移动、删除、剪贴板、编辑器命令和扩展动作等能力；不支持的能力应返回 `false`，由执行器以明确结果反馈。

### 4. 数据与分组

```kotlin
val groups = listOf(
    SymbolGroup(
        id = "common",
        title = "常用",
        symbols = listOf(
            SymbolEntry(
                id = 1L,
                label = "->",
                shortAction = SymbolAction.InsertText("->")
            )
        )
    )
)

drawer.submitGroups(groups)
drawer.saveGroups()

// 按 ID 切换分组；第二个参数控制是否平滑滚动
drawer.selectGroup(index = 0, smoothScroll = true)

// 展开/收起
drawer.setExpanded(expanded = true, animate = true)
```

默认 Repository 为 `SharedPreferencesSymbolGroupRepository`。也可以实现 `SymbolGroupRepository` 并通过 `drawer.setRepository(repository)` 接入自定义数据层。

### 5. 管理与设置界面

两个界面均为 View，不强制宿主使用特定 Activity 或导航框架：

```kotlin
val manager = SymbolDrawerManagerScreen(context).apply {
    onGroupsChanged = { updatedGroups ->
        drawer.submitGroups(updatedGroups)
    }
}

val settings = SymbolDrawerSettingsScreen(context).apply {
    onSettingsChanged = { snapshot ->
        drawer.applySettings(snapshot)
    }
}
```

宿主负责将它们放入 Activity、Fragment、Dialog 或自己的页面容器中。管理页和设置页可独立使用，也可由抽屉的管理/设置入口打开。

### 6. 通过 JitPack 远程 Maven 导入

不需要克隆仓库或把 V2 源码复制到自己的工程。先在根目录 `settings.gradle.kts` 的仓库列表添加 JitPack：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

然后添加已经发布成功的版本（将版本号替换为实际存在的 release tag）：

```kotlin
dependencies {
    implementation("com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:v0.25.0")
}
```

完整 Maven 坐标、Groovy DSL、手动发布工作流和 Tag 规则见 [V2 模块快速接入文档](zero-symbol-input-view-v2/README.md#2-jitpack-远程-maven-导入)。

### 7. 编辑器解耦与独立 API

符号工具栏 API 位于 `com.zerostudio.idea.widget.symboltoolbar.api`，其模型、动作协议和扩展注册位于独立子包。该 API 不引用 `DrawerContainer`、`DrawerConfig` 或任何抽屉类型。

V2 不要求依赖 Sora Editor 或任何编辑器核心对象。宿主只需实现 `SymbolActionHost`，把文本插入、选区替换、光标、删除、剪贴板和编辑器命令映射到自己已有的编辑器 API。可选实现 `SymbolTextDocument`，复用通用行、选区、光标和文档编辑算法。Sora Adapter 应放在宿主应用或独立可选模块中，不应进入 V2 核心 AAR。

每个 `SymbolDrawerView` 提供 `drawer.extensions`，用于注册自定义动作、分组指示器、设置 Tab、设置项和主题 Provider，也可隐藏/恢复内置功能而不删除源码。详见 [扩展 API 完整文档](zero-symbol-input-view-v2/api-docs/12-extension-framework.md)。

### 7.1 符号工具栏自定义扩展 API

工具栏扩展位于独立的 `com.zerostudio.idea.widget.symboltoolbar.api.extension` 包，不复用 `drawer.extensions`，也不要求宿主创建抽屉。当前扩展点包括：

- **分组数据源**：`SymbolToolbarGroupProvider`，以协程方式提供不可变分组快照。
- **符号项视图**：`SymbolToolbarItemViewExtension`，自定义 item View 的创建、绑定和回收。
- **分组指示器**：`SymbolToolbarGroupIndicatorExtension`，替换组导航展示，但选择状态仍由工具栏宿主持有。
- **设置扩展**：`SymbolToolbarSettingsExtension`，注入独立设置页/区域，并通过 `SymbolToolbarSettingsScope` 更新配置。
- **主题扩展**：`SymbolToolbarThemeExtension`，提供工具栏专属 ARGB 主题 token。
- **调用拦截器**：`SymbolToolbarInvocationInterceptor`，按优先级执行调用前继续/消费/拒绝及调用后通知。
- **生命周期扩展**：`SymbolToolbarLifecycleExtension`，接收控件附着、配置变更和分离通知。
- **动作处理器/贡献项**：使用稳定 action type 注册 handler，并可独立发布可发现的动作贡献元数据。

注册统一由 `SymbolToolbarExtensionRegistry` 管理。每类扩展内部 ID 唯一，重复注册会显式失败；注册返回 `AutoCloseable`，关闭时只注销对应注册项；快照按 priority、id 稳定排序；观察者可订阅变更并在宿主释放时取消订阅。调用执行由 `SymbolToolbarExtensionRuntime` 负责拦截器链、动作分发、结果回调和生命周期通知。

扩展对象及处理器只存在于当前运行时，不会自动写入 JSON。持久化时应保存稳定 ID 和配置值，重新创建控件/会话时重新注册扩展。动作宿主只暴露宿主显式授予的能力；扩展 API 不持有编辑器核心对象，也不访问抽屉状态。

#### 7.2 对外事件接口与简洁动作 DSL

事件监听与动作代码是两种不同能力：

- **事件接口**：监听控件发生了什么，并允许在回调中自由编写 Kotlin 代码，例如日志、统计、状态同步、调用业务服务。
- **动作列表**：定义用户点击/长按符号时真正执行哪些动作。它不是只读事件通知，而是可执行的有序代码列表。

**单击和长按是两份完全独立的动作列表**。一个符号可以配置多个单击步骤、多个长按步骤；执行时按列表顺序运行，遇到非 Success 结果即停止。长按不会自动执行单击列表，单击也不会执行长按列表。开发者写入的 lambda 仅保存在当前进程运行时，不会被序列化进 JSON。

```kotlin
val bindings = symbolItemActions(
    itemId = "symbol.arrow",
    click = {
        step("insert-arrow") { insert(" -> ") }
        step("record-click") {
            analytics.track("arrow_clicked")
            SymbolToolbarActionResult.Success
        }
    },
    longPress = {
        step("insert-template") { insert(" -> { }") }
        step("move-cursor") { moveCursor(-2) }
        step("notify-plugin") {
            myPlugin.onArrowLongPressed(itemId)
            SymbolToolbarActionResult.Success
        }
    }
)
val registration = actionLists.register(bindings)
```

上面的 analytics、myPlugin 是开发者自己的 Kotlin 对象；API 不限制回调里只能调用内置编辑动作。需要编辑文本时可使用 insert、deleteBackward、moveCursor、command，也可调用自己的同步/挂起业务逻辑。每一步返回 SymbolToolbarActionResult.Success 才继续下一步；返回 Rejected、Unsupported 或 Failed 会终止当前列表。协程取消会继续向上传播。

事件订阅示例：

```kotlin
val runtime = SymbolToolbarExtensionRuntime(
    controlId = "editor.symbols",
    registry = extensions,
    actionHost = editorActionHost
)
val subscription = runtime.events.on<SymbolToolbarActionEvent.Completed> { event ->
    logger.info("item=${event.invocation.itemId}, result=${event.result}")
}
```

SymbolToolbarActionListRegistry 用于按 itemId 保存和替换单击/长按动作列表；register 返回的 AutoCloseable 只注销本次注册。列表执行器 SymbolToolbarActionList.execute(invocation, host) 负责按序执行并在第一个非成功结果处停止。

**接入状态说明：** 本次新增的是完整的动作列表声明、运行时注册与顺序执行 API。它尚不能单独证明旧版 SymbolDrawerView 已自动使用这些列表；真实控件必须在识别到 TAP/LONG_PRESS 后，分别查找对应列表并执行。现有旧模型的 shortAction/longAction 和 V1 数字动作兼容路径不应被删除或改写，接入时应保留旧动作作为兼容回退路径。事件 Hub 的 UI 事件也必须由控件在真实触摸/分页/设置回调中发布后才会触发。
### 8. V1 / MT 管理器紧凑 JSON 互通

管理界面提供 `importMtManagerJson(json)` 和 `exportMtManagerJson()`，用于旧版紧凑分组/符号 JSON（字段 `n/d/a/b/c/d/e`）。采用该结构的文件可以直接导入。V2 完整备份应使用 `importBundle` / `exportBundle`，以保留 V2 专属动作和稳定 ID。旧格式无法表达全部 V2 动作；导出遇到不支持的动作会明确拒绝，不会静默丢失。详见 [MT JSON 互通说明](zero-symbol-input-view-v2/README.md#10-mt-管理器--v1-json-互通)。

---

## 数据模型与动作系统

### 分组数据

`SymbolGroup` 使用稳定字符串 ID；`SymbolEntry` 使用稳定 Long ID。更新分组时应保持 ID 稳定，以便分页选择、列表 Diff 和管理状态能够正确恢复。

### 类型化动作

常用动作示例：

```kotlin
SymbolAction.InsertText("fun ")
SymbolAction.ReplaceSelection("replacement")
SymbolAction.SurroundSelection("(", ")")
SymbolAction.MoveCursor(CursorTarget.LINE_END)
SymbolAction.Delete(DeleteKind.BACKWARD)
SymbolAction.TransformSelection(SelectionTransform.UPPERCASE)
SymbolAction.Clipboard(ClipboardOperation.PASTE)
SymbolAction.EditorCommand("editor.format", payload = null)
SymbolAction.Composite(
    listOf(
        SymbolAction.InsertText("if ("),
        SymbolAction.MoveCursor(CursorTarget.LEFT)
    )
)
```

`SymbolActionExecutor` 会在执行前验证组合动作结构，并提供执行成功、不支持、无效、取消、失败和部分失败等结果。执行器对组合深度、动作数量和文本载荷长度设有限制，避免不受控的递归或超大载荷。

### 扩展动作注册

`SymbolActionRegistry<C>` 允许宿主按稳定的命名空间 ID 注册处理器。注册返回的 `AutoCloseable` 可用于在编辑器/会话生命周期结束时注销处理器。注册表只保存当前进程中的函数引用；持久化数据应保存动作 ID 和 payload，而不是 lambda。

---

## 管理、设置与备份

- 管理操作由 `SymbolGroupManager` 执行，并以不可变快照提交。
- 批量复制/移动等操作在校验后一次性提交，避免逐项写入造成中间状态。
- 搜索、收藏、最近使用和筛选属于管理界面能力。
- 设置使用独立仓库，运行时可通过 `applySettings()` 更新抽屉。
- JSON 导入应经过版本和结构校验；不要把不可信 JSON 直接当作可执行代码。
- 遇到损坏的本地分组数据时，V2 会保留原始数据备份和错误信息，便于后续恢复。

## V1 数据迁移

V2 包含旧数据迁移适配层：

- 从旧版符号数据和 SharedPreferences 中读取可识别的数据。
- 将旧版数字动作 ID 与文本载荷映射到 V2 类型化动作/兼容动作表示。
- 保留旧数字 ID 与 payload，避免迁移时无故丢失旧动作信息。
- 仅在 V2 存储为空且满足迁移条件时导入旧数据。
- V2 以独立存储保存新数据，不要求新运行时继续依赖 V1 View 或 V1 管理界面。

迁移完成后，宿主应以 V2 Repository 中的数据为准。不要在 V1、V2 两套存储中同时写入同一份运行时状态。

---

## 构建与测试

使用仓库根目录的 Gradle Wrapper：

```bash
# 构建 V2 release AAR
./gradlew :zero-symbol-input-view-v2:assembleRelease

# 运行 V2 JVM 单元测试
./gradlew :zero-symbol-input-view-v2:test

# 运行 V2 Android 仪器测试（需要已连接设备或模拟器）
./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest

# 构建集成示例应用
./gradlew :app:assembleDebug
```

仪器测试覆盖抽屉布局、锚点/状态、手势仲裁、分组分页、管理界面、设置、数据迁移和生命周期行为。CI 的设备/API 矩阵结果以 GitHub Actions 实际运行记录为准。

---

## 许可与第三方声明

V2 自有代码的 SPDX 许可标识为：

```text
AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
```

这里的 `AND` 表示列出的许可条款累积适用，并非任选其中一个。使用、修改、分发或提供网络服务前，请阅读并遵守对应完整许可文本。完整文本位于 V2 模块及仓库的许可文件中。

仓库还包含其它上游项目及第三方组件。第三方代码、资源和依赖仍按各自原始许可证和版权声明执行；V2 的许可说明不会覆盖或替代这些上游许可。

- [GNU AGPL v3.0](https://www.gnu.org/licenses/agpl-3.0.html)
- [GNU GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.html)
- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

[English README](README.md)
