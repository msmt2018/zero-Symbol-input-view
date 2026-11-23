# 17 — Compose 源码审计记录与已知边界

本页记录 Compose 模块的架构边界与审计方法。它不是设备运行测试报告，也不把旧 View 模块的兼容性结果当作 Compose 迁移结果。

## 审计范围

目标模块：`zero-symbol-input-view-v2-compose/`

按依赖方向审计：
1. 编辑器中立文本/动作合同。
2. 领域模型、验证与动作执行。
3. Repository、Codec、迁移和持久化。
4. Drawer 锚点、几何、状态和交互策略。
5. 导航、面板、自适应布局和扩展注册。
6. Compose screens 与符号抽屉 UI。

## 关键边界

- 领域/数据/核心状态 API 不应为了 UI 被全部标记为 `@Composable`。
- Compose 页面不使用 Android View 树作为 UI 实现，不通过 `AndroidView` / `AndroidViewBinding` 包装旧控件。
- Compose 页面状态不依赖 Activity、Fragment、ViewModel 或 LifecycleOwner。
- Compose 自身仍具有 Composition、recomposition、remember state scope 和 effect cancellation 语义。
- Kotlin `typealias` 不生成 Java ABI facade class。
- SharedPreferences Repository 可以使用 applicationContext 做持久化；这不等于页面依赖 Activity 生命周期。

## 验证方式

- 源码扫描：检查生产源码和测试源码中的传统 View、Activity/Fragment 页面状态及 AndroidView 包装。
- 依赖审计：检查 Compose artifact 的 runtime classpath，不仅检查直接依赖。
- 构建验证：Compose Debug/Release、单元测试和 instrumentation test 编译。
- 行为验证：Drawer 状态、编辑事务、数据迁移、页面稳定 ID、扩展注册顺序和错误路径。

静态扫描不能代替运行时测试；编译成功也不能单独证明所有交互行为正确。

## 兼容模块

`zero-symbol-input-view-v2/` 是独立的旧 View 模块。其 Android 设备测试失败必须单独报告，不应与 Compose 模块的源码边界和构建结果混为一谈。