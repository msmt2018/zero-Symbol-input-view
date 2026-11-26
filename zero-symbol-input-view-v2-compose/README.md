# Zero Symbol Input View V2 — Jetpack Compose

本模块是符号输入抽屉和通用 Drawer 的 Jetpack Compose 原生实现。页面、分页、网格、面板、管理界面和设置界面使用 Compose Foundation / Material 3；不创建传统 Android View，不使用 AndroidView / AndroidViewBinding 作为兼容包装。

## 模块边界

- 模块：zero-symbol-input-view-v2-compose
- 命名空间：com.zerostudio.idea.widget.editor.symbolinput.v2
- 通用 Drawer：com.zerostudio.idea.widget.editor.symbolinput.v2.drawer
- UI 技术：Jetpack Compose Runtime / UI / Foundation / Material 3
- 数据和动作：独立于具体编辑器实现，由宿主适配 SymbolActionHost / SymbolTextDocument。

本仓库的旧 View 模块 zero-symbol-input-view-v2 是独立兼容构建产物，不属于本 Compose 模块。该旧模块及其 View 测试不应被误认为 Compose 页面实现；迁移审计和 Compose 构建只针对本模块执行。

## 同仓库依赖

    dependencies {
        implementation(project(":zero-symbol-input-view-v2-compose"))
    }

外部 Maven 消费者应使用 Compose 模块对应的已发布坐标。不要复用旧 View 模块的 artifact 坐标；发布前以当前分支的 Maven publication 配置和工作流校验结果为准。

## 符号抽屉入口

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
            onOpenManager = { /* 宿主导航 */ },
            onOpenSettings = { /* 宿主导航 */ }
        )
    }

SymbolDrawer 提供分组分页、符号网格、展开/折叠、拖动和动画。数据快照由宿主或数据层提供；Composable 不应执行阻塞 I/O。

## 通用 Drawer 入口

    @Composable
    fun EditorToolDrawer() {
        val state = rememberDrawerContainerState()
        DrawerContainer(
            state = state,
            title = "Editor tools",
            content = { EditorContent() }
        )
    }

实际集成时，主内容通过 content slot 提供，页面使用稳定 ID 的 DrawerPage，面板使用 drawerPanelContent。DrawerContainerState 由 rememberDrawerContainerState 创建，控制器和状态机共享单一状态源。

## 管理与设置

- SymbolDrawerManagerScreen：分组/符号 CRUD、排序、JSON 导入导出、分组复制、符号批量复制/移动/删除。
- SymbolDrawerSettingsScreen：基于不可变设置快照和回调更新设置。
- SymbolGroupManager：校验数据、执行单次持久化命令、维护不可变快照。
- SymbolGroupRepository / BundleCodec：负责持久化和导入导出，不依赖 Compose UI。

## 自底向上的迁移分层

1. 编辑器中立文本/动作 API 与事务契约。
2. 领域模型、校验、Repository、Codec 和旧数据迁移。
3. Drawer 锚点解析、状态机、交互控制器、拖动和嵌套滚动策略。
4. 导航、面板、自适应窗口、样式和扩展注册框架。
5. Compose 组件、页面、管理 UI、设置 UI 与可访问性。

底层 API 不应为了页面渲染而引入 Compose 类型；UI 层不应反向成为底层状态机的依赖。

## 生命周期约束

本模块不以 Activity / Fragment / ViewModel / LifecycleOwner 管理页面状态，也没有 View attach/detach 或 View factory 契约。Compose 的 Composition、重组和 effect 启动/取消语义仍然存在，因此“零生命周期”在本项目中的准确含义是“不依赖传统 Android View 页面生命周期”，而不是没有任何生命周期。

## 迁移和构建审计

Gradle verification 包含：
- Compose 源码 API 边界扫描。
- Compose AAR / class artifact 边界扫描。
- Debug / Release 生产运行时依赖图扫描。
- Compose 源码、测试源码和 instrumentation 源码迁移清单。
- Debug / Release 构建、单元测试与 instrumentation 测试编译。

源码扫描通过不等于整个仓库旧 View 模块已经删除。必须同时检查本 Compose 模块、文档、产物和 CI 结果。

## API 文档

- api-docs/01-quick-start.md：Compose 快速开始。
- api-docs/02-symbol-drawer-view.md：SymbolDrawer Compose API。
- api-docs/03-models-and-actions.md：模型与动作。
- api-docs/04-action-runtime.md：动作执行器和宿主契约。
- api-docs/05-data-and-management.md：数据与管理。
- api-docs/06-settings.md：设置 API。
- api-docs/07-universal-drawer.md：Compose 通用 Drawer。
- api-docs/08-navigation-panels-style.md：导航、面板与样式。
- api-docs/09-adaptive-window.md：自适应窗口。
- api-docs/10-api-index-compatibility.md：API 与兼容边界。
- api-docs/11-interaction-and-builtins.md：交互与内置动作。
- api-docs/12-extension-framework.md：扩展注册框架。
- api-docs/13-public-package-and-editor-neutral-actions.md：公共包与编辑器中立动作。
- api-docs/14-drawer-public-api-reference.md：Drawer 公共 API。
- api-docs/15-symbol-control-extension-api.md：符号扩展 API。
- api-docs/16-testing-release-troubleshooting.md：测试、发布与故障排查。
