# Zero Symbol Input View V2 — API 文档中心

面向 Android/Kotlin 集成者、编辑器适配器开发者和控件扩展开发者。本文档以 `zero-symbol-input-view-v2` 当前源码为准。

## 语言

- [简体中文](README.md)
- [English](en/README.md)

## 文档地图

| 文档 | 主题 |
|---|---|
| 01 快速开始 | Gradle、XML/View 接入和最小集成 |
| 02 SymbolDrawerView | 构造、回调、数据、分页、展开和 View 状态 |
| 03 Models and Actions | 分组、符号、typed actions |
| 04 Action Runtime | Host、Executor、Registry、执行结果 |
| 05 Data and Management | Repository、Manager、JSON、迁移 |
| 06 Settings | 设置模型、持久化和应用 |
| 07 Universal Drawer | View Drawer 容器、控制器、锚点和手势 |
| 08 Navigation / Panels / Style | 页面、面板、导航和样式 |
| 09 Adaptive Window | 窗口信息与自适应布局 |
| 10 API Index | 公共类型、包路径和兼容性 |
| 11 Interaction and Built-ins | 拖动、嵌套滚动和文本动作 |
| 12 Extension Framework | 动作、renderer、indicator、设置和主题扩展 |
| 13 Public Packages | 新公共包与编辑器中立文档契约 |
| 14 Drawer API Reference | View Drawer 完整公共 API |
| 15 Symbol Control Extensions | 符号控件扩展实战 |
| 16 Testing and Release | 测试矩阵、发布和故障排查 |

## 当前包结构

- 符号控件：`com.zerostudio.idea.widget.editor.symbolinput.v2`
- 符号模型：`com.zerostudio.idea.widget.editor.symbolinput.v2.model`
- 通用 Drawer：`com.zerostudio.idea.widget.drawer`
- Drawer Core / Navigation / Panel / Style / Adapter：`com.zerostudio.idea.widget.drawer.{core,navigation,panel,style,adapter}`
- 编辑器中立动作：`com.zerostudio.idea.widget.drawer.action`

通用 Drawer 已物理迁移到新包；不要再使用 `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer`。符号输入产品 API 没有因此更改包名。

## 依赖

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

远程 Maven 消费者应使用当前已发布的 View artifact tag；不要把 Compose artifact 的发布状态或坐标与本模块混淆。

## 选择入口

- 符号输入工具栏：`SymbolDrawerView`
- 通用 View 抽屉：`DrawerContainer`
- 分组数据：`SymbolGroupRepository` / `SymbolGroupManager`
- 编辑器接入：`SymbolActionHost`，需要通用文本编辑时实现 `SymbolTextDocument`
- 通用 Drawer 配置：`drawer.config.DrawerConfig`
- 符号网格配置：`model.DrawerConfig`

## 跨 artifact 注意事项

`zero-symbol-input-view-v2` 是 Android View 实现。Compose 实现位于独立的 `zero-symbol-input-view-v2-compose` artifact；虽然两者共享通用 Drawer 的概念和公共包名，但 DrawerContainer、DrawerPanelContent 和 DrawerPageHost 的 API 类型不同。一个应用只应选择其中一个 UI artifact。
