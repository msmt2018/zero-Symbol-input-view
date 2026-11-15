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


## 远程 Maven SDK（JitPack）

V2 可以作为远程 Android SDK 依赖接入，不需要克隆本仓库，也不需要把模块源码复制到宿主工程。发布后的 Maven artifact 由 JitPack 提供。

先在依赖仓库配置中添加 JitPack。如果宿主工程使用 Gradle 集中式仓库管理，应在根目录的 `settings.gradle.kts` 中配置，而不只是写在 app 模块里：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

然后在 app 模块的 `build.gradle.kts` 中添加 V2 SDK：

```kotlin
dependencies {
    implementation("com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:<published-tag>")
}
```

将 `<published-tag>` 替换为已经创建且 JitPack 构建及 artifact 验证成功的 Git Tag（例如 `vX.Y.Z`）。版本号使用完整 Tag 名称，包含开头的 `v`；不要把尚未发布的分支名当作正式依赖版本。公开 artifact 不需要 Maven 账号、密码或 GitHub Token。完整坐标以及远程/本地接入说明见 [V2 模块接入指南](zero-symbol-input-view-v2/README.md#2-jitpack-远程-maven-导入)。

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

## JSON 数据格式与互通性

控件数据可以在宿主编辑器、外部符号管理工具和备份文件之间交换，而不局限于控件内部存储。

- **V1 紧凑 JSON：** 兼容旧版符号快捷栏数据。顶层为分组数组，分组字段为 `n`（名称）、`d`（符号项目）；项目字段 `a/b/c/d/e` 分别表示短按动作 ID、显示文本、短按参数、长按动作 ID、长按参数。
- **V2 完整 JSON bundle：** 使用版本化 envelope，保留稳定 Group/Symbol ID、图标和 typed actions，适合完整备份及 V2 应用之间交换。
- **兼容边界：** V1 只能表达旧数字动作目录支持的动作。Composite、部分 Selection 和任意 Extension 动作无法保证无损写回 V1；遇到无法表达的动作时会拒绝导出，不会静默丢失。
- **宿主扩展动作：** 通过稳定 action ID + payload 保存动作描述；handler/lambda 只在运行时注册，不会被序列化，进程重建后由宿主重新注册。

V1 紧凑 JSON 示例：

```json
[
  {
    "n": "常用",
    "d": [
      { "a": 0, "b": "括号", "c": "()", "d": 0, "e": "[]" }
    ]
  }
]
```

字段定义、严格导入校验、导出失败语义和 V2 bundle 使用方法见[数据模型与动作](zero-symbol-input-view-v2/api-docs/03-models-and-actions.md)及[仓储、管理与迁移](zero-symbol-input-view-v2/api-docs/05-data-and-management.md)。MT 管理器兼容性以此紧凑结构为准；其它 JSON 变体必须提供样例验证，不能仅凭扩展名认定兼容。

## 文档与接入指南

- [V2 模块接入指南](zero-symbol-input-view-v2/README.md)：本地 Module、JitPack、Activity/Fragment、任意编辑器适配器、管理/设置及 JSON 互通。
- [API 文档总目录](zero-symbol-input-view-v2/api-docs/README.md)：完整公共 API、模型、动作运行时、抽屉交互和扩展框架。
- [扩展注册框架](zero-symbol-input-view-v2/api-docs/12-extension-framework.md) 与[符号控件扩展 API 实战](zero-symbol-input-view-v2/api-docs/15-symbol-control-extension-api.md)：扩展现有控件，不创建平行控件。
- [测试、发布与故障排查](zero-symbol-input-view-v2/api-docs/16-testing-release-troubleshooting.md)：仓库维护者使用的工程文档。

## 兼容性与集成边界

- V2 核心不导入特定编辑器引擎。
- 宿主适配器只声明真实支持的能力；不支持的操作必须返回 false，不能伪装成功。
- Composite 动作会先校验完整动作树，但不自动构成事务；原子回滚需要宿主提供事务能力。
- 通用 `DrawerContainer` 与符号专用 `SymbolDrawerView` 职责不同，配置类型也不同。
- Kotlin typealias facade 不等于 Java ABI 兼容。
- Window Insets、折叠屏、嵌套滚动、触摸仲裁和无障碍仍需在目标宿主布局/设备矩阵中验证。

## 开源许可协议

### SPDX 许可标识

V2 项目原创源码文件使用以下 SPDX 许可表达式：

```text
SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
```

这里的 `AND` 是有意采用的累积许可表达式：列出的三份许可证同时适用于同一受许可作品，不表示接收者可以从中任选一份。使用受许可内容时，必须遵守三份许可证中所有适用的条件。

### 三份许可证的主要要求

1. **GNU Affero 通用公共许可证 v3.0（仅此版本，AGPL-3.0-only）。** 授予使用、研究、修改和再分发作品的权利。特别是，当修改版本通过计算机网络向用户提供交互服务时，AGPLv3 第 13 条要求按照 AGPL 条款向这些用户提供该修改版本的 Corresponding Source（对应源代码）。
2. **GNU 通用公共许可证 v3.0（仅此版本，GPL-3.0-only）。** 授予使用、研究、修改和再分发作品的权利。传播目标代码时，必须遵守 GPLv3 的适用要求，包括提供对应源代码或有效的书面要约、保留许可和版权声明，以及履行其他许可条件。
3. **Apache License 2.0（Apache-2.0）。** 授予版权及适用的专利许可，但必须遵守其条件，包括附带许可证副本、保留相关版权/专利/商标/归属声明、标记修改过的文件，并遵守专利终止条款。

以上仅为便于理解的摘要，不构成法律意见，也不能替代完整许可证文本。如果实际意图并非三份许可证累积适用，应在发布前寻求法律审查，并统一修正 SPDX 表达式及全部相关声明。

### 完整许可证文本

- [GNU AGPL v3.0 完整文本](zero-symbol-input-view-v2/LICENSE-AGPL-3.0-only.txt) · [官方文本](https://www.gnu.org/licenses/agpl-3.0.html)
- [GNU GPL v3.0 完整文本](zero-symbol-input-view-v2/LICENSE-GPL-3.0-only.txt) · [官方文本](https://www.gnu.org/licenses/gpl-3.0.html)
- [Apache License 2.0 完整文本](zero-symbol-input-view-v2/LICENSE-Apache-2.0.txt) · [官方文本](https://www.apache.org/licenses/LICENSE-2.0)

### 适用范围与第三方代码

本许可声明适用于 `zero-symbol-input-view-v2/` 目录内带有上述 SPDX 标识的 V2 项目原创文件，以及版权持有人已授权按该许可发布的贡献。它不会自动将整个仓库、旧版 V1 模块、第三方依赖、引入/复制的外部代码，或带有独立上游许可声明的文件重新许可。

仓库根目录的 `LICENSE` 仍适用于未被 V2 文件级许可声明明确覆盖的仓库内容。必须保留所有第三方版权和许可证声明；文件级声明不能覆盖更具体的上游许可。

**Copyright (C) 2026 android_zero**  
作者：android_zero · [GitHub](https://github.com/msmt2018) · 微信：`android_zero`
