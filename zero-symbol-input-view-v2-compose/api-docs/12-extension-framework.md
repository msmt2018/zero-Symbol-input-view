# 12 — Compose 扩展注册框架

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.extension`

当前 Compose 扩展体系提供动作、分组指示器、符号单元格渲染、旧数据分组 provider、设置 Tab/设置项、主题和内置功能可见性贡献。所有 UI 扩展内容均为 Composable slot；不支持旧版 View factory / RecyclerView ViewHolder 回调。

## 1. SymbolDrawerExtensionRegistry

每个抽屉会话可持有独立注册表。注册表提供线程安全的注册、替换、快照和监听通知；`snapshot()` 返回 `SymbolDrawerExtensionSnapshot`，包含 revision、各扩展列表和 built-in visibility map。

注册方法：
- `registerAction(extension)`
- `registerIndicator(extension)`
- `registerItemRenderer(extension)`
- `registerGroupProvider(extension)`
- `registerSettingsTab(extension)`
- `registerSetting(extension)`
- `registerTheme(extension)`
- `registerLegacyAction(actionId, handler)`

每个注册方法返回 `AutoCloseable`。同 ID 注册会替换旧贡献；旧句柄不会误删后续替换项。调用方应在其业务会话结束时关闭句柄；这属于显式资源所有权，不是 Fragment/View 生命周期绑定。

## 2. 动作扩展

`SymbolDrawerActionExtension` 包含 id、title、description、payloadHint、priority 和 handler。ID 必须符合扩展 ID 规则。handler 通过 `SymbolActionHost` 执行，不应把 UI 引用或 Activity 存入持久化动作 payload。

## 3. Composable UI 扩展

- `SymbolGroupIndicatorExtension.Content(state)`：自定义分组指示器，state 提供 groups、selectedIndex 和 onSelectGroup。
- `SymbolDrawerItemRenderer.Content(entry, state, onClick, onLongClick)`：自定义单个符号项的 Compose 内容。
- `SymbolDrawerSettingsTabExtension.Content(scope)`：增加设置 Tab。
- `SymbolDrawerSettingExtension.Content(scope)`：向指定 tabId 增加设置行/卡片。
- `SymbolDrawerSettingsScope`：通过 current 读取当前设置快照，通过 update 提交不可变变换。

扩展 UI 不应建立第二份分组选择真值、第二套分页状态或独立的持久化数据源。

## 4. 数据与主题扩展

`SymbolDrawerLegacyGroupProvider.loadGroups()` 是 suspend 数据提供接口，返回 legacy SymbolGroup 数据；其结果仍需经过模型校验和数据层适配。主题扩展通过 `SymbolDrawerThemeExtension` 提供主题 token；不要在主题解析中修改 Window 或持有 Activity。

## 5. 观察与错误

注册表提供 revision 和 snapshot。监听句柄应显式关闭；观察者异常不会中断后续观察者通知。扩展错误可通过 `onExtensionErrorListener` 收集。调用 `clear()` 会清除所有注册和动作 handler；仅在该注册表确实结束使用时调用。

## 6. 不支持的旧扩展合同

以下旧 View API 不属于 Compose 扩展：
- `createView` / `bindView` / View factory
- RecyclerView ViewHolder attach/detach/recycle callbacks
- MotionEvent / ACTION_CANCEL 观察回调
- ViewPager2 适配器扩展

请使用 Composable Content、状态快照和普通 Kotlin handler。