# 16 — Compose 测试、发布与故障排查

本篇只描述 Compose 模块的验证要求。旧 View 模块的 instrumentation 结果应单独标记，不能作为 Compose UI 测试结论。

## 1. 验证层级

| 层级 | 验证目标 | 方式 |
|---|---|---|
| 纯 Kotlin 单元测试 | 模型校验、codec、动作执行、文本编辑、锚点/几何策略、Repository/Manager | JUnit |
| Compose 单元/语义测试 | Composable 状态、交互、Pager、手势与无障碍语义 | Compose UI test |
| Android 构建 | Kotlin 编译、资源、Manifest、Debug/Release AAR | Gradle |
| 产物边界 | AAR 源码/API、运行时依赖与禁止依赖 | CI boundary audit |
| 宿主集成 | 编辑器 adapter、剪贴板、导航和窗口 Insets | 宿主应用测试 |

## 2. 当前 CI 关键检查

Compose V2 工作流应验证：

- Compose 源码清单审计。
- 传统 View、Activity/Fragment、Lifecycle API 源码边界。
- AAR 产物与 runtimeClasspath 禁止依赖边界。
- Compose Debug/Release 构建。
- Compose 单元测试。
- Compose Instrumentation 测试编译。
- Debug APK、旧 V2 Library 与 Maven publication validation 等仓库级任务。

旧 View 模块 Android 21/29/35 instrumentation 属于独立兼容任务；其失败不得被误报为 Compose 源码/测试失败，也不得通过修改旧模块测试掩盖问题。

## 3. 本地验证

在仓库根目录使用 Compose 模块对应 Gradle task：

- :zero-symbol-input-view-v2-compose:assembleDebug
- :zero-symbol-input-view-v2-compose:assembleRelease
- :zero-symbol-input-view-v2-compose:testDebugUnitTest
- :zero-symbol-input-view-v2-compose:compileDebugAndroidTestKotlin

任务名称以当前 Gradle 插件配置为准；不存在的 task 应先检查模块 tasks 列表，不应将 task 名称错误归类为 Kotlin 编译错误。

## 4. 核心单元测试矩阵

### 动作运行时
- typed action 合法/非法参数。
- 未支持 Host 能力返回 Unsupported。
- Host 返回 false 与抛异常结果区分。
- Composite 部分失败和取消语义。
- 扩展 handler 匹配后拒绝，不重复 fallback。
- 未注册扩展保留宿主 fallback。

### 文本文档
- UTF-16 左闭右开范围。
- surrogate pair、emoji、组合字符和 CRLF。
- 正向/反向选区、空选区和文档边界。
- 多编辑按右到左应用。
- 事务失败时恢复文档/选择状态。
- 只读文档拒绝写操作。

### 抽屉状态与几何
- 锚点 ID 唯一、像素高度非负。
- 多锚点排序、合并、clamp 和 fling 目标。
- 拖动小数像素累积。
- resize 时保持展开比例和动画 generation。
- 嵌套滚动 PRE/POST 仲裁不超额消费。
- 重入 listener 通知顺序稳定。
- listener 异常不阻断后续观察者。

## 5. Compose UI 测试矩阵

- 空分组与非空分组渲染。
- 稳定 key 下的分组/符号重排。
- 横向 Pager 与纵向抽屉拖动互不抢占。
- 展开/折叠、fling、取消手势和快速反向拖动。
- 设置修改、重置、导入/导出。
- 管理界面分组 CRUD、复制、批量复制/移动/删除。
- 扩展 renderer/indicator/settings 内容的注册、替换和注销。
- Insets、分屏、窗口尺寸变化和折叠特征输入。
- 无障碍语义、触控目标和减少动态效果设置。

## 6. 源码与依赖边界

Compose 模块生产源码、测试源码、发布产物和 runtimeClasspath 都必须审计。禁止通过以下方式伪造迁移完成：

- AndroidView / AndroidViewBinding 包装旧控件。
- 把 Activity、Fragment、LifecycleOwner、ViewModel 引入 Compose API。
- 通过 exclude、禁用 task 或缩小扫描范围掩盖传递依赖。
- 修改旧 View 模块测试来让 Compose 检查变绿。
- 只看 assemble 成功而跳过 source/artifact/runtime boundary。

若依赖边界失败，应查看最短传递依赖路径，优先移除错误架构依赖或调整 Compose API 依赖声明；不得仅删除检查规则。

## 7. 发布检查

- Debug/Release AAR 均成功构建。
- Maven coordinates、POM、consumer rules、Manifest 和资源正确。
- Kotlin typealias 的 Java ABI 限制明确记录。
- JSON schema、动作 ID、稳定 page ID 和公开方法变更有兼容测试。
- 发布 Tag 指向已通过对应 CI 的 commit。
- 不把非阻塞旧 View instrumentation 失败混写为 Compose 构建失败。

## 8. 常见问题

### Compose 页面空白
检查输入快照是否为空、selected ID 是否存在、Composable content slot 是否正确传递，以及 state 是否被错误地跨会话共享。

### Pager 选中状态错位
检查 DrawerPage.id 是否稳定且唯一；不要以列表 index 作为页面身份。检查程序化 selectedPageId 更新是否与用户翻页回调形成循环。

### 抽屉和列表滚动冲突
检查 NestedScrollConnection 的 PRE_SCROLL/POST_SCROLL 阶段、方向符号和子内容消费顺序；不要再添加传统 View 拦截逻辑。

### 扩展 UI 未更新
确认扩展注册表实例是同一会话实例，注册句柄未提前关闭，Composable 消费者已读取当前快照，并使用稳定 ID/key。

### Runtime boundary 失败
查看 Gradle dependencyInsight/CI shortest path 输出，定位 lifecycle/activity/savedstate 等传递依赖来自哪个 Compose artifact；不要以排除依赖掩盖不兼容 API。

## 9. 缺陷报告

请提供 commit SHA、分支、CI run/job、Gradle task、Android 版本、窗口模式、最小复现步骤、预期/实际结果及完整异常堆栈。不要提供 token、密码或用户私有数据。
