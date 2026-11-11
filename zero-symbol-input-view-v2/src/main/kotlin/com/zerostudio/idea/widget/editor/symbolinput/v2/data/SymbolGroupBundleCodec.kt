package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionJsonCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/** Portable, versioned backup format for groups and typed actions. */
object SymbolGroupBundleCodec {
    const val CURRENT_VERSION = 1
    const val MAX_GROUPS = 500
    const val MAX_SYMBOLS_PER_GROUP = 5_000
    const val MAX_TOTAL_SYMBOLS = 50_000
    const val MAX_BUNDLE_CHARS = 16 * 1024 * 1024
    const val MAX_ACTION_TEXT_CHARS = 1024 * 1024

    fun encode(groups: List<SymbolGroup>): String {
        validateGroups(groups)
        val root = JsonObject().apply {
            addProperty("format", "zero-symbol-drawer")
            addProperty("version", CURRENT_VERSION)
            add("groups", JsonArray().also { groupArray ->
                groups.forEach { group ->
                    groupArray.add(JsonObject().apply {
                        addProperty("id", group.id)
                        addProperty("title", group.title)
                        add("symbols", JsonArray().also { symbolArray ->
                            group.symbols.forEach { symbol ->
                                symbolArray.add(JsonObject().apply {
                                    addProperty("id", symbol.id)
                                    addProperty("label", symbol.label)
                                    symbol.icon?.let { addProperty("icon", it) }
                                    add("shortAction", SymbolActionJsonCodec.encode(symbol.shortAction))
                                    symbol.longAction?.let { add("longAction", SymbolActionJsonCodec.encode(it)) }
                                })
                            }
                        })
                    })
                }
            })
        }
        return root.toString().also { require(it.length <= MAX_BUNDLE_CHARS) { "Backup is too large" } }
    }

    fun decode(json: String): List<SymbolGroup> {
        require(json.length <= MAX_BUNDLE_CHARS) { "Backup is too large" }
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("format")?.asString == "zero-symbol-drawer") { "Not a V2 symbol drawer backup" }
        val version = root.get("version")?.asInt ?: error("Missing backup version")
        require(version == CURRENT_VERSION) { "Unsupported backup version: $version" }
        val groupArray = root.getAsJsonArray("groups") ?: error("Missing groups")
        require(groupArray.size() <= MAX_GROUPS) { "Too many groups in backup" }
        val result = groupArray.map { groupElement ->
            val group = groupElement.asJsonObject
            val symbolArray = group.getAsJsonArray("symbols") ?: JsonArray()
            require(symbolArray.size() <= MAX_SYMBOLS_PER_GROUP) { "Too many symbols in group" }
            SymbolGroup(
                id = group.get("id")?.asString ?: error("Group ID is missing"),
                title = group.get("title")?.asString ?: error("Group title is missing"),
                symbols = symbolArray.map { symbolElement ->
                    val symbol = symbolElement.asJsonObject
                    val shortAction = symbol.getAsJsonObject("shortAction")
                        ?: error("Short action is missing")
                    val longAction = symbol.get("longAction")
                        ?.takeUnless { it.isJsonNull }
                        ?.asJsonObject
                        ?.let { SymbolActionJsonCodec.decode(it) }
                    SymbolEntry(
                        id = symbol.get("id")?.asLong ?: error("Symbol ID is missing"),
                        label = symbol.get("label")?.asString ?: error("Symbol label is missing"),
                        shortAction = SymbolActionJsonCodec.decode(shortAction),
                        longAction = longAction,
                        icon = symbol.get("icon")?.takeUnless { it.isJsonNull }?.asString
                    )
                }
            )
        }
        validateGroups(result)
        return result
    }

    internal fun validateGroups(groups: List<SymbolGroup>) {
        require(groups.size <= MAX_GROUPS) { "Too many groups" }
        var totalSymbols = 0
        require(groups.all {
            it.id.isNotBlank() && it.id.length <= 256 &&
                it.title.isNotBlank() && it.title.length <= 4096
        }) { "Group IDs and titles must be non-blank and within size limits" }
        require(groups.map { it.id }.distinct().size == groups.size) { "Duplicate group IDs" }
        groups.forEach { group ->
            require(group.symbols.size <= MAX_SYMBOLS_PER_GROUP) { "Too many symbols in group ${group.id}" }
            totalSymbols += group.symbols.size
            require(group.symbols.all {
                it.id != 0L && it.label.isNotBlank() && it.label.length <= 4096 &&
                    (it.icon?.length ?: 0) <= 256
            }) { "Symbol IDs, labels or icons are invalid in group ${group.id}" }
            require(group.symbols.map { it.id }.distinct().size == group.symbols.size) {
                "Duplicate symbol IDs in group ${group.id}"
            }
            group.symbols.forEach { symbol ->
                validateAction(symbol.shortAction, 0)
                symbol.longAction?.let { validateAction(it, 0) }
            }
        }
        require(totalSymbols <= MAX_TOTAL_SYMBOLS) { "Too many symbols in backup" }
    }

    private fun validateAction(action: SymbolAction, depth: Int) {
        require(depth <= 8) { "Action nesting limit exceeded" }
        when (action) {
            is SymbolAction.InsertText ->
                require(action.template.length <= MAX_ACTION_TEXT_CHARS) { "Insert action is too large" }
            is SymbolAction.ReplaceSelection ->
                require(action.text.length <= MAX_ACTION_TEXT_CHARS) { "Replace action is too large" }
            is SymbolAction.SurroundSelection ->
                require(action.prefix.length.toLong() + action.suffix.length <= MAX_ACTION_TEXT_CHARS) {
                    "Surround action is too large"
                }
            is SymbolAction.EditorCommand -> {
                require(Regex("[a-zA-Z][a-zA-Z0-9_.-]{0,127}").matches(action.commandId)) {
                    "Invalid editor command ID"
                }
                require((action.payload?.length ?: 0) <= MAX_ACTION_TEXT_CHARS) { "Editor command payload is too large" }
            }
            is SymbolAction.Extension -> {
                require(Regex("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}").matches(action.extensionId)) {
                    "Invalid extension ID"
                }
                require((action.payload?.length ?: 0) <= MAX_ACTION_TEXT_CHARS) { "Extension payload is too large" }
            }
            is SymbolAction.Composite -> {
                require(action.actions.size in 1..64) { "Invalid composite action size" }
                action.actions.forEach { validateAction(it, depth + 1) }
            }
            is SymbolAction.MoveCursor,
            is SymbolAction.Delete,
            is SymbolAction.TransformSelection,
            is SymbolAction.Clipboard,
            SymbolAction.OpenManager -> Unit
        }
    }
}
