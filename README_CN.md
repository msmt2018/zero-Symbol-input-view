# Zero Symbol Input View V2

<p align="center">

**面向 Android 编辑器的分组符号输入抽屉与编辑器中立动作框架。**

[English](README.md) · [API 开发文档](zero-symbol-input-view-v2/api-docs/README.md) · [V2 模块指南](zero-symbol-input-view-v2/README.md)

</p>

---

## 项目简介

Zero Symbol Input View V2 是面向代码编辑器、文本编辑器和 IDE 的 Android UI 组件库。它不是简单固定的一排快捷字符，而是一套完整的分组符号抽屉：包含独立的纵向抽屉交互、横向分组分页、符号管理、设置、数据持久化以及可扩展动作系统。

当前仓库以 V2 为主要开发和集成方向。V1 仍保留用于旧版兼容和数据迁移；新功能和新宿主应优先接入 V2。

### 核心能力

- **分组符号页面：** 一个符号组对应一个页面；横向翻页与纵向抽屉展开互相独立。
- **真实抽屉交互：** 拖动、fling、稳定锚点、折叠/展开和手势方向仲裁。
- **可配置网格：** 列数、折叠显示行数、单元格尺寸、间距、触摸目标和最大展开高度。
- **分组指示器：** 可替换真正的分组导航指示器，但原有 ViewPager2 仍是唯一分页状态所有者。
- **编辑器中立动作：** 插入、选区、光标、删除、剪贴板、编辑器命令和扩展动作。
- **管理和设置：** 分组/符号管理、收藏、最近使用、批量操作、外观、布局、交互和数据备份。
- **扩展能力：** 动作处理器、自定义符号单元格、分组指示器、设置 Tab/行、主题、旧数据提供器和生命周期回调。
- **自适应布局：** 紧凑/中等/展开窗口与大屏侧边停靠选项。
- **数据安全：** 版本化 JSON、校验、规范化、异常恢复和显式 V1 导入/迁移。

## 架构概览

```text
宿主应用
├── 编辑器 / 文本文档
├── 编辑器适配器
│   ├── SymbolActionHost
│   └── SymbolTextDocument（可选）
└── Zero Symbol Input View V2
    ├── SymbolDrawerView
    │   ├── 抽屉交互与几何
    │   ├── ViewPager2 分组分页
    │   ├── 分组指示器插槽
    │   └── SymbolPageAdapter / 符号网格
    ├── SymbolAction + SymbolActionExecutor
    ├── SymbolDrawerExtensionRegistry
    ├── 管理页 + 设置页
    └── Repository + JSON codecs
```

**V2 核心不依赖任何特定编辑器内核。** 宿主只需为当前获得焦点的文档实现适配器。Sora、自研编辑器、EditText 或其它编辑器可以复用同一 V2 库，不需要把编辑器专属依赖引入 V2 核心。

仓库还包含独立的通用 Drawer 框架。它不是符号工具栏的替代实现：`DrawerContainer` 是功能中立的通用容器；`SymbolDrawerView` 继续使用自己的符号网格、ViewPager2 和符号交互管线。

## 快速集成

在根目录 `settings.gradle.kts` 中包含模块：

```kotlin
include(":zero-symbol-input-view-v2")
```

在 app 模块添加依赖：

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

布局中添加控件：

```xml
<com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
    android:id="@+id/symbolDrawer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

连接宿主动作适配器：

```kotlin
val actionHost: SymbolActionHost = createMyEditorActionHost()

drawer.onSymbolActionListener =
    SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
        val action = if (isLongPress) symbol.longAction else symbol.shortAction
        if (action != null) {
            val result = drawer.executeAction(action, actionHost)
            handleSymbolActionResult(result)
        }
    }

drawer.onOpenManagerListener = { openManagerScreen() }
drawer.onOpenSettingsListener = { openSettingsScreen() }
drawer.reloadGroups()
```

示例中的 `createMyEditorActionHost` 和编辑器实现由宿主负责。动作 API 是同步接口，应在编辑器要求的线程调用。

## 扩展 API 开发

扩展 API 接入的是**现有符号控件**，不会重新创建第二套工具栏、分页模型、手势识别器或持久化层。

```kotlin
val registration = drawer.extensions.registerAction(
    SymbolDrawerActionExtension(
        id = "com.example.editor.format",
        title = "格式化选区",
        description = "调用宿主格式化器处理当前选区",
        handler = SymbolActionHandler { host, payload ->
            host.executeEditorCommand("com.example.editor.format", payload)
        }
    )
)

// 符号数据保存 SymbolAction.Extension(id, payload)，而不是保存 lambda。
// registration 应由所属 Fragment View 或 Editor Session 负责关闭。
```

可扩展能力：

| 扩展类型 | 用途 |
|---|---|
| 动作扩展 | 通过稳定 ID + payload 注册运行时动作处理器 |
| 单元格渲染扩展 | 由原 SymbolPageAdapter 渲染自定义符号格 |
| 分组指示器扩展 | 替换真实分组导航指示器 |
| 设置 Tab / 设置行 | 向原有设置界面注入扩展 UI |
| 主题扩展 | 为已接入的抽屉表面提供 ARGB 主题 token |
| Legacy 分组提供器 | 通过原有 codec 导入 V1 模型数据 |
| 生命周期扩展 | 观察 attach、设置、分组、选中页和符号调用 |
| 内置功能可见性 | 隐藏/恢复已接入的内置入口 |

所有注册都会返回 `AutoCloseable`。宿主必须在对应 Fragment View 或编辑器 Session 结束时释放句柄。注册回调仅存在于运行时，进程重建后需要重新注册。

## 公共 API 包路径

新 Kotlin 集成建议优先使用通用抽屉公共命名空间：

```kotlin
com.zerostudio.idea.widget.drawer
com.zerostudio.idea.widget.drawer.core
com.zerostudio.idea.widget.drawer.navigation
com.zerostudio.idea.widget.drawer.panel
com.zerostudio.idea.widget.drawer.style
com.zerostudio.idea.widget.drawer.adapter
com.zerostudio.idea.widget.drawer.extension
com.zerostudio.idea.widget.drawer.action
```

符号输入产品 API 仍位于：

```kotlin
com.zerostudio.idea.widget.editor.symbolinput.v2
```

当前部分公共包入口使用 Kotlin `typealias` 指向现有实现类型。它们是 Kotlin 源码级 facade，不是独立 Java 可见类，也不代表源码物理迁移已经全部完成。详见[公共包路径与兼容性说明](zero-symbol-input-view-v2/api-docs/13-public-package-and-editor-neutral-actions.md)。

## 文档导航

| 文档 | 说明 |
|---|---|
| [V2 模块 README](zero-symbol-input-view-v2/README.md) | 依赖、JitPack、集成和迁移 |
| [API 文档总目录](zero-symbol-input-view-v2/api-docs/README.md) | 完整 API 参考导航 |
| [SymbolDrawerView API](zero-symbol-input-view-v2/api-docs/02-symbol-drawer-view.md) | 符号控件方法、回调和状态 |
| [动作执行 API](zero-symbol-input-view-v2/api-docs/04-action-runtime.md) | Host、Executor、Registry、结果与取消 |
| [扩展注册框架](zero-symbol-input-view-v2/api-docs/12-extension-framework.md) | 注册表和扩展契约 |
| [通用 Drawer 公共 API](zero-symbol-input-view-v2/api-docs/14-drawer-public-api-reference.md) | 通用 Drawer 框架完整参考 |
| [符号控件扩展 API 实战](zero-symbol-input-view-v2/api-docs/15-symbol-control-extension-api.md) | 扩展开发完整流程 |
| [自适应窗口 API](zero-symbol-input-view-v2/api-docs/09-adaptive-window.md) | 窗口与大屏适配 |

## 构建与测试

```bash
./gradlew :zero-symbol-input-view-v2:assembleRelease
./gradlew :zero-symbol-input-view-v2:test
./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Instrumentation 测试需要连接设备或模拟器。是否达到发布条件，应以对应 commit 的 GitHub Actions 实际结果为准。

## 兼容性与集成边界

- V2 核心不导入特定编辑器引擎。
- 宿主适配器只声明真实支持的能力；不支持的操作必须返回 false，不能伪装成功。
- Composite 动作会先校验完整动作树，但不自动构成事务；原子回滚需要宿主提供事务能力。
- 通用 `DrawerContainer` 与符号专用 `SymbolDrawerView` 职责不同，配置类型也不同。
- Kotlin typealias facade 不等于 Java ABI 兼容。
- Window Insets、折叠屏、嵌套滚动、触摸仲裁和无障碍仍需在目标宿主布局/设备矩阵中验证。

## 许可

V2 项目源码声明：

```text
AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
```

SPDX 的 `AND` 表示许可义务累积适用，不是三选一。使用、修改和再分发前请阅读完整许可文本及第三方声明。
