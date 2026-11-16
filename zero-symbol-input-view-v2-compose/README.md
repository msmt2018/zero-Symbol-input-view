# Zero Symbol Input View V2 — Jetpack Compose

这是独立的原生 Jetpack Compose 版本。旧的 `zero-symbol-input-view-v2` Android View 模块保持不变；本模块不应把旧 View 控件包装成 Compose 控件。

## 当前权威开发文档

- [简体中文：Compose V2 API 开发与集成手册](api-docs/COMPOSE_API_REFERENCE.md)
- [English: Compose V2 API Development and Integration Guide](api-docs/en/COMPOSE_API_REFERENCE.md)

上述手册提供准确包名、完整 import、最小接入、编辑器动作 Host、扩展注册、Renderer、Indicator、Provider、Theme、Event、Settings extension、Manager/Settings Route、Repository 和迁移边界。所有示例以当前分支源码为准。

## 最小接入

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.rememberSymbolDrawerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

@Composable
fun EditorSymbolToolbar(
    groups: List<SymbolGroup>,
    onSymbolClick: (SymbolEntry) -> Unit,
    onSymbolLongClick: (SymbolEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberSymbolDrawerState()
    SymbolDrawer(
        groups = groups,
        state = state,
        modifier = modifier,
        onSymbolClick = onSymbolClick,
        onSymbolLongClick = onSymbolLongClick
    )
}
```

## 架构约束

- UI：Composable、Compose State、Modifier、Material 3、Foundation Pager/Grid。
- 生命周期：使用者不需要手动创建、启动、停止、销毁 Screen 或控件；Composition 负责 UI 工作和资源清理。
- 编辑器集成：通过 `SymbolActionHost` 或 `SymbolTextDocument` 适配宿主编辑器，不依赖特定编辑器内核。
- 数据：稳定 ID、不可变快照、V2 typed action、JSON bundle 与 legacy migration。
- 扩展：Composable Renderer/Indicator/Settings slots、typed events、immutable extension snapshot。
- 禁止：将旧 View 控件包装成 Compose、在 Compose API 暴露 View 生命周期回调。

## 迁移状态

本模块仍处于完整迁移阶段。当前 Compose 符号抽屉、状态、扩展、管理/设置 Route 已有原生实现；Generic Drawer facade 和部分历史 UI/平台适配仍需重写。不要将旧 View 的 `SymbolDrawerView`、`reloadGroups()`、`OnSymbolActionListener`、旧 Drawer Controller/Panel/Adapter API 当作 Compose API。具体边界见权威手册第 8 节。
