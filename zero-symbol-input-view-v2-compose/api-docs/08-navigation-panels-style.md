# 08 — Compose 页面导航、面板与样式 API

本页以当前 Compose 模块源码为准。页面和面板内容是可组合内容，不提供传统 View factory、attach/detach 回调或手动缓存 API。

## 1. DrawerPage

包：drawer.navigation。

DrawerPage 是不可变页面描述，字段如下：

- id：稳定且非空的页面 ID；同一导航集合中必须唯一。
- title：非空标题。
- content：DrawerPanelContent，封装 ColumnScope.() -> Unit 的 Composable 内容。
- icon：可选的 Composable 图标 lambda。

页面身份必须使用 id，不应把列表 index 当作持久身份。页面内容在 Pager 对应页面进入 Composition 时组合；页面离开 Composition 后，其 remember 状态和副作用按 Compose 规则处理。

## 2. DrawerNavigationController

这是页面选择与稳定 ID 管理器。Controller 自身不导入 Compose Runtime；它通过不可变 state/snapshot getter 和 listener 发布更新。DrawerPage 的 content/icon 是 Composable 契约，因此该导航 API 属于 Compose 模块，但状态控制器不直接持有 Compose snapshot state。

主要操作：

- submitPages(value, preserveSelection)：提交页面列表，校验 ID 唯一；尽可能保留原选择，否则选择第一项。
- selectPage(id) / selectPage(index)：选择页面；目标不存在或索引越界返回 false。
- pages()：返回页面列表快照。
- selectedPage()：返回当前页面或 null。
- selectedIndex()：返回当前索引；无选择时为 -1。
- state / snapshot()：读取当前 selectedPageId 和 pageIds 快照。
- addListener(listener) / removeListener(listener)：注册和移除同步状态监听；新增监听时立即收到当前快照。

**线程契约：** DrawerNavigationController 是单线程约束对象，不是线程安全对象。调用方必须在同一个线程串行执行所有操作，包括读取状态、提交页面、切换页面、注册监听器和移除监听器。状态变化通知在发布操作所在的线程同步执行；addListener 注册时的初始快照也在注册线程同步回调。Compose 宿主必须在 UI/Composition dispatcher 上调用控制器操作，使同步监听回调能够安全更新 Compose 状态。Controller 不会自行切换线程，也不会持有线程或 dispatcher。

Controller 对重入通知进行排队，避免监听器在通知过程中再次切页造成观察顺序倒置。监听器异常会在完成当前通知队列后向调用方传播。

DrawerNavigationController 本身不依赖 Compose Runtime；rememberDrawerNavigationSnapshot(controller) 是 Composition 适配器，负责订阅状态并在 DisposableEffect 结束时移除 listener。Controller 不持有 Compose snapshot state；但 DrawerPage 页面描述包含 Composable 内容槽，因此该类型仍属于 Compose 模块 API，而不是完全独立的 Kotlin/JVM API。

## 3. DrawerPageHost

DrawerPageHost 是 Composable 页面承载器，内部使用 Foundation HorizontalPager 和 rememberPagerState。

- 以 DrawerPage.id 作为 Pager key，保持页面重排时的身份稳定。
- selectedPageId 改变时将 Pager 滚动到对应页面。
- 用户翻页 settled 后回调 onPageSelected。
- 页面列表为空时渲染空容器。
- 不提供 Context、View 缓存、measureCurrentContentHeight、attach/detach 或 dispose 方法。

宿主应在自己的 Compose 导航层管理页面选择和 Back 行为；DrawerPageHost 不接管应用导航栈。

## 4. DrawerPanelContent 与 DrawerPanelHost

DrawerPanelContent 是一个 Composable slot：

- content：ColumnScope.() -> Unit。

drawerPanelContent { ... } 是便捷工厂。

DrawerPanelSlot 有 PEEK 和 EXPANDED 两种语义。DrawerPanelHost 接收 content、modifier 和 slot，在当前 Composition 中组合内容。slot 用于区分调用场景；面板内容的状态与副作用仍由 Compose Composition 管理。

## 5. 样式模型

包：drawer.style。

DrawerStyle 聚合以下不可变配置：

- DrawerThemeMode：FOLLOW_HOST、LIGHT、DARK。
- DrawerColorAttributes：宿主语义颜色属性名称。
- DrawerColors：surface、primaryText、secondaryText、accent、outline、scrim；overlay 只覆盖非 null 角色。
- DrawerDimensions：peek/expanded 高度、圆角、内边距、面板间距和 handle 尺寸，单位 dp。
- DrawerTypography：标题/正文/说明字号、字重和可选字体族名称。
- DrawerMotion：settle 时长、拖动阻尼、fling 速度范围和 spring 开关。
- DrawerShape：四角圆角和描边宽度。
- elevationDp、drawScrim、clipContentToShape。

配置构造会校验有限数值和合法范围。DrawerThemeResolver 根据当前 Material 3 主题和 DrawerStyle 解析 Compose 绘制所需的颜色/样式；不创建主题 Context，也不持有 Activity。

## 6. 集成边界

- DrawerContainer 负责 Compose 容器、布局、拖动和内容槽位。
- DrawerNavigationController 负责页面身份与选择状态。
- DrawerPageHost 负责 Compose Pager 展示。
- DrawerPanelContent 负责声明页面/面板 Composable。
- 宿主负责应用导航、系统 Back、窗口 Insets 和编辑器布局。
- 不要使用 AndroidView、AndroidViewBinding、FrameLayout、RecyclerView 或 ViewPager2 包装旧实现。

Compose 有 Composition、重组和 effect 启停语义；这里没有传统 View/Fragment 页面生命周期回调，不代表 Compose 完全没有生命周期。
