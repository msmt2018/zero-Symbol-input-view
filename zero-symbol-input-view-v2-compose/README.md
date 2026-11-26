# Zero Symbol Input View V2 — Jetpack Compose

本模块是符号输入抽屉与通用 Drawer 的原生 Jetpack Compose 实现。页面、分页、网格、面板、管理和设置 UI 使用 Compose Foundation / Material 3；不创建传统 Android View，不使用 AndroidView / AndroidViewBinding 包装旧控件。

## 模块边界

- Gradle module：`zero-symbol-input-view-v2-compose`
- Android namespace：`com.zerostudio.idea.widget.editor.symbolinput.v2`
- 符号输入 UI：`com.zerostudio.idea.widget.editor.symbolinput.v2`
- 通用 Drawer：`com.zerostudio.idea.widget.drawer`
- UI 技术：Jetpack Compose Runtime / UI / Foundation / Material 3
- 数据和动作：编辑器中立，由宿主适配 `SymbolActionHost` / `SymbolTextDocument`

旧 View 模块 `zero-symbol-input-view-v2` 是独立兼容构建产物，不属于本模块的 Compose UI 运行时。

## 同仓库依赖

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2-compose"))
}
```

外部 Maven 消费者必须使用 Compose artifact 实际发布的坐标与成功发布的版本；不能复用旧 View artifact 坐标。

## 符号抽屉

```kotlin
@Composable
fun EditorSymbolTools(groups: List<SymbolGroup>, onInsert: (SymbolEntry) -> Unit) {
    val state = rememberSymbolDrawerState()
    SymbolDrawer(
        groups = groups,
        state = state,
        onSymbolClick = onInsert,
        onOpenManager = { /* host navigation */ },
        onOpenSettings = { /* host navigation */ }
    )
}
```

`SymbolDrawer` 使用 Compose Pager、LazyRow/Compose layout、拖动和动画。数据快照由宿主或 Repository 提供；Composable 函数体不得执行阻塞 I/O。

## 通用 Drawer

```kotlin
@Composable
fun EditorToolDrawer() {
    val state = rememberDrawerContainerState()
    DrawerContainer(
        state = state,
        title = "Editor tools",
        content = { EditorContent() }
    )
}
```

公共包为 `com.zerostudio.idea.widget.drawer`。页面使用稳定 ID 的 `DrawerPage`；面板使用 `DrawerPanelContent` / `drawerPanelContent`。

## 管理、设置和数据

- `SymbolDrawerManagerScreen`：分组/符号 CRUD、排序、复制、批量操作和 JSON 导入导出。
- `SymbolDrawerSettingsScreen`：不可变设置快照、更新回调和重置。
- `SymbolGroupManager`：校验领域数据并执行仓储命令。
- `SymbolGroupRepository` / codecs：持久化、bundle 导入导出和旧数据迁移。

Repository 和 Manager 位于 UI 之下；Composable 不拥有存储格式或阻塞文件操作。

## 自底向上分层

1. 编辑器中立文本/动作 API。
2. 领域模型、校验、Repository、Codec 和迁移。
3. Drawer 锚点、状态机、拖动和 nested-scroll 策略。
4. 导航、面板、自适应窗口、样式和扩展注册。
5. Compose 组件、管理页、设置页和无障碍。

## 生命周期边界

本模块不以 Activity / Fragment / ViewModel / LifecycleOwner 管理页面状态，也没有 View attach/detach 或 View factory 契约。Compose 的 Composition、重组、remember scope 和 effect 取消语义仍然存在；“零生命周期”只表示不耦合传统 Android View 页面生命周期。

## API 文档

中文与英文文档位于 `api-docs/`。请从 [API 文档中心](api-docs/README.md) 进入；API 签名以当前 Kotlin 源码为准。CI/source boundary 结果必须与文档状态分开记录。
