# 10 — API 类型索引、包路径与兼容性

本文以 `zero-symbol-input-view-v2` 当前源码为准。通用 Drawer 已迁移至 `com.zerostudio.idea.widget.drawer`，不再使用旧的 `editor.symbolinput.v2.drawer` 包路径。

## 1. 符号输入产品 API

| 类型 | 包 | 用途 |
|---|---|---|
| `SymbolDrawerView` | `editor.symbolinput.v2` | Android View 符号抽屉 |
| `SymbolGroup` / `SymbolEntry` | `editor.symbolinput.v2.model` | 分组与符号数据 |
| `SymbolAction` | `editor.symbolinput.v2.model` | 短按/长按 typed action |
| `DrawerConfig` | `editor.symbolinput.v2.model` | 符号网格布局配置 |
| `SymbolDrawerSettings` | `editor.symbolinput.v2.settings` | 不可变设置快照 |
| `SymbolDrawerSettingsScreen` | `editor.symbolinput.v2.settings` | View 设置界面 |
| `SymbolDrawerManagerScreen` | `editor.symbolinput.v2.manager` | View 管理界面 |

## 2. 通用 Drawer API（新公共包）

| 类型 | 包 | 用途 |
|---|---|---|
| `DrawerContainer` | `com.zerostudio.idea.widget.drawer` | 通用 ViewGroup 容器 |
| `DrawerController` / `DrawerState` | `drawer.core` | 控制接口与状态快照 |
| `DrawerAnchor` / `DrawerAnchorInput` | `drawer.core` | 锚点模型 |
| `DrawerAnchorStrategy` / `DrawerAnchorResolver` | `drawer.core` | 锚点策略和解析 |
| `DrawerInteractionController` | `drawer.core` | 触摸方向仲裁、拖动和 settle |
| `DrawerConfig` / `DrawerPresentationMode` | `drawer.config` | 通用容器配置 |
| `DrawerPage` / `DrawerNavigationController` | `drawer.navigation` | 稳定 ID 页面导航 |
| `DrawerPageHost` | `drawer.navigation` | View 页面缓存/切换 |
| `DrawerPanelContent` / `DrawerPanelHost` | `drawer.panel` | View 面板内容与 host |
| `DrawerStyle` / `DrawerColors` / `DrawerDimensions` | `drawer.style` | 样式 token |
| `DrawerPreset` / `DrawerPresets` | `drawer.preset` | 预设配置 |
| `SymbolDrawerAnchorAdapter` | `drawer.adapter` | 符号抽屉几何到锚点的适配 |

## 3. 动作和编辑器中立 API

- `SymbolActionHost`、`SymbolActionExecutor`、`SymbolActionResult`、`SymbolActionCancellation`
- `SymbolActionRegistry`、`SymbolActionHandler`、`RegistryBackedSymbolActionHost`
- `SymbolTextDocument`、`SymbolTextDocumentActionHost`、`SymbolTextEditing`
- `SymbolActionJsonCodec`、`GenericSymbolTextActions`、`BuiltinSymbolActionCatalog`

优先从 `com.zerostudio.idea.widget.drawer.action` 导入编辑器中立 API。部分旧产品包保留兼容声明；Kotlin typealias 不生成 Java 类。

## 4. 关键数据与运行时契约

- 分组/符号使用稳定 ID；Manager 和持久化层执行全量唯一性校验。
- 动作执行同步进行，不自动切线程。
- Composite 顺序执行，可能产生 PartialFailure，不自动回滚。
- 文本 offset 使用 UTF-16 code units，范围为左闭右开。
- Repository 写入成功后才发布新快照。
- 扩展注册返回 `AutoCloseable`；会话结束必须关闭句柄。
- View 页面和面板拥有 Android View attach/detach 约定。

## 5. 两个 artifact 的兼容边界

`zero-symbol-input-view-v2` 是 Android View artifact；`zero-symbol-input-view-v2-compose` 是独立 Compose artifact。二者使用相同的通用 Drawer 概念和部分 FQN，但 `DrawerContainer`、`DrawerPanelContent`、`DrawerPageHost` 的类型形态不同。不要同时依赖两个 artifact，也不要将 View API 签名用于 Compose 集成。

## 6. 排错原则

编译错误先核对 artifact 和完整包名；尤其区分 `model.DrawerConfig` 与 `drawer.config.DrawerConfig`。Java 集成还需区分真实 JVM 声明与 Kotlin typealias。运行时问题应提供 commit、task、设备 API、完整堆栈和最小复现。
