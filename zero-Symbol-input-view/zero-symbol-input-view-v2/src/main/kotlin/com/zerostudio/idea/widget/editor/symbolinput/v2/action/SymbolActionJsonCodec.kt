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
        fun required(name: String) = json.get(name)?.takeIf { !it.isJsonNull }?.asString
            ?: throw IllegalArgumentException("Missing action field: $name")
        fun optional(name: String) = json.get(name)?.takeIf { !it.isJsonNull }?.asString
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
                SymbolAction.Composite(array.map { decode(it.asJsonObject, depth + 1) })
            }
            "extension" -> SymbolAction.Extension(required("extensionId"), optional("payload"))
            else -> throw IllegalArgumentException("Unknown action kind")
        }
    }
}
