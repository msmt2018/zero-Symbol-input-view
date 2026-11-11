package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.google.gson.JsonParser
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*

/** V1 compact JSON is the canonical write format; previous V2 drafts remain readable. */
object SymbolGroupStorageCodec {
    fun encode(groups: List<SymbolGroup>): String {
        SymbolGroupBundleCodec.validateGroups(groups)
        return LegacySymbolDataCodec.encode(groups).also {
            require(it.length <= SymbolGroupBundleCodec.MAX_BUNDLE_CHARS) { "Stored symbol groups exceed the size limit" }
        }
    }

    fun decode(json: String): List<SymbolGroup> {
        require(json.length <= SymbolGroupBundleCodec.MAX_BUNDLE_CHARS) { "Stored symbol groups exceed the size limit" }
        val root = JsonParser.parseString(json)
        if (root.isJsonObject) return SymbolGroupBundleCodec.decode(json)
        val array = root.asJsonArray
        if (array.size() == 0) return emptyList()
        if (array[0].asJsonObject.has("n") || array[0].asJsonObject.has("d")) {
            return LegacySymbolDataCodec.decode(json)
        }
        return decodePreviousV2Array(array)
    }

    private fun decodePreviousV2Array(array: com.google.gson.JsonArray): List<SymbolGroup> {
        require(array.size() <= SymbolGroupBundleCodec.MAX_GROUPS) { "Too many stored groups" }
        val groups = array.mapIndexed { groupIndex, rawGroup ->
            val group = rawGroup.asJsonObject
            val symbols = group.getAsJsonArray("symbols") ?: com.google.gson.JsonArray()
            require(symbols.size() <= SymbolGroupBundleCodec.MAX_SYMBOLS_PER_GROUP) { "Too many symbols in group" }
            val converted = symbols.mapIndexed { index, rawSymbol ->
                val symbol = rawSymbol.asJsonObject
                val short = if (symbol.has("shortAction")) {
                    com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionJsonCodec.decode(symbol.getAsJsonObject("shortAction"))
                } else LegacySymbolActionAdapter.convert(LegacySymbolAction(
                    symbol.get("shortType").asInt,
                    symbol.get("shortPayload")?.takeUnless { it.isJsonNull }?.asString
                ))
                val long = if (symbol.has("longAction") && !symbol.get("longAction").isJsonNull) {
                    com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionJsonCodec.decode(symbol.getAsJsonObject("longAction"))
                } else if (symbol.has("longType") && !symbol.get("longType").isJsonNull) {
                    LegacySymbolActionAdapter.convert(LegacySymbolAction(
                        symbol.get("longType").asInt,
                        symbol.get("longPayload")?.takeUnless { it.isJsonNull }?.asString
                    ))
                } else null
                SymbolEntry(
                    id = symbol.get("id")?.asLong ?: index.toLong() + 1L,
                    label = symbol.get("label")?.asString ?: error("Symbol label is missing"),
                    shortAction = short,
                    longAction = long,
                    icon = symbol.get("icon")?.takeUnless { it.isJsonNull }?.asString
                )
            }
            SymbolGroup(
                id = group.get("id")?.asString ?: "previous-v2-$groupIndex",
                title = group.get("title")?.asString ?: error("Group title is missing"),
                symbols = converted
            )
        }
        SymbolGroupBundleCodec.validateGroups(groups)
        return groups
    }
}
