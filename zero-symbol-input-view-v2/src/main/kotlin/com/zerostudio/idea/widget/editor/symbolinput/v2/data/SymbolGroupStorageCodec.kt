package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.google.gson.JsonParser
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionJsonCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/**
 * Versioned persistence boundary for V2 groups.
 *
 * New writes use the portable versioned envelope. Reads remain compatible with
 * the unversioned V2 JSON-array format and its migration-only numeric actions.
 */
object SymbolGroupStorageCodec {
    fun encode(groups: List<SymbolGroup>): String =
        SymbolGroupBundleCodec.encode(groups)

    fun decode(json: String): List<SymbolGroup> {
        require(json.length <= SymbolGroupBundleCodec.MAX_BUNDLE_CHARS) {
            "Stored symbol groups exceed the size limit"
        }
        val root = JsonParser.parseString(json)
        if (root.isJsonObject) {
            return SymbolGroupBundleCodec.decode(json)
        }

        val array = root.asJsonArray
        require(array.size() <= SymbolGroupBundleCodec.MAX_GROUPS) { "Too many stored groups" }
        val groups = array.map { rawGroup ->
            val group = rawGroup.asJsonObject
            val symbols = group.getAsJsonArray("symbols")
                ?: com.google.gson.JsonArray()
            require(symbols.size() <= SymbolGroupBundleCodec.MAX_SYMBOLS_PER_GROUP) {
                "Too many symbols in group"
            }
            SymbolGroup(
                id = group.get("id")?.asString ?: error("Group ID is missing"),
                title = group.get("title")?.asString ?: error("Group title is missing"),
                symbols = symbols.map { rawSymbol ->
                    val symbol = rawSymbol.asJsonObject
                    val shortAction = if (symbol.has("shortAction")) {
                        SymbolActionJsonCodec.decode(symbol.getAsJsonObject("shortAction"))
                    } else {
                        LegacySymbolActionAdapter.convert(
                            LegacySymbolAction(
                                symbol.get("shortType").asInt,
                                symbol.get("shortPayload")?.takeUnless { it.isJsonNull }?.asString
                            )
                        )
                    }
                    val longAction = if (symbol.has("longAction") && !symbol.get("longAction").isJsonNull) {
                        SymbolActionJsonCodec.decode(symbol.getAsJsonObject("longAction"))
                    } else if (symbol.has("longType") && !symbol.get("longType").isJsonNull) {
                        LegacySymbolActionAdapter.convert(
                            LegacySymbolAction(
                                symbol.get("longType").asInt,
                                symbol.get("longPayload")?.takeUnless { it.isJsonNull }?.asString
                            )
                        )
                    } else null
                    SymbolEntry(
                        id = symbol.get("id")?.asLong ?: error("Symbol ID is missing"),
                        label = symbol.get("label")?.asString ?: error("Symbol label is missing"),
                        shortAction = shortAction,
                        longAction = longAction,
                        icon = symbol.get("icon")?.takeUnless { it.isJsonNull }?.asString
                    )
                }
            )
        }
        SymbolGroupBundleCodec.validateGroups(groups)
        return groups
    }
}
