# 10 — Compose API 索引、包路径与兼容性

本文按 `zero-symbol-input-view-v2-compose` 当前生产源码整理。Compose artifact 的通用 Drawer 源码位于 `com.zerostudio.idea.widget.drawer`；符号输入 UI 仍位于 `com.zerostudio.idea.widget.editor.symbolinput.v2`。

## 1. Compose UI 入口

| API | 包 | 用途 |
|---|---|---|
| `SymbolDrawer` | `editor.symbolinput.v2` | Compose 符号输入抽屉 |
| `SymbolDrawerState` / `rememberSymbolDrawerState` | `editor.symbolinput.v2` | 符号抽屉展示状态 |
| `SymbolDrawerManagerScreen` | `editor.symbolinput.v2.manager` | Compose 分组/符号管理页 |
| `SymbolDrawerSettingsScreen` | `editor.symbolinput.v2.settings` | Compose 设置页 |
| `SymbolGroupIndicator` / `SymbolPage` | `editor.symbolinput.v2.ui` | 符号 UI 子组件 |

本 artifact 不提供 Android `SymbolDrawerView`。

## 2. 通用 Compose Drawer

| API | 包 | 用途 |
|---|---|---|
| `DrawerContainer` | `com.zerostudio.idea.widget.drawer` | Composable 通用容器 |
| `DrawerContainerState` / `rememberDrawerContainerState` | 同上 | Composition 记忆状态 |
| `DrawerConfig` / `DrawerPresentationMode` | `drawer.config` | 通用 Drawer 配置 |
| `DrawerStateHolder` / `DrawerController` | `drawer.core` | 单一状态引擎和控制接口 |
| `DrawerAnchor` / `DrawerAnchorInput` / `DrawerAnchorStrategy` | `drawer.core` | 锚点模型和策略 |
| `DrawerNestedScrollPolicy` / `DrawerNestedScrollConnection` | `drawer.core` | Compose 嵌套滚动仲裁 |
| `rememberDrawerStateHolder` / `rememberDefaultDrawerController` / `rememberDrawerStateSnapshot` | `drawer.core` | Composition 状态适配 |
| `DrawerPage` / `DrawerPageHost` | `drawer.navigation` | 稳定 ID 页面和 Composable host |
| `DrawerPanelContent` / `drawerPanelContent` / `DrawerPanelHost` | `drawer.panel` | Composable 面板 slot |
| `DrawerStyle` / `DrawerColors` / `DrawerDimensions` / `DrawerMotion` | `drawer.style` | Material/Compose 样式 token |

## 3. 领域、数据与动作

- Models：`SymbolGroup`、`SymbolEntry`、`SymbolAction`、`model.DrawerConfig`。
- Actions：`SymbolActionHost`、`SymbolActionExecutor`、`SymbolActionResult`、`SymbolActionRegistry`。
- Text document：`SymbolTextDocument`、`SymbolTextDocumentActionHost`、`SymbolTextEditing`。
- Data：`SymbolGroupRepository`、`ObservableSymbolGroupRepository`、`SymbolGroupManager`。
- Codecs/migration：`SymbolGroupStorageCodec`、`SymbolGroupBundleCodec`、`LegacySymbolDataCodec`、`LegacySymbolDataMigration`、Android migration adapter。
- Settings：`SymbolDrawerSettings`、`SymbolDrawerSettingsRepository`、`SymbolDrawerSettingsCodec`。

## 4. Extension API

`SymbolDrawerExtensionRegistry` 注册 action、legacy action、indicator、item renderer、group provider、settings tab/item 和 theme。每个注册返回 `AutoCloseable`；注册表只持有运行时贡献，不序列化 lambda 或 Composable。调用方负责句柄所有权和释放。注册成功不代表所有页面自动消费该贡献，具体消费者以当前页面实现为准。

## 5. 包和 ABI 规则

- 通用 Drawer 新包为实际源码包，不是旧路径的临时别名。
- 符号输入产品包保持 `editor.symbolinput.v2`。
- Kotlin `typealias` 仅提供 Kotlin 源码入口，不会生成独立 Java 类。
- Compose `DrawerContainer` 是 Composable 函数；旧 View artifact 的同名类型是 View class。
- 两个 artifact 不可互换，也不应同时加入同一个应用依赖图。

## 6. Compose 生命周期边界

不要求 Activity、Fragment、ViewModel 或 LifecycleOwner 作为组件状态容器。Composition、重组、remember 和 effect 取消语义仍然存在；不要将 Compose 描述成完全没有任何生命周期。
