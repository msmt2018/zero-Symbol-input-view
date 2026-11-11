package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import android.content.Context
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
        return runCatching { SymbolGroupStorageCodec.decode(json) }
            .getOrElse { error ->
                // Preserve the only original copy before defaults or a later save
                // can replace malformed data.
                if (!preferences.contains(KEY_CORRUPT_GROUPS_BACKUP)) {
                    preferences.edit()
                        .putString(KEY_CORRUPT_GROUPS_BACKUP, json)
                        .putString(KEY_CORRUPT_GROUPS_REASON, error.message?.take(512) ?: "Invalid stored data")
                        .commit()
                }
                emptyList()
            }
    }

    override fun isLegacyMigrationComplete(): Boolean =
        preferences.getBoolean(KEY_LEGACY_MIGRATION_COMPLETE, false)

    override fun markLegacyMigrationComplete() {
        preferences.edit().putBoolean(KEY_LEGACY_MIGRATION_COMPLETE, true).apply()
    }

    override fun saveGroups(groups: List<SymbolGroup>) {
        val encoded = SymbolGroupStorageCodec.encode(groups)
        check(preferences.edit().putString(KEY_GROUPS, encoded).commit()) {
            "Unable to persist symbol groups"
        }
    }

    private companion object {
        const val KEY_GROUPS = "groups_json_v2"
        const val KEY_LEGACY_MIGRATION_COMPLETE = "legacy_migration_complete"
        const val KEY_CORRUPT_GROUPS_BACKUP = "groups_corrupt_backup_v2"
        const val KEY_CORRUPT_GROUPS_REASON = "groups_corrupt_reason_v2"
    }
}
