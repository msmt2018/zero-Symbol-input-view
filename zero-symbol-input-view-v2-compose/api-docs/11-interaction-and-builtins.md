# 11 — Compose 手势适配与通用内置文本动作

本页描述当前 Compose 模块的交互 API。旧版 ViewGroup 拦截触摸、ValueAnimator 和 onAttached/onDetached 接口不属于本模块。

## 1. DrawerNestedScrollPolicy

包：drawer.core。

DrawerNestedScrollPolicy 是纯 Kotlin 的嵌套滚动仲裁策略，不依赖 Compose UI，也不修改抽屉状态。

输入 DrawerNestedScrollInput 包含 availableDeltaY、currentHeightPx、minimumHeightPx、maximumHeightPx、childCanScrollBackward 和 phase。输出 DrawerNestedScrollResult 包含 consumedDeltaY 与 remainingDeltaY，保证不超额消费。

- PRE_SCROLL：抽屉尚未展开到最大高度时，可先消费向上滚动。
- POST_SCROLL：子内容消费后剩余的向下滚动可用于收起抽屉。
- 输入尺寸、当前高度和 delta 必须有效。

Compose DrawerNestedScrollConnection 负责把 Compose NestedScrollConnection 回调转换为该策略/状态机输入。Compose 的 pointer input 与 nested scroll 适配层不应反向污染纯策略。

## 2. DrawerStateHolder 与 DrawerInteractionController

DrawerStateHolder 是纯 Kotlin 状态引擎，不依赖 Compose Runtime。它管理锚点、当前高度、拖动、动画目标和状态快照，并通过 addStateListener/removeStateListener 发布不可变状态。它提供几何更新、开始/更新/结束拖动、取消拖动、同步高度和锚点移动等操作。

rememberDrawerStateSnapshot(holder) 是 Compose 适配层：在当前 Composition 中订阅状态，并在 DisposableEffect 离开 Composition 时释放 listener。Compose UI 应通过该适配器读取快照，不应把 mutableStateOf 放回底层状态引擎。

DrawerInteractionController 是旧双锚点 API 的弃用兼容 facade，内部委托 DrawerStateHolder。新集成应使用 DrawerStateHolder 和 DrawerAnchorStrategy，以支持多个语义锚点。

SymbolDrawerDragPolicy 是内部纯策略辅助类，用于判断纵向拖动是否可由抽屉接管。

## 3. 锚点策略

DrawerAnchorStrategy 与 DrawerAnchorResolver 定义锚点身份、像素高度和语义状态。ExistingHeight、ThreeStage、ContentBased 和 Custom 策略由 resolver 统一规范化。页面容器只提交测量数据和策略，不应另建第二份高度真值。

## 4. Compose 手势与嵌套滚动

DrawerContainer 使用 Compose pointer input、nested scroll 和动画 API 协调抽屉交互：

- 横向手势留给横向分页。
- 向上拖动优先展开抽屉，达到最大高度后由子内容继续滚动。
- 向下滚动先让子内容消费；剩余向下 delta 才交给抽屉收起。
- 触摸取消、手势结束和动画目标由 Compose 输入作用域协调。
- 宿主不要再叠加第二套 GestureDetector 或传统 View 拦截逻辑。

## 5. BuiltinSymbolActionCatalog

BuiltinSymbolActionOption 描述 Manager 可选的内置动作，字段为 commandId、label、payloadHint 和 hasPayload。BuiltinSymbolActionCatalog 提供稳定命令目录；命令能否执行仍取决于宿主是否实现相应 Host 能力。

目录中包含行、选区、光标、文档及通用插入命令。不要把目录项存在误认为所有编辑器都实现了该命令。

## 6. GenericSymbolTextActions

GenericSymbolTextActions 通过 SymbolTextDocument 执行通用文本操作：

- 行：复制、删除、清空、替换、插入空行、合并、移动、选择、大小写、空白清理、注释和缩进。
- 选区：删除、复制、大小写、包围、缩进/反缩进、反转/排序行、注释。
- 光标：单词移动/删除、选择单词/行、行首/行尾、文档首尾和扩展选择。
- 文档：全选、空白清理、末尾换行、删除空白行、注释。
- 通用插入：insert_payload。

createRegistry() 创建独立的 SymbolActionRegistry；supportedCommandIds() 返回当前实现支持的 ID 快照。文档级全量操作限制为 32 MiB 和 200,000 行，超过限制时拒绝执行。

## 7. SymbolTextEditing

纯文本计算辅助 API，包括宏展开、Unicode code point 删除范围和复制行插入计算。它不持有 Android View，也不负责直接更新 UI；宿主通过 SymbolTextDocument 或自己的文档事务提交计算结果。

## 8. UTF-16 与编辑边界

所有文档 offset 使用 Kotlin String UTF-16 code unit 索引，范围为左闭右开。适配器必须正确处理 surrogate pair、CRLF、反向选区和只读文档。批量编辑按右到左顺序应用，避免较早替换改变后续 offset。