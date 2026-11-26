# 13 — 公共包路径与编辑器中立文档契约

本文按当前 V2 View 模块源码整理。通用 Drawer 源码已物理迁移到公共包；符号输入产品仍保留独立产品包。

## 1. 当前包结构

| API 区域 | 当前包 | 说明 |
|---|---|---|
| 通用 Drawer 容器 | `com.zerostudio.idea.widget.drawer` | View 版 `DrawerContainer` |
| 锚点、控制器、状态 | `com.zerostudio.idea.widget.drawer.core` | Drawer 运行状态和交互 |
| 页面导航 | `com.zerostudio.idea.widget.drawer.navigation` | 页面定义、导航控制和 View host |
| 面板 | `com.zerostudio.idea.widget.drawer.panel` | View 面板内容契约和 host |
| 配置 | `com.zerostudio.idea.widget.drawer.config` | 通用 Drawer 配置 |
| 样式 | `com.zerostudio.idea.widget.drawer.style` | 颜色、尺寸、字体、运动和形状 |
| 适配器 | `com.zerostudio.idea.widget.drawer.adapter` | 符号抽屉几何适配 |
| 编辑器中立动作 | `com.zerostudio.idea.widget.drawer.action` | 通用文本/动作 API |
| 符号输入产品 | `com.zerostudio.idea.widget.editor.symbolinput.v2` | `SymbolDrawerView` 等符号控件 |
| 符号模型 | `com.zerostudio.idea.widget.editor.symbolinput.v2.model` | `SymbolGroup`、`SymbolEntry`、typed actions |

旧包 `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` 已不再是 Drawer 源码的实现包。新代码不得继续导入该旧路径。

## 2. 推荐导入

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.core.DrawerController
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocument
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
```

`SymbolDrawerView` 仍属于 Android View 产品入口。公共 Drawer 包中的兼容 typealias 仅供 Kotlin 源码调用；typealias 不会生成新的 Java 类。需要 Java ABI 时，应使用实际声明类所在包，或由库提供真实 JVM facade。

## 3. 编辑器中立动作边界

V2 不要求宿主编辑器继承任何库类型。宿主针对当前焦点文档实现 `SymbolActionHost`；需要通用文本编辑操作时实现 `SymbolTextDocument`，再使用 `SymbolTextDocumentActionHost` 或自定义 adapter。

文档 API 的位置约定：

- Offset 使用 UTF-16 code units，与 JVM `String` 索引一致。
- 行号和列号从 0 开始。
- Range 使用左闭右开区间 `[start, end)`。
- `lineEndOffset` 不包含 CR/LF 行分隔符。
- 多范围编辑应校验非重叠，再从右向左应用，避免前面的替换改变后续 offset。

## 4. 动作执行约定

`SymbolActionExecutor` 是同步解释器，不会自动切换线程。编辑器要求主线程时，宿主必须在主线程调用。host 返回 false 表示当前能力不支持或拒绝；异常、取消、无效输入和 Composite 部分失败分别通过 `SymbolActionResult` 表达。

Composite 不是自动事务。只有宿主文档实现确实提供事务/回滚语义时，才能承诺原子性。

## 5. 模块边界

本模块是传统 Android View 实现：`SymbolDrawerView`、`DrawerContainer`、页面 host 和面板 host 均为 View API。不要将本文的 View 构造、Context、attach/detach 或 View 状态保存约定套用到 Compose 模块。Compose 模块拥有独立 artifact 和独立 Compose API。
