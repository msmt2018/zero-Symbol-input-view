package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*

/** Persists the exact V1 compact JSON contract: group {n,d}, item {a,b,c,d,e}. */
object LegacySymbolDataCodec {
    private val gson = Gson()

    fun encode(groups: List<SymbolGroup>): String = gson.toJson(groups.map { group ->
        LegacyGroup(group.title, group.symbols.map { symbol ->
            val short = toLegacy(symbol.shortAction)
            val long = symbol.longAction?.let(::toLegacy)
            LegacyItem(short.type, symbol.label, short.payload, long?.type, long?.payload)
        }.toMutableList())
    })

    fun decode(json: String): List<SymbolGroup> {
        val type = object : TypeToken<MutableList<LegacyGroup>>() {}.type
        val legacy: MutableList<LegacyGroup> = gson.fromJson(json, type) ?: mutableListOf()
        require(legacy.size <= SymbolGroupBundleCodec.MAX_GROUPS) { "Too many groups" }
        var total = 0
        return legacy.mapIndexed { groupIndex, group ->
            require(group.name.isNotBlank()) { "Group name is empty" }
            require(group.items.size <= SymbolGroupBundleCodec.MAX_SYMBOLS_PER_GROUP) { "Too many symbols in group ${group.name}" }
            total += group.items.size
            val symbols = group.items.mapIndexed { itemIndex, item ->
                require(item.display.isNotEmpty()) { "Symbol display is empty" }
                SymbolEntry(
                    id = itemIndex.toLong() + 1L,
                    label = item.display,
                    shortAction = LegacySymbolActionAdapter.convert(LegacySymbolAction(item.shortAction, item.shortText)),
                    longAction = item.longAction?.let { LegacySymbolActionAdapter.convert(LegacySymbolAction(it, item.longText)) }
                )
            }
            SymbolGroup("legacy-$groupIndex-${group.name.hashCode()}", group.name, symbols)
        }.also { require(total <= SymbolGroupBundleCodec.MAX_TOTAL_SYMBOLS) { "Too many symbols" } }
    }

    private fun toLegacy(action: SymbolAction): LegacySymbolAction = when (action) {
        is SymbolAction.InsertText -> LegacySymbolAction(0, action.template)
        is SymbolAction.Delete -> LegacySymbolAction(if (action.kind == DeleteKind.TO_LINE_START || action.kind == DeleteKind.TO_LINE_END) 4 else 3)
        is SymbolAction.TransformSelection -> when (action.transform) {
            SelectionTransform.UPPERCASE -> LegacySymbolAction(7)
            SelectionTransform.LOWERCASE -> LegacySymbolAction(8)
            SelectionTransform.TOGGLE_CASE -> error("V1 has no toggle-case action")
        }
        is SymbolAction.MoveCursor -> LegacySymbolAction(when (action.target) {
            CursorTarget.LINE_START -> 16; CursorTarget.LINE_END -> 17
            CursorTarget.LEFT -> 18; CursorTarget.RIGHT -> 19
            CursorTarget.UP -> 20; CursorTarget.DOWN -> 21
            CursorTarget.DOCUMENT_START -> 23; CursorTarget.DOCUMENT_END -> 24
        })
        is SymbolAction.Clipboard -> LegacySymbolAction(when (action.operation) {
            ClipboardOperation.COPY -> 28; ClipboardOperation.CUT -> 29; ClipboardOperation.PASTE -> 30
        })
        SymbolAction.OpenManager -> LegacySymbolAction(22)
        is SymbolAction.EditorCommand -> when (action.commandId) {
            "line.clear" -> LegacySymbolAction(4)
            "editor.indent" -> LegacySymbolAction(9)
            "editor.toggleLineComment" -> LegacySymbolAction(11)
            "selection.selectAll" -> LegacySymbolAction(25)
            "editor.format" -> LegacySymbolAction(32)
            "line.duplicate" -> LegacySymbolAction(35)
            "line.replace" -> LegacySymbolAction(36, action.payload)
            "editor.unindent" -> LegacySymbolAction(39)
            "history.undo" -> LegacySymbolAction(55)
            "history.redo" -> LegacySymbolAction(56)
            else -> error("Action '${action.commandId}' is not part of the V1 action catalog")
        }
        is SymbolAction.Extension -> {
            val id = Regex("legacy\\.action\\.(\\d+)").matchEntire(action.extensionId)
                ?: error("Extension '${action.extensionId}' cannot be represented by V1 JSON")
            LegacySymbolAction(id.groupValues[1].toInt(), action.payload)
        }
        is SymbolAction.ReplaceSelection -> LegacySymbolAction(0, action.text)
        is SymbolAction.SurroundSelection -> error("Surround-selection is not part of the V1 action catalog")
        is SymbolAction.Composite -> error("Composite actions are not part of the V1 action catalog")
    }

    private data class LegacyGroup(
        @SerializedName("n") var name: String = "",
        @SerializedName("d") var items: MutableList<LegacyItem> = mutableListOf()
    )
    private data class LegacyItem(
        @SerializedName("a") var shortAction: Int = 0,
        @SerializedName("b") var display: String = "",
        @SerializedName("c") var shortText: String? = null,
        @SerializedName("d") var longAction: Int? = null,
        @SerializedName("e") var longText: String? = null
    )
}
