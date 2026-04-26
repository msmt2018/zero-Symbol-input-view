package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

object SymbolDataManager {
    private const val PREFS_NAME = "advanced_symbol_prefs"
    private const val KEY_DATA = "symbol_json_data"
    private const val KEY_COLLAPSED_ROWS = "symbol_collapsed_rows"
    private const val KEY_SYMBOLS_PER_ROW = "symbol_per_row"
    private const val KEY_INDICATOR_STYLE = "symbol_indicator_style"
    private const val KEY_REMEMBER_EXPANDED = "symbol_remember_expanded"
    private const val KEY_UNIFORM_GROUP_HEIGHT = "symbol_uniform_group_height"
    private const val KEY_TEXT_SIZE = "symbol_text_size_sp"
    private const val KEY_SHOW_DRAG_HANDLE = "symbol_show_drag_handle"
    private const val KEY_ADVANCED_ACTIONS = "symbol_enable_advanced_actions"
    private const val KEY_LAST_EXPANDED = "symbol_last_expanded"
    val gson = Gson()

    /**
     * 从 assets 目录读取默认的符号配置文件
     */
    private fun loadDefaultJsonFromAssets(context: Context): String {
        return try {
            val inputStream = context.assets.open("editor/symbolinput/Default-Symbol-input.json")
            InputStreamReader(inputStream).use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            "[]" // 如果由于某种原因找不到该文件，则返回空数组，防止应用崩溃
        }
    }

    /**
     * 加载符号数据。优先从 SharedPreferences(用户自定义存储) 读取，
     * 若未找到(例如首次启动)，则回退到加载 Assets 里的默认 JSON。
     */
    fun loadData(context: Context): MutableList<SymbolGroup> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DATA, null)
        
        // 若没有用户修改的数据，加载 Assets 的默认配置
        val finalJson = if (json.isNullOrEmpty()) loadDefaultJsonFromAssets(context) else json
        
        return try {
            val listType = object : TypeToken<MutableList<SymbolGroup>>() {}.type
            gson.fromJson(finalJson, listType) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    /**
     * 保存用户修改后的数据
     */
    fun saveData(context: Context, data: List<SymbolGroup>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DATA, gson.toJson(data)).apply()
    }

    fun getUiSettings(context: Context): SymbolUiSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return SymbolUiSettings(
            collapsedRows = prefs.getInt(KEY_COLLAPSED_ROWS, 2).coerceIn(1, 10),
            symbolsPerRow = prefs.getInt(KEY_SYMBOLS_PER_ROW, 10).coerceIn(1, 20),
            indicatorStyle = prefs.getInt(KEY_INDICATOR_STYLE, 0).coerceIn(0, 4),
            rememberExpanded = prefs.getBoolean(KEY_REMEMBER_EXPANDED, false),
            uniformGroupHeight = prefs.getBoolean(KEY_UNIFORM_GROUP_HEIGHT, true),
            symbolTextSizeSp = prefs.getInt(KEY_TEXT_SIZE, 18).coerceIn(12, 28),
            showDragHandle = prefs.getBoolean(KEY_SHOW_DRAG_HANDLE, true),
            enableAdvancedActions = prefs.getBoolean(KEY_ADVANCED_ACTIONS, true)
        )
    }

    fun saveUiSettings(context: Context, settings: SymbolUiSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_COLLAPSED_ROWS, settings.collapsedRows.coerceIn(1, 10))
            .putInt(KEY_SYMBOLS_PER_ROW, settings.symbolsPerRow.coerceIn(1, 20))
            .putInt(KEY_INDICATOR_STYLE, settings.indicatorStyle.coerceIn(0, 4))
            .putBoolean(KEY_REMEMBER_EXPANDED, settings.rememberExpanded)
            .putBoolean(KEY_UNIFORM_GROUP_HEIGHT, settings.uniformGroupHeight)
            .putInt(KEY_TEXT_SIZE, settings.symbolTextSizeSp.coerceIn(12, 28))
            .putBoolean(KEY_SHOW_DRAG_HANDLE, settings.showDragHandle)
            .putBoolean(KEY_ADVANCED_ACTIONS, settings.enableAdvancedActions)
            .apply()
    }

    fun setLastExpanded(context: Context, expanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LAST_EXPANDED, expanded).apply()
    }

    fun getLastExpanded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LAST_EXPANDED, false)
    }

    /**
     * 根据Action ID获取中文描述（用于副标题）
     */
    fun getActionDesc(context: Context, actionId: Int, text: String?): String {
        val values = context.resources.getIntArray(R.array.symbol_action_values)
        val names = context.resources.getStringArray(R.array.symbol_action_names)
        val index = values.indexOf(actionId)
        val baseName = if (index >= 0) names[index] else context.getString(R.string.action_unknown)
        return if (actionId == 0) "$baseName: ${text?.replace("\n", "\\n")}" else baseName
    }
}
