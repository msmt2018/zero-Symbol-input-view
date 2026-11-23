# 10 — Compose API 索引与兼容性边界

本索引对应 `zero-symbol-input-view-v2-compose`，不代表旧 View 模块的 API。

## 1. 分层索引

| 层 | 主要类型 | 职责 |
|---|---|---|
| 编辑器中立契约 | `EditorNeutralActionApi`、`SymbolActionExecutionApi`、`SymbolTextDocument` | 文本编辑、动作执行边界 |
| 领域与数据 | `SymbolModels`、`SymbolGroupRepository`、`SymbolGroupManager`、Codec/Migration | 数据校验、存储、迁移 |
| 抽屉核心 | `DrawerStateHolder`、`DrawerAnchorResolver`、`DrawerInteractionController` | 锚点、状态、手势决策 |
| 导航与面板 | `DrawerNavigationController`、`DrawerPageHost`、`DrawerPanelHost` | Compose 页面和面板 slot |
| 自适应与扩展 | `AdaptiveWindowObserver`、`SymbolDrawerExtensionRegistry` | 窗口布局和扩展注册 |
| Compose UI | `SymbolDrawer`、`DrawerContainer`、Manager/Settings screens | 页面渲染和用户交互 |

## 2. 页面与面板类型

- `DrawerPage`：稳定 ID、标题、`DrawerPanelContent` 和可选 Composable icon。
- `DrawerPageHost`：Compose Foundation HorizontalPager host。
- `DrawerPanelContent`：`@Composable ColumnScope.() -> Unit` 内容槽。
- `DrawerPanelHost`：Compose Column host。
- `DrawerContainer`：抽屉 Compose 容器；Back 由宿主处理。

这些类型没有 View factory、ViewHolder、attach/detach 回调或 ViewPager2 页面缓存 API。

## 3. 兼容性

旧模块 `zero-symbol-input-view-v2` 保留原有 View API，是独立构建产物。Compose 模块不以包装旧 View 的方式实现 UI；`AndroidView` / `AndroidViewBinding` 不属于迁移方案。

若公共 Kotlin facade 使用 `typealias`，它只提供 Kotlin 源码级别别名，不会生成 Java 可见的独立类。Java ABI 兼容需要真实 facade class 和 Java 编译测试，不可由 typealias 推断。

## 4. 生命周期边界

Compose 页面不依赖 Activity/Fragment/ViewModel/LifecycleOwner 作为 UI 状态模型。Compose 本身仍有 Composition、recomposition 和 effect cancellation 语义。持久化业务状态应由 UI 中立 Repository/模型管理；短期交互状态可以由 Composition state 管理。