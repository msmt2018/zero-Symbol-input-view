# Compose V2 API Documentation

This directory defines the integration contract for the native Compose module only.

## Canonical guides

- [简体中文：Compose V2 API 开发与集成手册](../COMPOSE_API_REFERENCE.md)
- [English: Compose V2 API Development and Integration Guide](COMPOSE_API_REFERENCE.md)

The canonical guides cover module dependencies, fully qualified packages and imports, SymbolDrawer integration, SymbolActionHost, SymbolTextDocument, action results, all eight extension registration categories, Compose renderers/indicators, dynamic providers, theme/events/settings extensions, manager and settings routes, repositories, JSON interoperability, lifecycle-free UI and migration boundaries.

## Historical numbered pages

The original `01-quick-start.md` through `16-testing-release-troubleshooting.md` (and the English counterparts) were inherited from the Android View implementation. They contain historical examples such as `SymbolDrawerView`, View listeners/adapters and Activity/LifecycleOwner integration.

Those pages are **not** the Compose module's integration contract. Use the canonical guides for all new Compose integrations and extensions. The old pages may only be consulted when maintaining the historical View API; do not copy their snippets into the Compose module.

## Documentation parity rules

Every public API change must update both language guides. Package names, imports, types, registration methods, callbacks, arguments and result semantics must match source exactly. Every copyable snippet must include imports. Mark APIs as implemented, migrating or planned. Never present host-owned example classes as library classes, and never mix View and Compose APIs.
