# 12 — Compose 扩展注册框架 API

本页只描述 Compose 模块当前真实存在的扩展契约。扩展 UI 使用 Composable，不提供 View factory、Context 参数、RecyclerView ViewHolder 或 View attach/detach 回调。

## 1. SymbolDrawerExtensionRegistry

注册表是线程安全的运行时贡献容器，按稳定 ID 保存动作、分组指示器、单元格 renderer、旧数据 provider、设置 Tab、设置项和主题扩展。

注册方法：

- registerAction(extension)
- registerLegacyAction(actionId, handler)
- registerIndicator(extension)
- registerItemRenderer(extension)
- registerGroupProvider(extension)
- registerSettingsTab(extension)
- registerSetting(extension)
- registerTheme(extension)

每个注册方法返回 AutoCloseable。相同 ID 的新注册替换旧贡献；旧句柄关闭时只允许移除自己仍拥有的注册，不能误删后续替代项。

查询方法 actions()、indicators()、itemRenderers()、groupProviders()、settingsTabs()、settingsItems() 和 themes() 返回按 priority、id 排序的快照。

snapshot() 返回 revision、各类贡献列表和 builtInVisibility 快照。setBuiltInVisible / isBuiltInVisible / builtInVisibilitySnapshot 管理内置功能可见性。clear() 清空当前注册表的所有贡献。

## 2. 动作扩展

SymbolDrawerActionExtension 包含 id、title、description、payloadHint、priority 和 SymbolActionHandler<SymbolActionHost>。

扩展动作应以 SymbolAction.Extension(extensionId, payload) 持久化，不序列化 lambda。registerLegacyAction 只用于兼容旧数字动作 ID。

wrapActionHost(delegate) 返回 RegistryBackedSymbolActionHost：

1. 先检查对应注册 handler。
2. 找到 handler 且返回 false 时，视为该 handler 已匹配但拒绝执行，不重复调用 fallback。
3. 未找到 handler 时才调用 delegate 的 executeExtension。
4. 动作执行异常由 SymbolActionExecutor 转换为结构化失败结果。

## 3. Composable 单元格 renderer

SymbolDrawerItemRenderer 提供 id、title、description、priority、accepts(entry) 和 Composable Content(entry, state, onClick, onLongClick)。

SymbolDrawerItemRenderState 包含 groupIndex、position、selected、pressed、enabled 和可选 theme。

renderer 只替换单元格内容，不拥有网格尺寸、分组数据、分页、手势仲裁或动作执行。未接受条目时使用后续 renderer/默认内容。UI 消费者必须遵守 Compose key 和稳定 item ID。

## 4. Composable 分组指示器

SymbolGroupIndicatorExtension 提供 id、title、description、priority 和 Composable Content(state)。

SymbolGroupIndicatorState 包含 groups 快照、selectedIndex 和 onSelectGroup(index)。扩展只呈现分组选择 UI；最终页面选择仍由抽屉导航状态所有者处理。不得在扩展内部创建第二份 Pager 或独立持久化选择真值。

## 5. 设置扩展

SymbolDrawerSettingsTabExtension 提供 id、title、priority 和 Composable Content(scope)。

SymbolDrawerSettingExtension 提供 id、tabId、title、priority 和 Composable Content(scope)。

SymbolDrawerSettingsScope 暴露 current 设置快照和 update(transform)。扩展应通过该作用域提交设置变更。扩展自己的私有字段应由宿主单独持久化，不要未经 schema 设计写入 V2 固定设置 JSON。

## 6. Legacy group provider

SymbolDrawerLegacyGroupProvider 提供 id、title、priority 和 suspend loadGroups()，返回 legacy SymbolGroup 列表。旧数据必须通过既有 codec/migration/repository 管线转换，不应建立平行的长期数据源。

## 7. 主题扩展

SymbolDrawerTheme 与 SymbolDrawerThemeExtension 位于公共主题扩展包。主题解析应保持确定性，不应修改 Window、写入持久化数据或持有 Activity/Context。实际消费者覆盖范围以当前 Compose UI 的接入点为准；不要把主题扩展描述为自动接管所有页面的全部 Material 3 token。

## 8. 线程与错误

注册表的注册、注销和观察通知是同步操作；不会自动切换到主线程。Composable UI 状态更新应由 Compose UI 作用域完成。handler 不应执行阻塞 I/O。

注册表会隔离 observer 回调异常；动作 handler 的异常由动作执行链处理。不要吞掉需要向宿主报告的业务失败。

## 9. 生命周期与持有规则

注册句柄由明确的 session owner 持有并关闭。若扩展只服务于某个 Composition，可在宿主使用 DisposableEffect 管理句柄；若扩展属于更长的编辑器 session，则由 session owner 管理。不要在静态单例中持有编辑器、Context 或页面状态。

Compose 有 Composition 和 effect 生命周期；这里不使用 Fragment View lifecycle 或传统 View attach/detach 回调。