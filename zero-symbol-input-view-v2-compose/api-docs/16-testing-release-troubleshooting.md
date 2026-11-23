# 16 — Compose 测试、发布与故障排查

## 1. 测试层次

| 层级 | 重点 |
|---|---|
| 领域单元测试 | 模型校验、动作执行、编辑事务、Repository、Codec、迁移 |
| 核心状态测试 | DrawerStateHolder、锚点解析、拖动/settle/fling、嵌套滚动策略 |
| Compose UI 测试 | Composable 内容、语义节点、点击/长按、分页选择同步 |
| 构建边界检查 | Compose source、产物依赖和 runtime classpath 禁止传统 View/Lifecycle 栈 |

## 2. 重点用例

- 文档编辑事务在异常/取消时回滚，UTF-16 surrogate pair 边界不被拆分。
- 动作执行限制递归深度、数量和文本预算；取消和嵌套失败可观察。
- Drawer 状态在拖动、fling、锚点变化和过期动画下保持一致。
- 页面以稳定 ID 为 key；重排后不会把旧 index 当作新选择。
- 扩展注册、替换、注销及重入通知顺序正确。
- 大窗口/分屏尺寸变化重新计算自适应布局。
- Settings/Manager 对无效 JSON 或仓储失败给出错误，不破坏原快照。

## 3. Compose 状态诊断

- UI 只显示旧数据：检查 Repository 快照是否重新提交为 Compose state。
- 页面切换回弹：检查 selectedPageId 与 page ID 是否稳定，避免另建 index 真值。
- 扩展内容未刷新：检查注册表是否产生新 revision、观察句柄是否仍打开。
- 重组期间重复写入：把持久化/不可逆副作用移出 Composable body，使用明确事件回调或受控 effect。
- Back 未关闭抽屉：Back 策略由宿主负责，DrawerContainer 不自动安装 Activity BackHandler。

## 4. CI 发布门槛

- Compose debug/release 变体构建通过。
- Compose 单元测试通过。
- Compose instrumentation test 源码可编译并运行所需测试。
- 源码扫描不包含 AndroidView/AndroidViewBinding、传统 View、Activity/Fragment/ViewModel/LifecycleOwner 页面依赖。
- 运行时依赖边界审计通过；不得通过禁用任务或排除真实依赖来伪造通过。
- 旧 View 模块的兼容测试单独报告，不应与 Compose 迁移结果混为一谈。

## 5. 生命周期表述

Compose 并非字面上的“零生命周期”：Composition、recomposition、remember state 作用域和 effect cancellation 都存在。这里的目标是页面/UI 不依赖传统 Android View 树及 Activity/Fragment/LifecycleOwner 页面生命周期。