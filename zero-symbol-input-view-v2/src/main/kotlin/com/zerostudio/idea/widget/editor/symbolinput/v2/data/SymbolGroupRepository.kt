package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionJsonCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/** Data boundary used by the drawer and the V2 manager UI. */
interface SymbolGroupRepository {
    fun loadGroups(): List<SymbolGroup>
    fun saveGroups(groups: List<SymbolGroup>)
    fun isLegacyMigrationComplete(): Boolean = loadGroups().isNotEmpty()
    fun markLegacyMigrationComplete() = Unit
}

/**
 * V2-only SharedPreferences store. The explicit JSON codec gives actions a
 * stable discriminator-based schema and still reads the previous V2 draft schema.
 */
class SharedPreferencesSymbolGroupRepository(
    context: Context,
    preferencesName: String = "symbol_drawer_v2"
) : SymbolGroupRepository {
    private val preferences = context.applicationContext
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun loadGroups(): List<SymbolGroup> {
        val json = preferences.getString(KEY_GROUPS, null) ?: return emptyList()
        return runCatching {
            val root = JsonParser.parseString(json).asJsonArray
            root.map { rawGroup ->
                val group = rawGroup.asJsonObject
                val symbols = group.getAsJsonArray("symbols") ?: JsonArray()
                SymbolGroup(
                    id = group.get("id").asString,
                    title = group.get("title").asString,
                    symbols = symbols.map { rawSymbol ->
                        val symbol = rawSymbol.asJsonObject
                        val shortAction = if (symbol.has("shortAction")) {
                            SymbolActionJsonCodec.decode(symbol.getAsJsonObject("shortAction"))
                        } else {
                            LegacySymbolActionAdapter.convert(
                                LegacySymbolAction(symbol.get("shortType").asInt, symbol.get("shortPayload")?.takeUnless { it.isJsonNull }?.asString)
                            )
                        }
                        val longAction = if (symbol.has("longAction") && !symbol.get("longAction").isJsonNull) {
                            SymbolActionJsonCodec.decode(symbol.getAsJsonObject("longAction"))
                        } else if (symbol.has("longType") && !symbol.get("longType").isJsonNull) {
                            LegacySymbolActionAdapter.convert(
                                LegacySymbolAction(symbol.get("longType").asInt, symbol.get("longPayload")?.takeUnless { it.isJsonNull }?.asString)
                            )
                        } else null
                        SymbolEntry(
                            id = symbol.get("id").asLong,
                            label = symbol.get("label").asString,
                            shortAction = shortAction,
                            longAction = longAction,
                            icon = symbol.get("icon")?.takeUnless { it.isJsonNull }?.asString
                        )
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    override fun isLegacyMigrationComplete(): Boolean =
        preferences.getBoolean(KEY_LEGACY_MIGRATION_COMPLETE, false)

    override fun markLegacyMigrationComplete() {
        preferences.edit().putBoolean(KEY_LEGACY_MIGRATION_COMPLETE, true).apply()
    }

    override fun saveGroups(groups: List<SymbolGroup>) {
        val root = JsonArray()
        groups.forEach { group ->
            val storedGroup = JsonObject().apply {
                addProperty("id", group.id)
                addProperty("title", group.title)
                add("symbols", JsonArray().also { symbols ->
                    group.symbols.forEach { symbol ->
                        symbols.add(JsonObject().apply {
                            addProperty("id", symbol.id)
                            addProperty("label", symbol.label)
                            symbol.icon?.let { addProperty("icon", it) }
                            add("shortAction", SymbolActionJsonCodec.encode(symbol.shortAction))
                            symbol.longAction?.let { add("longAction", SymbolActionJsonCodec.encode(it)) }
                        })
                    }
                })
            }
            root.add(storedGroup)
        }
        check(preferences.edit().putString(KEY_GROUPS, root.toString()).commit()) {
            "Unable to persist symbol groups"
        }
    }

    private companion object {
        const val KEY_GROUPS = "groups_json_v2"
        const val KEY_LEGACY_MIGRATION_COMPLETE = "legacy_migration_complete"
    }
}
