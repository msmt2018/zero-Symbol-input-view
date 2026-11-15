/*
 * Editor-neutral action API entry points.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.action

typealias SymbolTextDocumentActionHost = com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolTextDocumentActionHost
typealias SymbolTextClipboard = com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolTextClipboard
typealias SymbolActionHost = com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
typealias SymbolActionExecutor = com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionExecutor
typealias SymbolActionResult = com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionResult
typealias SymbolActionCancellation = com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionCancellation
typealias GenericSymbolTextActions = com.zerostudio.idea.widget.editor.symbolinput.v2.action.GenericSymbolTextActions

/** Persistable action model; action IDs and handler registry are real JVM API types. */
typealias SymbolAction = com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
