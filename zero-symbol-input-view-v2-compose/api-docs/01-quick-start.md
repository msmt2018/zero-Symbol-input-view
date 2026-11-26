# 01 — Jetpack Compose 快速开始

## 1. 模块定位

本模块是 Compose 原生实现。符号抽屉、通用 Drawer、分页、面板、管理页和设置页均使用 Jetpack Compose Foundation / Material 3。模块源码不创建传统 Android View；组件 API 不要求宿主提供 Activity、Fragment、ViewModel 或 LifecycleOwner。

Compose 仍具有 Composition、重组和副作用管理语义。这里的“无页面生命周期耦合”指组件不依赖传统 View / Activity / Fragment 生命周期回调，并非声称 Compose 完全没有生命周期。

## 2. Gradle 依赖

同仓库集成：

    dependencies {
        implementation(project(":zero-symbol-input-view-v2-compose"))
    }

外部 Maven 集成请使用 Compose 模块实际发布的坐标和已发布版本。不要把旧 View 模块的 Maven 坐标误认为 Compose 版本；发布配置以仓库根目录发布工作流为准。

## 3. 符号抽屉

    @Composable
    fun EditorSymbolTools(
        groups: List<SymbolGroup>,
        onInsert: (SymbolEntry) -> Unit
    ) {
        val state = rememberSymbolDrawerState()
        SymbolDrawer(
            groups = groups,
            state = state,
            onSymbolClick = onInsert,
            onSymbolLongClick = { entry ->
                // 将长按动作交给宿主动作适配器
            },
            onOpenManager = { /* 宿主导航 */ },
            onOpenSettings = { /* 宿主导航 */ }
        )
    }

SymbolDrawer 负责分组分页、符号单元格、展开/折叠手势和动画。SymbolDrawerState 只保存 Compose 展示状态，不持有 View、Context、Activity、Fragment、Adapter 或 Lifecycle 对象。

## 4. 通用 Drawer

    @Composable
    fun EditorToolDrawer() {
        val state = rememberDrawerContainerState()
        val pages = listOf(
            DrawerPage(
                id = "symbols",
                title = "Symbols",
                content = drawerPanelContent { Text("Symbol tools") }
            )
        )
        DrawerContainer(
            state = state,
            title = "Editor tools",
            pages = pages,
            selectedPageId = "symbols",
            onPageSelected = { /* 更新选中的稳定 page ID */ },
            onDismiss = { state.collapse() }
        ) {
            EditorContent()
        }
    }

主内容、页面内容和面板内容都是可组合 lambda。分页由 Compose HorizontalPager 承载；没有 View factory、手动 attach/detach 或手动 dispose 契约。

## 5. 管理页与设置页

SymbolDrawerManagerScreen 接收 SymbolGroupRepository；SymbolDrawerSettingsScreen 接收不可变设置快照和更新回调。Repository、Codec、SymbolGroupManager 等数据能力位于 UI 之下，Composable 不应直接执行阻塞磁盘 I/O。

管理页支持分组和符号 CRUD、排序、JSON 导入导出，以及批量复制、移动、删除等操作。持久化命令由 SymbolGroupManager 执行。

## 6. 动作执行边界

符号 UI 不依赖特定编辑器。宿主实现 SymbolActionHost 或使用编辑器中立的文本 API，再由 SymbolActionExecutor 执行 SymbolAction。动作执行与 Compose 绘制分离；不要把编辑器 View 或 Activity 引入数据/动作层。

## 7. 迁移约束

- UI 使用 Compose Foundation / Material 3。
- 禁止 AndroidView、AndroidViewBinding、ViewGroup、RecyclerView、ViewPager2 和传统 View 控件。
- 不把 Activity / Fragment / LifecycleOwner / ViewModel 作为组件状态容器。
- 底层模型、动作、仓储、校验、锚点和交互策略保持 UI 中立，不应无理由标注为 @Composable。
- 页面副作用使用 Compose effect API，并确保 key 与取消语义正确。
- Composition 生命周期仍然存在；不能将其描述为完全没有任何生命周期。
