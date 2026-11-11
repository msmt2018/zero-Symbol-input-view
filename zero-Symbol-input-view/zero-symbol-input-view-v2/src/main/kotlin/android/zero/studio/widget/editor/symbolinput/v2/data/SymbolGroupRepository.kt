package android.zero.studio.widget.editor.symbolinput.v2.data

import android.content.Context
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolAction
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolEntry
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Data boundary used by the view and by a future manager screen. */
interface SymbolGroupRepository {
    fun loadGroups(): List<SymbolGroup>
    fun saveGroups(groups: List<SymbolGroup>)
}

/**
 * Small SharedPreferences implementation. Uses a v2-only key so the experimental
 * library cannot overwrite the legacy library's persisted symbol configuration.
 */
class SharedPreferencesSymbolGroupRepository(
    context: Context,
    preferencesName: String = "symbol_drawer_v2",
    private val gson: Gson = Gson()
) : SymbolGroupRepository {
    private val preferences = context.applicationContext
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun loadGroups(): List<SymbolGroup> {
        val json = preferences.getString(KEY_GROUPS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<StoredGroup>>() {}.type
            val stored: List<StoredGroup> = gson.fromJson(json, type) ?: emptyList()
            stored.map { group ->
                SymbolGroup(
                    id = group.id,
                    title = group.title,
                    symbols = group.symbols.map { symbol ->
                        SymbolEntry(
                            id = symbol.id,
                            label = symbol.label,
                            shortAction = SymbolAction(symbol.shortType, symbol.shortPayload),
                            longAction = symbol.longType?.let { SymbolAction(it, symbol.longPayload) }
                        )
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    override fun saveGroups(groups: List<SymbolGroup>) {
        val stored = groups.map { group ->
            StoredGroup(
                id = group.id,
                title = group.title,
                symbols = group.symbols.map { symbol ->
                    StoredSymbol(
                        id = symbol.id,
                        label = symbol.label,
                        shortType = symbol.shortAction.type,
                        shortPayload = symbol.shortAction.payload,
                        longType = symbol.longAction?.type,
                        longPayload = symbol.longAction?.payload
                    )
                }
            )
        }
        preferences.edit().putString(KEY_GROUPS, gson.toJson(stored)).apply()
    }

    private data class StoredGroup(val id: String, val title: String, val symbols: List<StoredSymbol>)
    private data class StoredSymbol(
        val id: Long,
        val label: String,
        val shortType: Int,
        val shortPayload: String?,
        val longType: Int?,
        val longPayload: String?
    )

    private companion object {
        const val KEY_GROUPS = "groups_json_v2"
    }
}
