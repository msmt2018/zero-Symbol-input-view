# 10 — Compose API 索引与兼容性

本索引仅描述 zero-symbol-input-view-v2-compose 模块。旧 View 模块保留为独立兼容产物，不属于本 Compose API。

## 1. 主要 API 分层

| 层 | 主要类型 | 职责 |
|---|---|---|
| 模型 | SymbolGroup、SymbolEntry、SymbolAction、DrawerConfig | 符号与动作数据、配置校验 |
| 动作 | SymbolActionExecutor、SymbolActionRegistry、SymbolActionHost | 动作验证、路由与宿主能力边界 |
| 文本 | SymbolTextDocument、SymbolTextDocumentActionHost、GenericSymbolTextActions | 编辑器中立的 UTF-16 文本操作 |
| 数据 | SymbolGroupRepository、SymbolGroupManager、StorageCodec、BundleCodec、Migration | 持久化、事务式快照更新、导入迁移 |
| 设置 | SymbolDrawerSettings、SettingsRepository、SettingsCodec | 不可变设置与版本化存储 |
| 抽屉核心 | DrawerAnchor、DrawerAnchorResolver、DrawerStateHolder、DrawerNestedScrollPolicy | 纯 Kotlin 锚点、几何、拖动状态机与嵌套滚动策略 |
| Compose 状态适配 | rememberDrawerStateSnapshot、DefaultDrawerController、DrawerControllerComposition | 将纯状态引擎桥接到 Composition 并驱动界面 |
| Compose 适配 | DrawerContainer、DrawerPageHost、DrawerPanelHost、AdaptiveWindowObserver、rememberDrawerStateSnapshot、rememberDrawerNavigationSnapshot、rememberAdaptiveWindowSnapshot | Compose 布局、分页、面板和状态/窗口快照桥接 |
| 扩展 | SymbolDrawerExtensionRegistry 及 Composable 扩展接口 | 动作、指示器、单元格和设置贡献 |
| 页面 | SymbolDrawer、SymbolDrawerSettingsScreen、SymbolDrawerManagerScreen | 符号抽屉、设置和数据管理界面 |

## 2. 主要 Compose 入口

- SymbolDrawer：符号输入抽屉入口。
- DrawerContainer：通用抽屉容器。
- DrawerPageHost：基于 HorizontalPager 的页面宿主。
- DrawerPanelHost：PEEK/EXPANDED 面板 slot。
- SymbolDrawerSettingsScreen：设置 Composable。
- SymbolDrawerManagerScreen：分组与符号管理 Composable。
- AdaptiveWindowObserver：Compose 窗口尺寸与 Insets 适配。

以上均为 Composable API，不是 View 子类，不接受 Context 来创建界面，也不提供 XML inflate 入口。

## 3. 公共包与 Kotlin ABI

com.zerostudio.idea.widget.drawer 下的 facade 文件目前包含 Kotlin typealias。它们提供 Kotlin 源码导入路径，但不会在 JVM 字节码中生成同名 Java 类。Java 消费者不能把 typealias 当作 Java ABI。

若需要稳定 Java API，必须物理声明 public class/interface 并添加 Java 编译 fixture；仅增加 typealias 文件不构成 Java 兼容完成。

## 4. 兼容性边界

- zero-symbol-input-view-v2-compose 与 zero-symbol-input-view-v2 是不同模块。
- Compose 模块不能通过 AndroidView/AndroidViewBinding 包装旧 View 控件。
- 旧模块仍有 Activity、Fragment、XML、View 和传统页面测试，不代表 Compose 模块也允许这些 API。
- V1 数据格式只通过明确的 legacy codec/migration adapter 读取；不应把旧 View 控件作为运行时 UI 依赖。
- 变更持久化 JSON、稳定 ID 或公开动作语义时，必须同步升级 schema/兼容测试。

## 5. 生命周期语义

Compose 页面由 Composition 管理，remember 状态与 LaunchedEffect 等副作用依赖 Composition/key 作用域。模块不要求宿主提供 LifecycleOwner、ViewModel 或 Fragment View 生命周期回调。不要将这一边界表述为“Compose 没有生命周期”。

## 6. 排错入口

- 构建失败：先检查失败 Gradle task 和首个 Kotlin/AGP 错误，不要只看最后一行 BUILD FAILED。
- Compose source boundary 失败：检查模块生产/测试源码是否引入传统 View、Activity/Fragment 或 Lifecycle API。
- Runtime dependency boundary 失败：检查实际 runtimeClasspath 最短依赖路径，不要通过排除任务或弱化规则掩盖传递依赖。
- 页面状态异常：检查稳定 page ID、remember key 和 state holder 是否被不相关会话共享。
- 数据不一致：检查 Repository 写入成功后才发布新快照的顺序。
