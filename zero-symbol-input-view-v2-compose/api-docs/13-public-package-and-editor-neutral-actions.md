# 13 — 公共包路径与编辑器中立动作 API

## 1. 分层原则

Compose 模块将编辑器中立文本/动作合同与 Compose UI 分离。编辑器适配层实现文本读取、光标/选区和动作执行能力；领域层只依赖这些合同，不接收 Activity、Fragment、View 或 LifecycleOwner。

## 2. UTF-16 文档坐标

Kotlin/Java 字符串索引以 UTF-16 code unit 为单位，因此文档编辑偏移使用 UTF-16 offset，范围采用 end-exclusive 语义。宿主与库必须统一该坐标体系。

编辑事务应先验证范围、重叠和同偏移冲突，再按从右向左的顺序应用多个编辑，避免前序插入改变后续 offset。涉及 surrogate pair 的操作必须验证边界，不能把一个 Unicode code point 的代理对拆开。

## 3. 动作执行边界

动作 ID、handler、执行上下文和文本编辑 host 都是 Kotlin API。动作 payload 用于可持久化参数，不应序列化 Activity、View、编辑器实例或回调对象。执行器对递归深度、动作数量和文本预算设限；宿主应处理取消、失败和执行结果。

## 4. 注册表观察

`SymbolDrawerExtensionRegistry.observe(emitCurrent, listener)` 返回 `AutoCloseable`。`snapshot()` 返回包含扩展贡献与 revision 的快照，`revision()` 单调递增。观察者句柄由订阅方显式关闭，不绑定 Android View attach/detach。

## 5. Compose 扩展消费者边界

注册表数据本身不会自动将每一种扩展注入任意 UI。当前 Compose 扩展类型通过明确的 Composable Content 合同提供指示器、单元格、设置 Tab 和设置项；消费者必须实际渲染对应 slot。不要引用旧版 `SymbolDrawerView`、View factory 或 ViewPager2 方法。

## 6. Kotlin 与 Java 兼容性

Kotlin `typealias` 仅提供 Kotlin 源码级别别名，不会生成 Java 可见的新 JVM class。若需要 Java ABI 稳定性，必须提供真实 public facade class/interface 并增加 Java 编译测试，不能把 Kotlin import 成功当作 Java 兼容证明。

## 7. 旧版兼容

旧版 View 模块保留其原有包路径和 API。Compose 模块的公共 API 应以当前 Compose 源码为准；不要将旧模块的 Context 构造、Fragment 生命周期或 View 回调作为 Compose 接入步骤。