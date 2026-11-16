# Zero Symbol Input View V2 — API 文档中心

> 面向 Android/Kotlin 集成者与框架扩展开发者。文档以当前分支源码为准；示例中标注的实现边界、线程和失败语义同样属于 API 契约。

## 语言

- [English API documentation](en/README.md)
- [简体中文 API documentation](README.md)

- [English API documentation](en/README.md)
- [简体中文 API documentation](../README.md)

## 文档地图

| 文档 | 主题 | 适用场景 |
|---|---|---|
| [01 — 快速开始与集成](01-quick-start.md) | Gradle、初始化、生命周期、最小接入 | 第一次集成 |
| [02 — 符号抽屉视图 API](02-symbol-drawer-view.md) | SymbolDrawerView 构造、回调、数据、分页、展开状态 | 使用符号控件 |
| [03 — 数据模型与动作类型](03-models-and-actions.md) | 分组、符号、动作、配置模型 | 构造和交换数据 |
| [04 — 动作注册与执行 API](04-action-runtime.md) | Host、Executor、Registry、结果、取消和 JSON | 接入编辑器动作 |
| [05 — 仓储、管理器与迁移](05-data-and-management.md) | Repository、Manager、导入导出、V1 迁移 | 数据管理 |
| [06 — 设置中心 API](06-settings.md) | 设置模型、持久化和运行时应用 | 定制设置 |
| [07 — 通用 Drawer 框架](07-universal-drawer.md) | DrawerContainer、Controller、Anchor、手势和状态 | 开发通用抽屉 |
| [08 — 页面、面板与样式](08-navigation-panels-style.md) | 页面导航、面板生命周期、主题、样式和预设 | 自定义抽屉内容 |
| [09 — 自适应窗口 API](09-adaptive-window.md) | WindowAdaptiveInfo、布局解析、网格和侧边停靠 | 平板、分屏、折叠屏 |
| [10 — 类型索引与兼容性](10-api-index-compatibility.md) | 类型总表、边界条件、兼容策略和排错 | 查找 API |
| [11 — 手势与内置文本动作](11-interaction-and-builtins.md) | 手势仲裁、内置命令、模板 token 和限制 | 扩展文本编辑动作 |
| [12 — 扩展注册框架](12-extension-framework.md) | 动作、单元格、指示器、设置、主题、生命周期扩展 | 扩展现有符号控件 |
| [13 — 公共包路径与编辑器中立文档契约](13-public-package-and-editor-neutral-actions.md) | 新公共命名空间、UTF-16 文档契约、事务编辑 | 新包迁移与编辑器适配 |
| [14 — 通用 Drawer 公共 API 参考](14-drawer-public-api-reference.md) | 新公共包、配置、锚点、控制器、导航、面板、样式 | 开发/复用抽屉框架 |
| [15 — 符号控件扩展 API 实战参考](15-symbol-control-extension-api.md) | 真实控件扩展点、调用链、代码模板、生命周期与边界 | 开发符号控件插件 |
| [16 — 测试、发布与故障排查](16-testing-release-troubleshooting.md) | 测试矩阵、发布检查、Java/Kotlin 兼容、常见问题 | 维护和发布 |

## 模块与包名

模块：`zero-symbol-input-view-v2`

Android namespace 和符号输入产品 API 仍位于：

```kotlin
com.zerostudio.idea.widget.editor.symbolinput.v2
```

通用 Drawer 的新公共 Kotlin 入口位于：

```kotlin
com.zerostudio.idea.widget.drawer
com.zerostudio.idea.widget.drawer.core
com.zerostudio.idea.widget.drawer.navigation
com.zerostudio.idea.widget.drawer.panel
com.zerostudio.idea.widget.drawer.style
com.zerostudio.idea.widget.drawer.adapter
com.zerostudio.idea.widget.drawer.extension
com.zerostudio.idea.widget.drawer.action
```

**重要：** 当前公共包中的部分类型通过 Kotlin `typealias` 暴露。Kotlin 源码可直接导入；typealias 不会生成独立 JVM/Java 类，Java 项目不能把它当成 Java 可见类导入。内部实现仍位于历史 V2 包路径，文档不会把过渡 facade 描述成已完成物理迁移。

## 依赖

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

JitPack 远程依赖请使用已经成功发布的 Git Tag：

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.msmt2018.zero-Symbol-input-view:zero-symbol-input-view-v2:<published-tag>")
}
```

不要把尚未发布的分支名当成 Maven 版本。

## 按任务选择 API

| 目标 | 推荐入口 |
|---|---|
| 显示原有符号工具栏 | `SymbolDrawerView` |
| 读取/提交符号组 | `SymbolGroupRepository`、`submitGroups` |
| 接入任意编辑器 | `SymbolActionHost`；需要通用文本操作时实现 `SymbolTextDocument` |
| 注册自定义动作 | `drawer.extensions.registerAction` 或 `SymbolActionRegistry` |
| 替换符号格子渲染 | `registerItemView` |
| 替换真实分组指示器 | `registerIndicator` + `setCustomGroupIndicator` |
| 扩展管理页设置页 | `registerSettingsTab`、`registerSetting` |
| 扩展主题 | `registerTheme` + `setThemeExtension` |
| 构建功能中立抽屉 | `DrawerContainer` + `DrawerConfig` |
| 自定义锚点和交互 | `DrawerAnchorStrategy`、`DrawerController` |
| 自定义页面/面板 | `DrawerPageHost`、`DrawerPanelContent` |
| 大屏适配 | `WindowAdaptiveInfo`、`AdaptiveLayoutResolver` |

## 推荐阅读路径

- **只想集成控件：** 01 → 02 → 04 → 06。
- **开发编辑器适配器：** 03 → 04 → 11 → 13 → 16。
- **开发符号控件扩展：** 12 → 15 → 02 → 16。
- **复用通用抽屉：** 07 → 08 → 14 → 09 → 16。
- **维护/发布库：** 10 → 12 → 13 → 16。

## 全局 API 约定

1. **稳定 ID：** 持久化数据和跨进程引用使用稳定字符串/数字 ID，不使用列表 index 作为身份。
2. **快照：** API 接收/返回的集合按快照使用；提交后不要原地修改调用方集合。
3. **线程：** Android View、View factory、页面生命周期和编辑器对象应在宿主要求的线程操作。动作接口是同步接口，不会自动切换主线程。
4. **失败：** 区分无效输入、能力不支持、执行异常、取消和部分完成；不要把回调触发当成编辑成功。
5. **生命周期：** 注册返回的 `AutoCloseable`、状态监听器和 View 回调必须随 Fragment View 或编辑器 Session 释放。
6. **职责边界：** 符号控件、通用 Drawer、宿主编辑器和持久化仓储各自保留唯一状态所有者；不要另建平行分页器、动作引擎或数据源。
7. **验证：** 纯 Kotlin 解析器可单元测试；窗口 Insets、嵌套滚动、触摸和可访问性仍需在真实宿主布局及目标设备验证。

## 当前实现状态说明

文档区分“类型/方法已存在”“已接入原控件消费者”和“仍需宿主实现”三种情况。注册一个扩展并不代表所有 UI 自动消费它；请查看第 12、14、15 篇中的消费路径和限制。编辑器适配器、剪贴板、管理器导航以及宿主特定命令始终由应用实现。

## 许可

模块源文件声明：`AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0`。`AND` 表示许可义务累积适用，不是任选其一。集成、修改和再分发前请阅读完整许可文件及第三方依赖声明。
