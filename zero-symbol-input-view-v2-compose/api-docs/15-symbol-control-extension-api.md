# 15 — 符号抽屉 Compose 扩展实战

本页示例以 Compose 模块实际扩展接口为准。不要将旧 V2 View 模块的 Fragment、ViewPager2、RecyclerView 或 View renderer 接口复制到 Compose 集成代码。

## 1. 注册一个 Compose 分组指示器

```kotlin
val handle = registry.registerIndicator(object : SymbolGroupIndicatorExtension {
    override val id = "example.group-indicator"
    override val title = "Example indicator"

    @Composable
    override fun Content(state: SymbolGroupIndicatorState) {
        Row {
            state.groups.forEachIndexed { index, group ->
                TextButton(onClick = { state.onSelectGroup(index) }) {
                    Text(group.title)
                }
            }
        }
    }
})
```

由抽屉宿主决定何时显示该 slot。不要在扩展中创建第二个 pager 或保存独立 selectedIndex。

## 2. 注册单元格 Composable renderer

实现 `SymbolDrawerItemRenderer` 的 `accepts(entry)` 与 `Content(entry, state, onClick, onLongClick)`。渲染器只负责当前符号项的 Composable 表现；动作分发和分组数据仍归抽屉/领域层管理。

## 3. 注册设置贡献

`SymbolDrawerSettingsTabExtension` 增加完整设置 Tab；`SymbolDrawerSettingExtension` 通过 tabId 向现有 Tab 贡献内容。通过 `SymbolDrawerSettingsScope.update` 提交不可变设置变换，不要直接修改设置对象。

## 4. 注册生命周期与资源所有权

```kotlin
val registrations = listOf(
    registry.registerIndicator(myIndicator),
    registry.registerItemRenderer(myRenderer)
)
// owning editor/session ends:
registrations.forEach(AutoCloseable::close)
```

句柄应由创建它的业务会话持有并显式释放。不要绑定 Fragment viewLifecycleOwner，也不要在静态单例中持有 Activity、View 或编辑器实例。

## 5. 线程与回调

注册表会在锁外分发快照通知。handler 应短小、可预测；耗时工作由调用方调度。Composable Content 遵循 Compose 规则，不要在 composition 阶段执行持久化或不可逆副作用。

## 6. 验证清单

- 扩展 ID 稳定且唯一。
- 注册/替换/关闭句柄不会误删新版本。
- UI 只通过 Composable slot 呈现。
- 状态来源唯一，数据更新经过 Repository/领域层。
- 注册句柄在所属会话结束时关闭。
- 不引用 Android View、Activity、Fragment、ViewModel 或 LifecycleOwner。