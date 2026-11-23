# 15 — Compose 符号控件扩展 API 实战参考

本指南只针对 zero-symbol-input-view-v2-compose。扩展点均为 Compose-native API，不支持创建或绑定传统 Android View。

## 1. 获取扩展注册表

SymbolDrawer 是 Composable 函数，不会自动创建或暴露扩展注册表。宿主应显式创建 SymbolDrawerExtensionRegistry，并由编辑器 session 或 Composition owner 管理。注册表保存扩展描述、handler 和 Composable 内容契约；不要把编辑器 View、Activity 或 Context 放入扩展对象。

当前 SymbolDrawer、SymbolDrawerSettingsScreen 和 SymbolDrawerManagerScreen 尚未自动消费所有注册表贡献。注册只代表贡献已进入 registry snapshot；宿主/具体 Compose 容器仍需显式读取并呈现相应贡献，不能假设注册后自动显示。注册方法返回 AutoCloseable，宿主应由明确的 session owner 或 Composition DisposableEffect 持有并关闭句柄。

## 2. 注册动作

使用 SymbolDrawerActionExtension 声明稳定 id、title、description、payloadHint、priority 和 handler。符号动作保存 SymbolAction.Extension(id, payload)，不保存 lambda。

handler 通过 SymbolActionHost 执行编辑器中立命令。宿主应检查当前文档/编辑器能力，返回 true 表示完成，false 表示不支持或拒绝。

## 3. 自定义单元格

实现 SymbolDrawerItemRenderer：

- accepts(entry) 决定是否接受条目。
- Content(entry, state, onClick, onLongClick) 提供 Composable 内容。
- state 是当前条目的不可变显示快照。
- 点击/长按回调交回控件统一分发。

renderer 不负责网格尺寸、列表 key、分页、动作执行、焦点编辑器或持久化。内容应保持可重组、可重复调用，不要在 Composable 函数体内执行一次性注册或 I/O。

## 4. 自定义分组指示器

实现 SymbolGroupIndicatorExtension.Content(state)。state 提供 groups、selectedIndex 和 onSelectGroup(index)。

指示器只负责呈现和发出选择请求。不要创建另一个 HorizontalPager，也不要维护与主导航不同步的 selectedIndex。

## 5. 自定义设置 Tab 与设置项

SymbolDrawerSettingsTabExtension.Content(scope) 创建完整设置页；SymbolDrawerSettingExtension.Content(scope) 创建插入指定 tabId 的设置项。

scope.current 提供当前设置快照，scope.update { old -> old.copy(...) } 提交变更。扩展专属数据应保存在宿主自己的存储中，不应随意扩展 V2 固定 JSON schema。

## 6. 自定义主题

注册 SymbolDrawerThemeExtension 并返回语义颜色 token。主题扩展只影响实际调用其 resolver 的 Compose 消费者。应核对当前页面的 token 覆盖情况，不要假设它自动覆盖所有 Material 3 组件。

## 7. 旧数据 provider

SymbolDrawerLegacyGroupProvider.loadGroups() 可提供旧版分组模型。使用 LegacySymbolDataCodec、LegacySymbolActionAdapter 和 SymbolGroupRepository 的迁移管线处理；provider 不应变成第二个长期 repository。

## 8. 验证清单

- 稳定 ID 合法且跨重组保持不变。
- 重复注册后旧句柄关闭不会删除新注册。
- 注册/注销不会导致重复动作执行。
- Composable renderer 在条目重排后仍绑定正确 item ID。
- 指示器只发出选择请求，不拥有 Pager 状态。
- 设置扩展通过 scope.update 提交完整快照。
- 扩展异常不会破坏内置 UI。
- 所有句柄在所属 session/Composition 结束时释放。

本模块没有 ViewHolder 回收回调、View factory、onViewAttachedToWindow 或 Fragment View 生命周期 API。