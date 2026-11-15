# 11 — Gesture Interaction and Built-in Text Actions

## DrawerInteractionController

This controller is a layout-independent drag/settle engine. It does not create Views or measure content. The host supplies min/max/current heights and applies the `onHeightChanged` callback.

It supports:
- bounds and animation/fling configuration;
- synchronizing a restored/measured height;
- attach/detach cleanup;
- intercept-phase axis arbitration;
- handling an already-intercepted touch sequence;
- expand/collapse/settle;
- animation cancellation and gesture reset.

Horizontal gestures are left to the child pager. Vertical drag takeover depends on drawer bounds and whether the touched child can still scroll upward. Do not feed unclaimed touch sequences into `onTouchEvent` as a replacement for normal child dispatch.

## SymbolDrawerDragPolicy and anchor adapter

The symbol-specific drag policy preserves the product toolbar's geometry and compatibility behavior. `SymbolDrawerAnchorAdapter` maps symbol groups/configuration/density to resolved anchors; it does not own the generic Drawer state.

## BuiltinSymbolActionCatalog

Provides the built-in command catalogue and stable command IDs. Treat IDs and payload schemas as public compatibility data; do not infer commands from display labels.

## GenericSymbolTextActions

`createRegistry()` creates handlers for common editor-neutral commands. `supportedCommandIds()` returns the supported IDs. Command families include line editing, selection transformations, caret movement/deletion, document operations and generic insertion payloads.

Document-wide operations enforce size/line-count limits (the source documentation specifies 32 MiB and 200,000 lines) to avoid unbounded scans. Surface limit/unsupported outcomes rather than silently truncating text.

## SymbolTextEditing and templates

The text utility layer computes ranges and insertion behavior against the document adapter. Template directives documented by the source include literal dollar escaping, selection start/end, caret placement, selected-text insertion, newline/tab, current line/word/indent and selected-text transformations. Unknown marker forms remain literal. See the template table in page 13 and the source API for exact token spelling.

## Adapter requirements

Offsets are UTF-16 code units. Use end-exclusive ranges, handle CRLF and surrogate pairs, and apply multiple edits from right to left. Host adapters must reject writes to read-only documents and should provide transaction hooks if atomicity is required.
