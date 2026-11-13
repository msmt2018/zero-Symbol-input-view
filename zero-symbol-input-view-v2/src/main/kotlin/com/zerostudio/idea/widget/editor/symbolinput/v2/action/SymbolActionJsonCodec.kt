/*
 * This file is part of ZeroStudio.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*

/** Explicit discriminator-based codec; never relies on Gson polymorphic reflection. */
object SymbolActionJsonCodec {
    fun encode(action: SymbolAction): JsonObject {
        val json = JsonObject()
        when (action) {
            is SymbolAction.InsertText -> { json.addProperty("kind", "insertText"); json.addProperty("template", action.template) }
            is SymbolAction.ReplaceSelection -> { json.addProperty("kind", "replaceSelection"); json.addProperty("text", action.text) }
            is SymbolAction.SurroundSelection -> {
                json.addProperty("kind", "surroundSelection")
                json.addProperty("prefix", action.prefix)
                json.addProperty("suffix", action.suffix)
            }
            is SymbolAction.MoveCursor -> { json.addProperty("kind", "moveCursor"); json.addProperty("target", action.target.name) }
            is SymbolAction.Delete -> { json.addProperty("kind", "delete"); json.addProperty("deleteKind", action.kind.name) }
            is SymbolAction.TransformSelection -> { json.addProperty("kind", "transformSelection"); json.addProperty("transform", action.transform.name) }
            is SymbolAction.EditorCommand -> {
                json.addProperty("kind", "editorCommand")
                json.addProperty("commandId", action.commandId)
                action.payload?.let { json.addProperty("payload", it) }
            }
            is SymbolAction.Clipboard -> { json.addProperty("kind", "clipboard"); json.addProperty("operation", action.operation.name) }
            SymbolAction.OpenManager -> json.addProperty("kind", "openManager")
            is SymbolAction.Composite -> {
                json.addProperty("kind", "composite")
                json.add("actions", JsonArray().also { array -> action.actions.forEach { array.add(encode(it)) } })
            }
            is SymbolAction.Extension -> {
                json.addProperty("kind", "extension")
                json.addProperty("extensionId", action.extensionId)
                action.payload?.let { json.addProperty("payload", it) }
            }
        }
        return json
    }

    fun decode(json: JsonObject, depth: Int = 0): SymbolAction {
        require(depth <= 8) { "Action JSON nesting limit exceeded" }
        fun required(name: String): String {
            val value = json.get(name)
                ?: throw IllegalArgumentException("Missing action field: $name")
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                "Invalid action field: $name must be a string"
            }
            return value.asString
        }
        fun optional(name: String): String? {
            val value = json.get(name) ?: return null
            if (value.isJsonNull) return null
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                "Invalid action field: $name must be a string"
            }
            return value.asString
        }
        fun <T : Enum<T>> enumValueOf(name: String, values: Array<T>): T =
            values.firstOrNull { it.name == required(name) }
                ?: throw IllegalArgumentException("Invalid action enum: $name")

        return when (required("kind")) {
            "insertText" -> SymbolAction.InsertText(required("template"))
            "replaceSelection" -> SymbolAction.ReplaceSelection(required("text"))
            "surroundSelection" -> SymbolAction.SurroundSelection(required("prefix"), required("suffix"))
            "moveCursor" -> SymbolAction.MoveCursor(enumValueOf("target", CursorTarget.values()))
            "delete" -> SymbolAction.Delete(enumValueOf("deleteKind", DeleteKind.values()))
            "transformSelection" -> SymbolAction.TransformSelection(enumValueOf("transform", SelectionTransform.values()))
            "editorCommand" -> SymbolAction.EditorCommand(required("commandId"), optional("payload"))
            "clipboard" -> SymbolAction.Clipboard(enumValueOf("operation", ClipboardOperation.values()))
            "openManager" -> SymbolAction.OpenManager
            "composite" -> {
                val array = json.getAsJsonArray("actions")
                    ?: throw IllegalArgumentException("Composite action has no actions")
                require(array.size() in 1..64) { "Invalid composite action size" }
                SymbolAction.Composite(array.map { element ->
                    require(element.isJsonObject) { "Composite action entries must be objects" }
                    decode(element.asJsonObject, depth + 1)
                })
            }
            "extension" -> SymbolAction.Extension(required("extensionId"), optional("payload"))
            else -> throw IllegalArgumentException("Unknown action kind")
        }
    }
}
