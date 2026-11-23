# Zero Symbol Input View V2 — Compose API 文档中心

本目录只描述 zero-symbol-input-view-v2-compose 模块。仓库中的旧 zero-symbol-input-view-v2 模块仍是独立的传统 View 兼容构建产物，不属于 Compose UI 运行时。

## 模块

- Gradle module：zero-symbol-input-view-v2-compose
- Namespace：com.zerostudio.idea.widget.editor.symbolinput.v2
- UI：Jetpack Compose Runtime、UI、Foundation、Material 3
- 数据/动作：SymbolGroupRepository、SymbolGroupManager、SymbolActionExecutor、SymbolTextDocument
- 主要页面：SymbolDrawer、SymbolDrawerSettingsScreen、SymbolDrawerManagerScreen
- 通用容器：DrawerContainer、DrawerPageHost、DrawerPanelHost

本模块不创建 Android View，不使用 AndroidView/AndroidViewBinding 包装旧控件，也不要求 Activity、Fragment、ViewModel 或 LifecycleOwner 作为组件状态容器。Compose 仍有 Composition、重组和 effect 生命周期；“无传统 View 生命周期耦合”不等于“完全没有生命周期”。

## 文档地图

| 文档 | 内容 |
|---|---|
| 01 — 快速开始 | 模块依赖、Composable 入口、编辑器动作适配 |
| 02 — SymbolDrawer Compose API | 符号抽屉状态、页面、动作回调 |
| 03 — 数据模型与动作类型 | 分组、符号、typed action 和 legacy 元数据 |
| 04 — 动作注册与执行 | Host、Executor、Registry、取消和结果 |
| 05 — 仓储、管理器与迁移 | Repository、Manager、codec、V1 数据迁移 |
| 06 — 设置中心 | 设置快照、Repository、codec 和 Compose 设置页 |
| 07 — 通用 Drawer 框架 | 容器、锚点、状态机、手势和 Compose slots |
| 08 — 页面导航、面板与样式 | DrawerPage、HorizontalPager、Composable panel、主题 |
| 09 — 自适应窗口 | WindowAdaptiveInfo、Insets、布局解析和停靠策略 |
| 10 — API 索引与兼容性 | 模块分层、公共包和 Kotlin/Java ABI |
| 11 — 手势与内置文本动作 | 嵌套滚动策略、动作目录、通用文本命令 |
| 12 — 扩展注册框架 | Composable renderer、indicator、settings 与 action 扩展 |
| 13 — 公共包与编辑器中立文档契约 | UTF-16 文档 API、事务和宿主 adapter |
| 14 — Drawer 公共 API 参考 | 通用 Compose Drawer 对外 API |
| 15 — 符号控件扩展实战 | 注册、Composable 内容和扩展约束 |
| 16 — 测试、发布与排错 | Compose 测试、边界审计和 CI |

## 本地集成

在宿主工程 settings.gradle.kts 中包含本仓库的 Compose module，并在应用模块依赖中引用：

implementation(project(":zero-symbol-input-view-v2-compose"))

不要把旧模块 zero-symbol-input-view-v2 当作 Compose 依赖。远程 Maven 发布坐标是否可用，应以该 Compose module 实际发布配置和成功发布记录为准；仓库中旧 View 模块的 Maven 验证不能替代 Compose artifact 发布验证。

## 推荐阅读路径

- 集成符号抽屉：01 → 02 → 04 → 06
- 编辑器适配：03 → 04 → 11 → 13
- 扩展 Composable 内容：12 → 15
- 复用通用 Drawer：07 → 08 → 09 → 14
- 维护与发布：10 → 16

## 关键边界

1. 数据模型、Repository 和文本动作不持有 UI View 或 Activity/Fragment。
2. UI 使用 Composable slot；页面身份使用稳定 ID，不使用列表 index 作为身份。
3. 文本 offset 使用 UTF-16 code unit，范围为左闭右开。
4. 运行时扩展 handler/Composable 不序列化；只持久化稳定 ID 和 payload。
5. Kotlin typealias 不生成 Java 可见的独立 JVM 类。
6. CI 必须检查源码、发布产物和 runtimeClasspath；不得通过排除依赖或禁用任务掩盖边界问题。

