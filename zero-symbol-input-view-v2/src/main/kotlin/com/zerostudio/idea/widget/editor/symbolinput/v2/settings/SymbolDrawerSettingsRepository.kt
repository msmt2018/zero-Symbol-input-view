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
        require(root.keySet() == expected) { "Settings backup fields do not match the supported schema" }
        fun int(name: String): Int {
            val value = root.get(name)
            require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber) { "Invalid integer setting: $name" }
            val number = value.asInt
            require(value.asDouble == number.toDouble()) { "Non-integral setting: $name" }
            return number
        }
        fun bool(name: String): Boolean {
            val value = root.get(name)
            require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) { "Invalid boolean setting: $name" }
            return value.asBoolean
        }
        fun enumValue(name: String): String {
            val value = root.get(name)
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { "Invalid enum setting: $name" }
            return value.asString
        }

        val version = int("schemaVersion")
        require(version == SymbolDrawerSettings.CURRENT_SCHEMA_VERSION) {
            "Unsupported settings schema version: $version"
        }
        return SymbolDrawerSettings(
            schemaVersion = version,
            themeMode = ThemeMode.valueOf(enumValue("themeMode")),
            columns = int("columns"),
            collapsedRows = int("collapsedRows"),
            cellHeightDp = int("cellHeightDp"),
            cellHorizontalSpacingDp = int("cellHorizontalSpacingDp"),
            cellVerticalSpacingDp = int("cellVerticalSpacingDp"),
            indicatorHeightDp = int("indicatorHeightDp"),
            indicatorStyle = GroupIndicatorStyle.valueOf(enumValue("indicatorStyle")),
            showGroupTitles = bool("showGroupTitles"),
            showSymbolLabels = bool("showSymbolLabels"),
            showActionHints = bool("showActionHints"),
            maxExpandedHeightDp = int("maxExpandedHeightDp"),
            animationDurationMs = int("animationDurationMs"),
            reduceMotion = bool("reduceMotion"),
            flingVelocityDpPerSecond = int("flingVelocityDpPerSecond"),
            longPressTimeoutMs = int("longPressTimeoutMs"),
            hapticMode = HapticMode.valueOf(enumValue("hapticMode")),
            autoCollapseAfterAction = bool("autoCollapseAfterAction"),
            pageCacheLimit = int("pageCacheLimit"),
            prefetchAdjacentGroups = bool("prefetchAdjacentGroups"),
            minimumTouchTargetDp = int("minimumTouchTargetDp"),
            searchIncludesActionPayload = bool("searchIncludesActionPayload")
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
