package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

interface SymbolDrawerSettingsRepository {
    fun load(): SettingsLoadResult
    fun save(settings: SymbolDrawerSettings)
    fun reset()
    /** True when a durable user snapshot exists rather than only defaults. */
    fun hasStoredSettings(): Boolean = false
}

data class SettingsLoadResult(
    val settings: SymbolDrawerSettings,
    val recoveredFromInvalidData: Boolean = false
)

/** Versioned JSON codec kept separate so defaults and malformed values are testable. */
object SymbolDrawerSettingsCodec {
    private val gson = Gson()

    fun encode(settings: SymbolDrawerSettings): String = gson.toJson(settings)

    fun decode(json: String): SymbolDrawerSettings = decodeInternal(json, strict = false)

    /** Import path: unlike local preference migration, external backups must be complete. */
    fun decodeStrict(json: String): SymbolDrawerSettings = decodeInternal(json, strict = true)

    private fun decodeInternal(json: String, strict: Boolean): SymbolDrawerSettings {
        val rootElement = JsonParser.parseString(json)
        require(rootElement.isJsonObject) { "Settings backup must be a JSON object" }
        val root = rootElement.asJsonObject
        val expected = setOf(
            "schemaVersion", "themeMode", "columns", "collapsedRows", "cellHeightDp",
            "cellHorizontalSpacingDp", "cellVerticalSpacingDp", "indicatorHeightDp",
            "indicatorStyle", "showGroupTitles", "showSymbolLabels", "showActionHints",
            "maxExpandedHeightDp", "animationDurationMs", "reduceMotion",
            "flingVelocityDpPerSecond", "longPressTimeoutMs", "hapticMode",
            "autoCollapseAfterAction", "pageCacheLimit", "prefetchAdjacentGroups",
            "minimumTouchTargetDp", "searchIncludesActionPayload"
        )
        require(root.keySet().all { it in expected }) { "Settings backup contains unknown fields" }
        if (strict) require(root.keySet() == expected) {
            "Settings backup fields do not match the supported schema"
        }

        val defaults = SymbolDrawerSettings()
        fun int(name: String, default: Int): Int {
            val value = root.get(name) ?: return default
            require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber) { "Invalid integer setting: $name" }
            val number = value.asInt
            require(value.asDouble == number.toDouble()) { "Non-integral setting: $name" }
            return number
        }
        fun bool(name: String, default: Boolean): Boolean {
            val value = root.get(name) ?: return default
            require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) { "Invalid boolean setting: $name" }
            return value.asBoolean
        }
        fun enumValue(name: String, default: String): String {
            val value = root.get(name) ?: return default
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { "Invalid enum setting: $name" }
            return value.asString
        }

        val version = int("schemaVersion", defaults.schemaVersion)
        require(version == SymbolDrawerSettings.CURRENT_SCHEMA_VERSION) {
            "Unsupported settings schema version: $version"
        }
        return SymbolDrawerSettings(
            schemaVersion = version,
            themeMode = ThemeMode.valueOf(enumValue("themeMode", defaults.themeMode.name)),
            columns = int("columns", defaults.columns),
            collapsedRows = int("collapsedRows", defaults.collapsedRows),
            cellHeightDp = int("cellHeightDp", defaults.cellHeightDp),
            cellHorizontalSpacingDp = int("cellHorizontalSpacingDp", defaults.cellHorizontalSpacingDp),
            cellVerticalSpacingDp = int("cellVerticalSpacingDp", defaults.cellVerticalSpacingDp),
            indicatorHeightDp = int("indicatorHeightDp", defaults.indicatorHeightDp),
            indicatorStyle = GroupIndicatorStyle.valueOf(enumValue("indicatorStyle", defaults.indicatorStyle.name)),
            showGroupTitles = bool("showGroupTitles", defaults.showGroupTitles),
            showSymbolLabels = bool("showSymbolLabels", defaults.showSymbolLabels),
            showActionHints = bool("showActionHints", defaults.showActionHints),
            maxExpandedHeightDp = int("maxExpandedHeightDp", defaults.maxExpandedHeightDp),
            animationDurationMs = int("animationDurationMs", defaults.animationDurationMs),
            reduceMotion = bool("reduceMotion", defaults.reduceMotion),
            flingVelocityDpPerSecond = int("flingVelocityDpPerSecond", defaults.flingVelocityDpPerSecond),
            longPressTimeoutMs = int("longPressTimeoutMs", defaults.longPressTimeoutMs),
            hapticMode = HapticMode.valueOf(enumValue("hapticMode", defaults.hapticMode.name)),
            autoCollapseAfterAction = bool("autoCollapseAfterAction", defaults.autoCollapseAfterAction),
            pageCacheLimit = int("pageCacheLimit", defaults.pageCacheLimit),
            prefetchAdjacentGroups = bool("prefetchAdjacentGroups", defaults.prefetchAdjacentGroups),
            minimumTouchTargetDp = int("minimumTouchTargetDp", defaults.minimumTouchTargetDp),
            searchIncludesActionPayload = bool("searchIncludesActionPayload", defaults.searchIncludesActionPayload)
        )
    }
}

class SharedPreferencesSymbolDrawerSettingsRepository(
    context: Context,
    preferencesName: String = "symbol_drawer_v2_settings",
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
) : SymbolDrawerSettingsRepository {
    override fun load(): SettingsLoadResult {
        val raw = preferences.getString(KEY_SETTINGS, null)
            ?: return SettingsLoadResult(SymbolDrawerSettings())
        val decoded = runCatching { SymbolDrawerSettingsCodec.decode(raw) }
        return decoded.fold(
            onSuccess = { SettingsLoadResult(it) },
            onFailure = { SettingsLoadResult(SymbolDrawerSettings(), recoveredFromInvalidData = true) }
        )
    }

    override fun save(settings: SymbolDrawerSettings) {
        // Constructor validation rejects out-of-range settings before persistence.
        val json = SymbolDrawerSettingsCodec.encode(settings)
        check(preferences.edit().putString(KEY_SETTINGS, json).commit()) {
            "Unable to persist symbol drawer settings"
        }
    }

    override fun reset() {
        check(preferences.edit().remove(KEY_SETTINGS).commit()) {
            "Unable to reset symbol drawer settings"
        }
    }

    override fun hasStoredSettings(): Boolean = preferences.contains(KEY_SETTINGS)

    private companion object {
        const val KEY_SETTINGS = "settings_json_v1"
    }
}
