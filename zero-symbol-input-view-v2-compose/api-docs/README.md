# Zero Symbol Input View V2 — Compose API 文档中心

本目录仅描述 `zero-symbol-input-view-v2-compose`。所有 API 签名、可见性、默认参数和异常行为均以当前 Compose 模块生产源码为准。

## 语言

- [简体中文](README.md)
- [English](en/README.md)

## 文档地图

| 文档 | 内容 |
|---|---|
| 01–06 | 快速开始、符号抽屉、模型/动作、动作运行时、数据管理、设置 |
| 07–09 | 通用 Compose Drawer、导航/面板/样式、自适应窗口 |
| 10–13 | API 索引、交互与内置动作、扩展框架、公共包和编辑器中立契约 |
| 14–16 | Drawer 完整参考、符号扩展实战、测试/发布/排错 |
| [17 KDoc API 完整开发参考](17-kdoc-api-reference.md) | 按 Kotlin/JavaDoc 规范梳理公开 API、包结构、参数契约、状态/线程/异常/兼容边界 |

## 规范包结构

- 符号输入 UI：`com.zerostudio.idea.widget.editor.symbolinput.v2`
- 符号模型/动作/数据/设置：`com.zerostudio.idea.widget.editor.symbolinput.v2.*`
- 通用 Compose Drawer：`com.zerostudio.idea.widget.drawer.*`
- 编辑器中立动作：`com.zerostudio.idea.widget.drawer.action`

通用 Drawer 已位于 `com.zerostudio.idea.widget.drawer.*`；旧路径 `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` 不再是规范源码包。

## 依赖

同仓库：

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2-compose"))
}
```

外部 Maven 消费者必须使用 Compose artifact 实际发布的坐标和已成功发布的版本，不得复用旧 View artifact 坐标。

## API 选择

- 符号输入：`SymbolDrawer` + `rememberSymbolDrawerState`
- 通用抽屉：`DrawerContainer` + `rememberDrawerContainerState`
- 页面/面板：`DrawerPage`、`DrawerPageHost`、`DrawerPanelContent`
- 状态/锚点：`DrawerStateHolder`、`DrawerController`、`DrawerAnchorStrategy`
- 数据：`SymbolGroupRepository`、`SymbolGroupManager`
- 设置：`SymbolDrawerSettingsRepository`、`SymbolDrawerSettingsScreen`
- 编辑器适配：`SymbolActionHost`、`SymbolTextDocument`

## KDoc / Dokka

公开 Kotlin 声明应使用标准 KDoc，按适用性说明 `@param`、`@return`、`@throws`、`@see`。内部实现、Composable slot、Kotlin typealias 和持久化契约必须分别说明，不能把 typealias 当成 Java 独立类，也不能把 Composable slot 当成 View factory。

## Composition 边界

本模块不要求 Activity、Fragment、ViewModel 或 LifecycleOwner 作为 UI 状态管理合同，不创建传统 Android View，也不使用 AndroidView 包装旧控件。Compose 仍有 Composition、recomposition、remember 和 effect cancellation 语义；“零传统 View 生命周期耦合”不等于“没有任何生命周期语义”。

## Artifact 隔离

旧 View artifact 的同名概念是 Android View class / View factory；本模块提供 Composable / Composable slot。二者不可互换，不应同时作为同一应用的 Drawer UI 依赖。
