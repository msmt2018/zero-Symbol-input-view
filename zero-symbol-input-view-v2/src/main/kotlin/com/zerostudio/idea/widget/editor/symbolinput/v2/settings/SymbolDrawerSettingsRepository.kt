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
}

data class SettingsLoadResult(
    val settings: SymbolDrawerSettings,
    val recoveredFromInvalidData: Boolean = false
)

/** Versioned JSON codec kept separate so defaults and malformed values are testable. */
object SymbolDrawerSettingsCodec {
    private val gson = Gson()

    fun encode(settings: SymbolDrawerSettings): String = gson.toJson(settings)

    fun decode(json: String): SymbolDrawerSettings {
        val root = JsonParser.parseString(json).asJsonObject
        fun int(name: String, default: Int) =
            root.get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: default
        fun bool(name: String, default: Boolean) =
            root.get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean ?: default
        fun enumValue(name: String, default: String) =
            root.get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: default

        val d = SymbolDrawerSettings()
        val version = int("schemaVersion", d.schemaVersion)
        require(version in 1..SymbolDrawerSettings.CURRENT_SCHEMA_VERSION) { "Unsupported settings schema version: $version" }
        return SymbolDrawerSettings(
            schemaVersion = version,
            themeMode = enumValue("themeMode", d.themeMode.name).let { runCatching { ThemeMode.valueOf(it) }.getOrDefault(d.themeMode) },
            columns = int("columns", d.columns),
            collapsedRows = int("collapsedRows", d.collapsedRows),
            cellHeightDp = int("cellHeightDp", d.cellHeightDp),
            cellHorizontalSpacingDp = int("cellHorizontalSpacingDp", d.cellHorizontalSpacingDp),
            cellVerticalSpacingDp = int("cellVerticalSpacingDp", d.cellVerticalSpacingDp),
            indicatorHeightDp = int("indicatorHeightDp", d.indicatorHeightDp),
            indicatorStyle = enumValue("indicatorStyle", d.indicatorStyle.name).let { runCatching { GroupIndicatorStyle.valueOf(it) }.getOrDefault(d.indicatorStyle) },
            showGroupTitles = bool("showGroupTitles", d.showGroupTitles),
            showSymbolLabels = bool("showSymbolLabels", d.showSymbolLabels),
            showActionHints = bool("showActionHints", d.showActionHints),
            maxExpandedHeightDp = int("maxExpandedHeightDp", d.maxExpandedHeightDp),
            animationDurationMs = int("animationDurationMs", d.animationDurationMs),
            reduceMotion = bool("reduceMotion", d.reduceMotion),
            flingVelocityDpPerSecond = int("flingVelocityDpPerSecond", d.flingVelocityDpPerSecond),
            longPressTimeoutMs = int("longPressTimeoutMs", d.longPressTimeoutMs),
            hapticMode = enumValue("hapticMode", d.hapticMode.name).let { runCatching { HapticMode.valueOf(it) }.getOrDefault(d.hapticMode) },
            autoCollapseAfterAction = bool("autoCollapseAfterAction", d.autoCollapseAfterAction),
            pageCacheLimit = int("pageCacheLimit", d.pageCacheLimit),
            prefetchAdjacentGroups = bool("prefetchAdjacentGroups", d.prefetchAdjacentGroups),
            minimumTouchTargetDp = int("minimumTouchTargetDp", d.minimumTouchTargetDp),
            searchIncludesActionPayload = bool("searchIncludesActionPayload", d.searchIncludesActionPayload)
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

    private companion object {
        const val KEY_SETTINGS = "settings_json_v1"
    }
}
