# Zero Symbol Input View V2 — API 开发文档

> 面向 Android/Kotlin 集成开发者的完整 API 参考。本文档以当前 V2 源码为准；接口状态、限制和行为以对应版本源码为最终依据。

## 文档目标与适用范围

本目录说明 `zero-symbol-input-view-v2` 模块对外提供的数据模型、符号抽屉、动作执行、仓储、设置、通用 Drawer、导航/面板、自适应布局和管理 UI API。文档不仅列出类名，还说明方法的输入输出、状态变化、调用时机、线程要求、失败语义和集成方式。

V2 是独立实现，与旧版库并行存在。V2 默认存储使用独立 SharedPreferences 命名空间，不会主动修改旧版存储；旧版数据只通过显式的一次性迁移适配器读取。

## 快速导航

| 文档 | 内容 |
|---|---|
| [01 — 快速开始与集成](01-quick-start.md) | Gradle、依赖、初始化、生命周期、最小可运行接入 |
| [02 — 符号抽屉视图 API](02-symbol-drawer-view.md) | `SymbolDrawerView` 构造、回调、所有公开方法、交互和状态恢复 |
| [03 — 数据模型与动作类型](03-models-and-actions.md) | `SymbolGroup`、`SymbolEntry`、`SymbolAction`、`DrawerConfig` 等 |
| [04 — 动作注册与执行 API](04-action-runtime.md) | `SymbolActionHost`、Executor、Registry、JSON Codec、文本编辑接口 |
| [05 — 仓储、管理器与迁移](05-data-and-management.md) | Repository、Manager 全部命令、导入导出和旧数据迁移 |
| [06 — 设置中心 API](06-settings.md) | Settings 模型、Repository、Codec、运行时应用 |
| [07 — 通用 Drawer 框架](07-universal-drawer.md) | DrawerContainer、Controller、Anchor、状态和呈现模式 |
| [08 — 页面、面板与样式](08-navigation-panels-style.md) | 页面导航、面板生命周期、主题、样式和预设 |
| [09 — 自适应窗口 API](09-adaptive-window.md) | WindowAdaptiveInfo、布局解析、网格和侧边停靠 |
| [10 — 类型索引与兼容性](10-api-index-compatibility.md) | 类型总表、边界条件、兼容策略、排错清单 |
| [11 — 手势与内置文本动作](11-interaction-and-builtins.md) | DrawerInteractionController、文本命令目录和完整 command ID |
| [12 — 扩展注册框架](12-extension-framework.md) | 自定义动作、指示器、设置 Tab、设置项、主题和内置功能隐藏 |

## 模块坐标

模块目录：`zero-symbol-input-view-v2`

Android namespace：

```kotlin
com.zerostudio.idea.widget.editor.symbolinput.v2
```

同仓库项目依赖：

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

JitPack 发布坐标（将版本替换为实际已发布 Tag）：

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:<version>")
}
```

## API 分层与选型

| 使用目标 | 首选 API |
|---|---|
| 在编辑器底部显示符号工具栏 | `SymbolDrawerView` |
| 提供分组/符号数据 | `SymbolGroup`、`SymbolEntry`、`submitGroups` |
| 将点击动作连接到编辑器 | `SymbolActionHost` + `SymbolActionExecutor` |
| 注册宿主自定义动作 | `SymbolActionRegistry` |
| 保存、读取和维护符号数据 | `SymbolGroupRepository`、`SymbolGroupManager` |
| 设置 UI 和持久化 | `SymbolDrawerSettingsScreen`、`SymbolDrawerSettingsRepository` |
| 提供独立的通用抽屉容器 | `DrawerContainer` + `DrawerConfig` |
| 通用抽屉页面与面板 | `DrawerPageHost`、`DrawerPanelContent` |
| 大屏/折叠屏适配 | `WindowAdaptiveInfo`、`AdaptiveLayoutResolver`、`SymbolDrawerDockingCoordinator` |

## 重要设计约定

1. **稳定 ID 优先。** 分组 ID 在仓储中必须唯一；符号 ID 在整个仓储范围内必须唯一且不能为 `0L`。更新数据时不要用列表下标充当身份。
2. **数据按不可变快照传递。** 调用方应使用新的 List/数据类快照更新 UI，不要在提交后原地修改传入集合。
3. **动作与编辑器解耦。** `SymbolAction` 只描述意图；编辑器能力由宿主实现 `SymbolActionHost`。
4. **同步执行。** 当前动作宿主接口同步调用，不会自动切换线程；编辑器要求主线程时，宿主必须在主线程调用。
5. **事务先持久化再发布。** `SymbolGroupManager` 成功持久化后才替换内存快照；写入失败不会发布未落盘状态。
6. **UI 生命周期由宿主负责。** Activity/Fragment 销毁、恢复、回调解除和自定义页面释放必须纳入宿主生命周期。
7. **通用 Drawer 与符号抽屉不是同一个 UI。** `DrawerContainer` 是功能中立的独立容器；`SymbolDrawerView` 保留自己的 ViewGroup、ViewPager2 和符号网格，不要把通用容器再次套在符号抽屉外面。

## 状态与成熟度说明

- `SymbolDrawerView`、typed action model、Repository、Settings、Manager Screen 和 `DrawerContainer` 是源码中存在的实现。
- 某些低层策略/解析器是纯 Kotlin 计算 API，可独立单元测试；它们不等于自动完成宿主窗口接入。
- 不要把“配置对象存在”理解成“所有 Android 设备和所有宿主场景已验证”。窗口 Insets、折叠屏、嵌套滚动、手势和辅助功能仍需在目标设备/宿主布局中验证。
- 不要把旧版整数动作 ID 当成 V2 主运行时模型；它们只作为迁移/兼容元数据保留。

## 版本与源码核对

本目录文档针对当前 PR 分支 `feat/symbol-drawer-architecture-v2` 的实现编写。升级模块版本后，请先核对对应 Tag 的公开签名与行为，再应用本文示例。对于源码中标记为 `internal`、`private` 的类型或成员，不应从应用模块直接调用。

## 许可

本模块源码文件声明的许可为 `AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0`。该表达式表示许可条款累积适用，不代表可任选其一。集成和再分发前请阅读仓库中的完整许可文本及第三方依赖许可。
