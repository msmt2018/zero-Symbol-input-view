# Zero Symbol Input View V2 — Compose API 文档

本文档对应 `zero-symbol-input-view-v2-compose` 模块。所有 UI 示例和扩展点均以 Compose API 为准；旧版 `zero-symbol-input-view-v2` 是独立的传统 View 兼容模块，不属于 Compose 模块。

## 语言

- [简体中文](README.md)
- [English](en/README.md)

## 文档索引

| 文档 | 主题 |
|---|---|
| [01 快速开始](01-quick-start.md) | 模块接入与最小 Compose 用法 |
| [02 SymbolDrawer Compose API](02-symbol-drawer-view.md) | 抽屉入口与核心参数 |
| [03 模型与动作](03-models-and-actions.md) | 领域数据模型 |
| [04 动作执行](04-action-runtime.md) | Host、Executor、Registry、事务编辑 |
| [05 数据与管理](05-data-and-management.md) | Repository、Manager、迁移与批量操作 |
| [06 设置](06-settings.md) | 设置模型、仓储和 Composable 页面 |
| [07 通用 Drawer](07-universal-drawer.md) | DrawerContainer、锚点、手势和状态 |
| [08 页面与面板](08-navigation-panels-style.md) | Compose pager、内容 slot、样式 |
| [09 自适应窗口](09-adaptive-window.md) | Compose 布局尺寸、Insets、大屏断点 |
| [10 API 索引](10-api-index-compatibility.md) | 分层 API 与兼容性边界 |
| [11 内置交互](11-interaction-and-builtins.md) | 手势和内置动作 |
| [12 扩展注册](12-extension-framework.md) | Composable 扩展点与注册句柄 |
| [13 公共包与编辑器中立 API](13-public-package-and-editor-neutral-actions.md) | 包路径与文本编辑契约 |
| [14 Drawer 公共 API](14-drawer-public-api-reference.md) | Drawer 公共接口 |
| [15 扩展实战](15-symbol-control-extension-api.md) | Compose 扩展示例 |
| [16 测试与发布](16-testing-release-troubleshooting.md) | 测试、CI 与排错 |

## 架构边界

Compose 模块按底层到 UI 分层：

1. 编辑器中立文本/动作契约
2. 领域模型、验证和动作运行时
3. Repository、Codec 和迁移
4. Drawer 状态、几何、锚点和交互策略
5. 导航、面板、自适应布局和扩展
6. Compose UI 与 screens

不要从 UI 控件反向驱动底层 API 重构。数据和动作 API 不应为了 Compose 而全部变成 `@Composable`。

## 生命周期语义

Compose 页面不依赖 Activity、Fragment、ViewModel 或 LifecycleOwner 作为 UI 状态模型，也不通过 AndroidView 包装传统 View。Compose 自身仍有 Composition、recomposition、remember state 作用域和 effect cancellation；因此“零生命周期”只能理解为不依赖传统 View/页面生命周期，不能理解为运行时没有任何生命周期语义。

## 旧版兼容模块

`zero-symbol-input-view-v2` 保持原有 View API 与兼容测试。不要将旧模块的 View、RecyclerView、ViewPager2、Fragment 集成示例复制到 Compose 模块。