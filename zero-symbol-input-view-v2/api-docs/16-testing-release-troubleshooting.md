# 16 — 测试、发布、兼容性与故障排查

本篇面向库维护者和宿主集成者，说明如何验证 V2 控件、扩展注册表、通用 Drawer 和编辑器动作适配器。测试结论必须区分 JVM 单元测试、Android 构建、设备交互测试和真实宿主集成测试。

## 1. 验证层级

| 层级 | 验证目标 | 推荐方式 |
|---|---|---|
| 纯 Kotlin 单元测试 | JSON codec、动作解析、锚点算法、数据规范化、ID 校验 | JUnit |
| Android 本地构建 | Kotlin/Java 编译、资源合并、Manifest、AAR 产物 | Gradle assemble |
| Android View 测试 | Adapter 绑定、布局测量、状态更新、回收 | Robolectric 或 instrumentation |
| 设备交互测试 | 触摸方向仲裁、NestedScrolling、fling、动画、Insets | 真机/模拟器 |
| 宿主集成测试 | 当前焦点编辑器、撤销栈、剪贴板、Fragment 生命周期 | 宿主应用测试 |

纯 JVM 测试通过不能证明真实 Android View 的手势、窗口 Insets 或无障碍行为正确。

## 2. 常用 Gradle 验证命令

在仓库根目录运行：

```bash
# 编译 V2 主代码和资源
./gradlew :zero-symbol-input-view-v2:compileDebugKotlin

# 构建 Debug / Release AAR
./gradlew :zero-symbol-input-view-v2:assembleDebug
./gradlew :zero-symbol-input-view-v2:assembleRelease

# 执行 V2 单元测试
./gradlew :zero-symbol-input-view-v2:test

# 生成测试报告（任务完成后）
./gradlew :zero-symbol-input-view-v2:testDebugUnitTest

# 构建宿主 APK
./gradlew :app:assembleDebug

# 连接设备后运行 Android instrumentation tests
./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest
```

具体可用 task 以当前 Gradle 插件和模块配置为准；如果某任务不存在，应先查看 `./gradlew :zero-symbol-input-view-v2:tasks --all`，不要把命令不存在误判为源码编译失败。

## 3. 必须覆盖的核心测试矩阵

### 3.1 动作运行时

- 每个 typed action 的合法、非法 payload。
- 不支持的 Host 能力返回 Unsupported，而不是 Executed。
- Host 返回 false 与抛出异常的结果区分。
- Composite 在第 N 个子动作失败时，返回已完成子动作信息。
- Cancellation 在执行前、执行中可观察阶段的行为。
- Extension handler 匹配后返回 false 不得再次调用 fallback。
- 未注册 Extension handler 时必须保留宿主 fallback。
- 同一动作不能因短按回调与扩展回调重复执行。

### 3.2 文本文档适配器

- UTF-16 offset 与 Kotlin String 索引一致。
- Range 使用左闭右开语义。
- surrogate pair、emoji、组合字符和 CRLF 行尾。
- 选区方向正向/反向、空选区和文档边界。
- 多范围编辑按从右向左应用，避免前序替换改变后续 offset。
- transaction hook 不可用时的明确降级。
- 不可编辑文档必须拒绝写操作。

### 3.3 原符号控件

- 折叠状态固定显示行数由设置决定，不得写死行数。
- 上下拖动只控制抽屉高度；横向滑动只切换分组页。
- 展开/折叠锚点、fling、快速反向拖动和 ACTION_CANCEL。
- ViewPager2 页面与分组指示器选中状态双向同步。
- 自定义 indicator 替换失败时保留内置指示器。
- 自定义 renderer create/bind 失败时回退原生 cell。
- 列数、显示行数、字体和窗口宽度变化后重新测量。
- 旋转、Fragment onDestroyView、重新 attach 后无重复监听器。

### 3.4 通用 Drawer

- Anchor ID 唯一、heightPx 非负、fraction 范围有效。
- 内容尺寸变化后 refreshAnchors 能更新可用高度。
- 嵌套滚动子 View 可滚动时，父抽屉不抢夺其纵向滚动。
- 横向子控件不会被纵向抽屉错误拦截。
- Back、Scrim tap、Drag handle 开关符合配置。
- PageHost 缓存、重建 Context、detach 和 dispose 不泄漏 View。

## 4. 扩展 API 生命周期检查表

每个扩展至少验证：

1. 注册后出现在预期的 snapshot/消费者中。
2. priority 和 ID 排序稳定。
3. 重复 ID 注册后旧 handle.close 不会删除新实例。
4. close 后 UI 消费者能及时移除旧贡献。
5. 扩展异常被隔离并报告，不破坏原控件主流程。
6. Fragment View 销毁时关闭 observer 和 registration handles。
7. Activity 重建后不会持有旧 Context/View。
8. 进程重建后由宿主重新注册 runtime callback。
9. 扩展未消费的数据不会被静默写入固定 V2 schema。
10. 内置 fallback 和宿主原有动作执行路径仍可用。

## 5. 发布前 API 检查

- 对外类型具有稳定、唯一的包路径和命名。
- 不把 internal 实现类误列为公共 API。
- 对 Kotlin `typealias` 明确标注 Java ABI 限制。
- 对公开接口新增抽象成员时考虑二进制兼容性；优先提供有默认实现的扩展点。
- 不随意修改稳定 ID、JSON 字段、动作 ID 和默认行为。
- 不删除旧构造函数/方法，除非经过版本化迁移。
- AAR 不应意外打包宿主编辑器内核依赖。
- POM、consumer rules、Manifest 和资源均应随 AAR 验证。
- 发布 Tag 必须指向已通过对应 CI 的 commit。
- JitPack 发布验证与普通 Android CI 是不同检查，不能互相替代。

## 6. Kotlin facade 与 Java 消费者

当前部分 `com.zerostudio.idea.widget.drawer.*` 公共类型是 Kotlin `typealias`，适合 Kotlin 消费者的源码导入。typealias 在 JVM 字节码层不会创建新类，因此：

- Kotlin 源码可从新 facade 包导入别名类型。
- Java 源码必须使用真正生成的 JVM 类所在包，或等待公共 API 物理迁移。
- Java 反射、ProGuard/R8 keep rule、JNI 类名不能依赖 typealias 名称。
- 对外发布 Java SDK 前，应将需要 Java 访问的类型物理声明在目标包，并增加 Java compile fixture 验证。

不要把“包路径已经有 facade 文件”写成“Java 公共 API 迁移完成”。

## 7. 常见问题

### 控件显示空白

- 确认调用了 `reloadGroups()` 或 `submitGroups(...)`。
- 检查 repository 是否返回空组。
- 检查 View 是否已经完成布局、父容器高度是否为 0。
- 检查分组是否包含合法符号项。
- 不要在每次 Fragment 重建时重复创建另一个 View 并保留旧实例。

### 符号点击没有修改编辑器

- 确认设置了 `onSymbolActionListener`。
- 确认根据 isLongPress 选择正确的 shortAction/longAction。
- 确认执行时传入的是当前焦点文档的 Host，而不是上一个编辑器 tab 的 Host。
- 确认动作 ID/payload 在当前 Host 能力范围内。
- 记录并区分 Executed、Unsupported、Invalid、Failed、Cancelled、PartialFailure。

### 自定义动作未出现在管理页

- 管理页面必须传入同一个 `drawer.extensions` 实例。
- 确认 registration handle 未提前 close。
- 确认扩展 ID 通过校验且没有被另一个同 ID 注册替换。
- 确认 manager view attach 后已观察 registry 更新。
- 保存的是 `SymbolAction.Extension(id, payload)`，不是 handler lambda。

### 自定义分组指示器未显示

- 确认调用 `registerIndicator` 后，再使用正确 ID 调用 `setCustomGroupIndicator`。
- 确认 provider.create 返回的组件 view 未被其它父容器占用。
- 确认组件能处理初始 groups/settings/theme。
- 检查扩展错误回调；创建或同步失败时控件会回退内置指示器。
- 组件内部不要创建自己的 ViewPager 或第二份 selectedIndex 真值。

### 设置扩展不显示

- SettingsScreen 必须接收相同 registry。
- `tabId` 必须与目标内置 Tab ID 一致；自定义 Tab 使用自己的稳定 ID。
- 确认扩展 View factory 不抛异常且返回未挂载 View。
- 设置项只在目标 tab 页面渲染时创建，不应假设注册时立即创建 View。

### 抽屉拖动和列表滚动冲突

- 不要在父布局对所有 MOVE 无条件调用 requestDisallowInterceptTouchEvent(true)。
- 子列表仍可向上滚动时，应先让子列表消费滚动。
- 横向分页与纵向抽屉必须分轴仲裁。
- 检查触摸取消、fling 和 settle 目标，不要自行叠加第二个 GestureDetector。

### 导入 JSON 后界面没有刷新

- 确认导入走原有 Manager/Repository 提交路径，而不是只更新临时 List。
- 导入解析成功后触发 repository 数据提交/观察者更新。
- 避免修改原始可变集合后期待快照自动更新。
- 检查当前显示页面是否观察的是同一个 repository/session。

## 8. 问题报告应包含

提交 bug 时提供：

- commit SHA、分支和 V2 模块版本。
- Android 版本、设备屏幕尺寸和窗口模式。
- 最小复现步骤及预期/实际结果。
- 相关动作 ID、payload 和结构化结果类型（脱敏后）。
- 完整异常堆栈与对应 CI job。
- 是否只在特定编辑器 Host、Fragment 重建或横竖屏切换后出现。

不要附带 API token、私有源码或用户文档内容。
