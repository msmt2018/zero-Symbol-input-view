# Compose V2 API 文档中心

本目录只把当前原生 Compose API 作为 Compose 模块的集成契约。

## 权威文档

- [简体中文：Compose V2 API 开发与集成手册](COMPOSE_API_REFERENCE.md)
- [English: Compose V2 API Development and Integration Guide](en/COMPOSE_API_REFERENCE.md)

权威手册覆盖：模块依赖、完整包名和 import、SymbolDrawer 接入、SymbolActionHost、SymbolTextDocument、动作执行结果、8 类扩展注册、Compose Renderer/Indicator、动态 Provider、Theme/Event/Settings 扩展、管理页、设置页、Repository、JSON 互通、零生命周期约定和迁移边界。

## 重要说明：历史编号文档

本目录原有的 `01-quick-start.md` 至 `16-testing-release-troubleshooting.md`（以及英文对应页）来自旧 Android View 版本的文档基线。它们包含 `SymbolDrawerView`、View Listener、View Adapter、Activity/LifecycleOwner 等历史 API 示例。

这些旧页面**不是** `zero-symbol-input-view-v2-compose` 的 Compose 接入契约。新项目、Compose 控件接入和扩展开发必须使用上面的权威手册。旧页面仅供维护历史 View API 时参考，不能将其示例复制到 Compose 模块。

## 文档同步规则

每次公开 API 变更必须同时更新中英文手册，并确保：
1. 包名和 import 与源码完全一致；
2. 每个可复制代码示例都有完整 import；
3. 类型、注册方法、回调、参数和结果语义均有源码依据；
4. 已实现、迁移中、规划中明确区分；
5. 不把宿主自定义类写成库内置类；
6. 不混合 View 和 Compose API。
