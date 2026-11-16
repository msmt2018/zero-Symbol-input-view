# Compose V2 源码审计与零生命周期重构计划

> 审计基线：`feat/compose-symbol-drawer-v2` / `d59e9349e2edf064d75148b1ac6068753af5c3b4`  
> 审计日期：2026-11-16  
> 目标模块：`zero-symbol-input-view-v2-compose`

## 1. 不可变边界

- `zero-symbol-input-view-v2` 是旧 View 产品，整个开发期间保持只读；不修改它的源码、资源、Gradle、测试、文档或发布配置。
- 唯一开发目标是 `zero-symbol-input-view-v2-compose`。
- Compose 模块必须是独立的原生 Jetpack Compose 实现，不依赖旧 View 模块，不使用 `AndroidView` / `AndroidViewBinding`，也不保留 View UI fallback。
- 可复用内容仅限 UI 无关的数据模型、JSON/旧格式兼容、验证、稳定 ID、编辑器无关动作协议和纯 Kotlin 算法。混有 View/Activity/Context 的文件必须先拆分。
- PR #55 在所有迁移、测试、边界审计和 CI 验收完成前保持 Open + Draft。

## 2. 当前源码盘点

Git tree 共盘点到 Compose 模块 **215 个 tracked files**，其中 **159 个 Kotlin 源文件**：
- `src/main`：94 个 Kotlin 文件
- `src/test`：53 个 Kotlin 文件
- `src/androidTest`：12 个 Kotlin 文件

主源码分布覆盖以下完整区域：

| 区域 | 文件/职责 | 审计结论 |
|---|---|---|
| `widget/drawer` 公共 API | DrawerApi、Core、Panel、Navigation、Style、Adapter、Extension、Action API | 需要逐个核对是否仍有手动 Controller/Adapter 生命周期；公共 UI 最终只暴露 Composable、State、不可变配置和事件回调 |
| `widget/drawer/compose` | Compose DrawerContainer | 原生 Compose 基础，可继续完善；不能依赖旧 generic drawer View |
| `symbolinput/v2/compose` | SymbolDrawer、State、Detent、Pager、Indicator、扩展快照、Manager/Settings Screen/Route | 原生 Compose 原型，需完成手势并发、完整功能、扩展契约和零生命周期审计 |
| `symbolinput/v2/model`、`action` | Symbol/Group/Action、执行协议、JSON codec、编辑器无关文本动作 | 优先保留纯 Kotlin 语义；逐个确认不导入 View/Activity/Context |
| `symbolinput/v2/data` | Legacy adapter/catalog、数据迁移、Group/Bundle/Storage codec、Repository | 保留 JSON 与数据兼容；Repository 接口和 Android 存储实现必须拆分 |
| `symbolinput/v2/settings` | Settings 模型/codec/repository/存储/旧 Picker/旧 Screen | 保留设置模型和格式；重写 UI，拆分持久化与文件访问 |
| `symbolinput/v2/adaptive` | 尺寸、网格、窗口策略、折叠屏模型、Observer/Controller | 纯尺寸/策略可保留；基于 View 的窗口观察必须改为 Compose WindowInsets/WindowSizeClass/状态输入 |
| `symbolinput/v2/drawer` | 旧 DrawerContainer、Controller、Anchor、Navigation、Panel、Preset、Style | 不能因包名叫 drawer 就整体保留；纯策略可抽取，View/手动刷新/手动释放控制器必须重写 |
| `symbolinput/v2/ui` | 旧 GroupIndicator、PageAdapter、HostThemeColors | Indicator 与 Adapter 必须 Compose 重写；只保留真正平台无关的颜色/模型映射 |
| `manager`、`settings` | 旧管理页与设置页 | 必须以 Material 3 Compose 重写，不能将旧页面作为内部实现 |
| 测试目录 | 53 个 unit tests、12 个 instrumentation tests | 按纯领域、Compose UI、旧 View host 三类重新归属；旧 View UI 测试不能算 Compose 覆盖 |

### 2.1 已明确的 View 耦合文件

下列文件不能进入最终 Compose artifact，必须重写、拆分或删除：

1. `SymbolDrawerView.kt`
2. `ui/SymbolGroupIndicator.kt`
3. `ui/SymbolPageAdapter.kt`
4. `manager/SymbolDrawerManagerScreen.kt`
5. `settings/SymbolDrawerSettingsScreen.kt`
6. `extension/SymbolDrawerExtensionRegistry.kt`
7. `drawer/DrawerContainer.kt`
8. `settings/JsonDocumentPicker.kt`
9. `adaptive/AdaptiveWindowObserver.kt`
10. `adaptive/AdaptiveWindowController.kt`（直接持有 Android View/Observer）

另外，旧 drawer 的 Controller/Adapter/Host 类需要逐个追踪调用关系；如果它们暴露手动 start/stop/close、View 类型或手动刷新语义，就不能作为公共 Compose UI API 保留。

### 2.2 依赖边界

当前 Compose Gradle 仍显式声明：
- `androidx.recyclerview:recyclerview`
- `androidx.viewpager2:viewpager2`
- `com.google.android.material:material`（View 版 Material）

这些必须在相应 View 源码删除后移除。还需审查：
- `androidx.activity:activity-ktx` 是否仅被旧 Picker 使用；
- `androidx.activity:activity-compose` 是否仅作为 Compose SAF Route 的平台适配；
- `androidx.lifecycle:lifecycle-runtime-compose` 是否仍为公共 API 的强制依赖。最终公共 API 不得要求宿主提供 LifecycleOwner；不需要生命周期感知收集时优先使用 Composition 范围内的 `collectAsState`。

当前 `verifyComposeUiBoundary` 只检查两个 Compose package root；`auditComposeMigration` 虽覆盖 main/test/androidTest，但目前只是 informational；`verifyComposeArtifactBoundary` 尚未接入 `check`。现有扫描规则也不足以检查反射、全限定名、别名导入和解析后的传递依赖，必须增强。

## 3. 零生命周期公共 API 契约

这里的“零生命周期”指**使用者不需要管理 UI 生命周期**，不是宣称 Android/Composition/协程在运行时不存在生命周期。

### 3.1 公共 API 允许

- `@Composable` Screen、Route、Drawer、Indicator、Cell、Dialog 和扩展 slot。
- 不可变配置、State、State holder、稳定 ID。
- 明确的业务事件回调，例如 `onSymbolClick`、`onGroupSelected`、`onSettingsChange`。
- Composition 范围内的 `remember`、`rememberSaveable`、`LaunchedEffect`、`DisposableEffect` 和 `rememberCoroutineScope`，由库内部封装管理。
- 平台适配层按需使用 Context、ContentResolver、Activity Result、WindowInsets 等 Android API，但不把 Activity/View 生命周期交给调用者。

### 3.2 公共 API 禁止

- Screen/控件的 `create/start/stop/destroy/attach/detach`。
- `onCreate/onStart/onResume/onPause/onStop/onDestroy`。
- `createView/bindView/updateView/recycleView`、View attach/recycle callbacks。
- `View`、`ViewGroup`、`MotionEvent`、RecyclerView/ViewPager2 等类型。
- 强制继承 Activity/Fragment/ViewModel，或强制传入 LifecycleOwner。
- 要求宿主手动刷新 UI、手动取消 UI Job、手动释放 UI 控件资源。
- 将 `AutoCloseable` 注册句柄作为每个 UI 扩展的必需生命周期协议。扩展配置优先采用不可变 builder/DSL/snapshot；确需动态注册时由 Composition scope/宿主容器集中托管，而不是把控件销毁责任交给调用者。

### 3.3 扩展 API 规则

- item renderer、indicator、settings tab/item、drawer region 全部是 Composable renderer/slot。
- 扩展交互使用有类型的业务事件，不允许 onAttached/onDetached/onDispose 作为扩展公共回调。
- 扩展只接收不可变数据与事件回调，不持有 View、不绑定/回收 View。
- 异步加载通过 suspend provider/Flow + Composition 范围收集；离开组合自动取消 UI 任务。
- 注册表只管理扩展数据与快照，不充当 UI 生命周期管理器。

## 4. 执行计划（按依赖顺序）

### 阶段 0：审计基线与自动防线
- 固定旧 View 模块只读边界、Compose artifact 独立性和 PR Draft 约束。
- 为所有 main/test/androidTest 源码生成机器可读的 View/Lifecycle/平台 API 清单。
- 增强全 artifact 源码扫描和 Gradle resolved dependency 检查。
- 将严格边界任务最终接入 `check`；迁移完成前保留 informational audit，不能假报通过。

### 阶段 1：抽屉状态与手势并发正确性
- 完成 projection、coalesced mutation queue、in-flight batch、ack/release/retry 的严格协议。
- cancellation/failure 必须恢复尚未应用的 in-flight delta；旧 worker 不得释放新 lease。
- handle drag 与 nested scroll 共用唯一 mutation pipeline。
- range/窗口变化、反向滚动、零 range、边界饱和、fling/settle barrier 均有纯 Kotlin 单测与 Compose UI 测试。
- settle 前必须 drain 所有已接受 mutation；不得在 pending/in-flight work 存在时 reconcile。

### 阶段 2：纯领域层与 Android 平台适配拆分
- 抽离 Symbol/Group/Action、JSON schema/codec、legacy migration、验证、稳定 ID、编辑器无关动作。
- Repository contract 与 SharedPreferences/DataStore 实现分离。
- SAF 输入输出限制大小、流式读写、错误可见；Activity Result 只放在 Compose Route/平台适配层。
- 确保领域层没有 Android View、Activity、Context、Lifecycle 类型依赖。

### 阶段 3：核心 Drawer 原生 Compose 完整实现
- 重构 DrawerSurface、Anchor/Detent、NestedScroll、Handle、Pager、LazyVerticalGrid。
- 保留既有交互契约：拖动方向/锚点/展开比例、分页手势、连续指示器显露、符号短按/长按、预取上限、稳定 key、状态恢复。
- 支持空数据、数据热更新、动态尺寸、无障碍语义、键盘/focus、触控目标、减少动态效果和性能约束。
- 不复制旧 View 实现的类结构；按 Compose state/layout/gesture 的真实模型重新设计。

### 阶段 4：Indicator 与扩展 API
- 将所有内置指示器样式迁移为 Compose Canvas/布局/动画组件。
- 重写扩展注册与快照；删除旧 View extension registry。
- 扩展项、指示器、动作、分组 Provider、主题、设置 tab/item 全部采用 Composable slots + typed events。
- 消除扩展 API 的 attach/detach/dispose 生命周期回调与宿主手动关闭 UI 注册句柄要求。

### 阶段 5：Manager / Settings / Dialog / SAF
- 完整迁移分组增删改排序、符号增删改排序、批量操作、搜索、JSON 导入导出、备份恢复、设置分类和扩展设置。
- 所有页面直接作为 Composable Screen/Route；State 驱动，不提供手动创建/刷新/销毁方法。
- 覆盖加载、空态、错误、确认、取消、并发写入和数据迁移场景。

### 阶段 6：自适应布局与平台集成
- 用 Compose WindowInsets、WindowSizeClass 和窗口状态输入替代 View layout listener。
- 覆盖手机/平板/横屏/多窗口/折叠屏/侧边停靠。
- 纯布局解析器可保留，View Observer/Controller 必须删除。
- 平台集成只作为可选适配，不使公共 UI API 依赖 LifecycleOwner。

### 阶段 7：测试、性能与无障碍
- 保留纯 Kotlin 领域单测；为 Drawer/Indicator/Manager/Settings/Extension 建立 Compose UI 测试。
- 删除只验证旧 View 控件的 instrumentation tests，替换为 Compose 交互测试。
- 覆盖高频手势、快速反向、取消、重组、恢复、超大分组、空数据、窗口 resize 和扩展并发注册。
- 检查 recomposition、稳定 key、Lazy item、Pager 预取、动画与内存；不以单纯“能编译”作为性能验收。

### 阶段 8：独立发布验收
- 移除全部 View UI 源码、View-only resources/tests/dependencies。
- 验证 Compose artifact 不依赖旧 View module，且 resolved runtime/published POM 不泄漏 View-only 依赖。
- 严格边界检查覆盖 main/test/androidTest 和发布产物。
- Debug/Release、单元测试、Compose instrumentation、Maven publication、消费端 sample 全部通过。
- 更新中英文 README：介绍控件能力、编辑器接入、扩展 API、JSON 数据互通和 API 支持；不添加无关构建教程。
- 全部通过后才允许将 PR #55 从 Draft 转为 Ready；未获明确指示前不合并。

## 5. 当前 CI 与下一步

基线 commit `d59e934`：
- Debug APK 构建：通过。
- Maven publication validation：通过。
- 旧 V2 library build/unit test：通过。
- Compose Debug/Release 构建步骤：通过；Compose unit tests：失败，382 项中 2 项失败，均在 `DrawerNestedScrollCoordinatorTest`。
- 旧 V2 Android 21/29/35 instrumentation：存在旧 View UI 测试失败，不属于 Compose 模块迁移目标；必须保留原样并单独记录，不得修改旧模块来消除失败。

本轮开发将先修正浮点边界断言，并把 mutation queue 升级为 in-flight acknowledgement/recovery；同时移除 Compose 扩展 API 的 attach/detach 回调，改用业务事件。随后继续逐个迁移 View 耦合文件与扩展 API，而不是把当前原型当作完成版本。
