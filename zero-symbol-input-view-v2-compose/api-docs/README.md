# Zero Symbol Input View V2 — Compose API 文档中心

本目录仅描述 `zero-symbol-input-view-v2-compose` 模块，API 以当前 Compose 源码为准。

## 语言

- [简体中文](README.md)
- [English](en/README.md)

## 文档地图

| 文档 | 内容 |
|---|---|
| 01 快速开始 | Compose 依赖、SymbolDrawer、状态与宿主职责 |
| 02 SymbolDrawer | Composable 符号抽屉 API |
| 03 Models and Actions | 分组、符号、typed actions |
| 04 Action Runtime | Host、Executor、Registry 和结果 |
| 05 Data and Management | Repository、Manager、codec 和迁移 |
| 06 Settings | 设置快照、Repository 和设置页 |
| 07 Universal Drawer | Compose Drawer 容器、状态机、锚点和手势 |
| 08 Navigation / Panels / Style | Composable 页面、面板和样式 |
| 09 Adaptive Window | 窗口快照、布局解析和停靠策略 |
| 10 API Index | Compose API 类型和兼容边界 |
| 11 Interaction and Built-ins | 嵌套滚动与内置文本动作 |
| 12 Extension Framework | Composable 扩展和运行时注册 |
| 13 Public Packages | 公共包与编辑器中立文档契约 |
| 14 Drawer API Reference | Compose Drawer 完整 API |
| 15 Symbol Control Extensions | Compose 符号控件扩展 |
| 16 Testing and Release | Compose 测试、发布和排错 |
| 17 Source Audit Notes (EN) | 源码边界和审计说明 |

## 当前包结构

- Compose 符号抽屉：`com.zerostudio.idea.widget.editor.symbolinput.v2`
- 符号模型、数据、设置和管理：`com.zerostudio.idea.widget.editor.symbolinput.v2.*`
- 通用 Compose Drawer：`com.zerostudio.idea.widget.drawer`
- Drawer Core / Navigation / Panel / Style：`com.zerostudio.idea.widget.drawer.{core,navigation,panel,style}`
- 编辑器中立动作：`com.zerostudio.idea.widget.drawer.action`

旧路径 `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` 已不是通用 Drawer 的源码包。新代码应导入 `com.zerostudio.idea.widget.drawer.*`。

## 同仓库依赖

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2-compose"))
}
```

本模块不是旧 View artifact 的包装层。符号抽屉、通用 Drawer、页面 host、面板、管理页和设置页均使用 Compose API。模块不要求 Activity、Fragment、ViewModel 或 LifecycleOwner 管理组件状态；Composition/recomposition/effect 生命周期仍然存在。

## API 选择

- 符号输入抽屉：`SymbolDrawer` + `rememberSymbolDrawerState`
- 通用抽屉：`DrawerContainer` + `rememberDrawerContainerState`
- 页面：`DrawerPage` + `DrawerPageHost`
- 面板：`DrawerPanelContent` + `drawerPanelContent`
- 状态/锚点：`DrawerStateHolder`、`DrawerController`、`DrawerAnchorStrategy`
- 数据：`SymbolGroupRepository`、`SymbolGroupManager`
- 设置：`SymbolDrawerSettingsRepository`、`SymbolDrawerSettingsScreen`
- 编辑器适配：`SymbolActionHost` / `SymbolTextDocument`

## Artifact 隔离

旧 View artifact 的 `DrawerContainer` 是 View class，`DrawerPanelContent` 是 View factory；本模块的同名 API 是 Composable/Composable slot。不要在同一个应用依赖图中同时使用两个 Drawer UI artifact。
