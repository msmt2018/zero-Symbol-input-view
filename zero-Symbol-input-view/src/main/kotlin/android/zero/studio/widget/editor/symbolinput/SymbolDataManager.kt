package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

object SymbolDataManager {
    private const val PREFS_NAME = "advanced_symbol_prefs"
    private const val KEY_DATA = "symbol_json_data"
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

    /**
     * 根据Action ID获取中文描述（用于副标题）
     */
    fun getActionDesc(context: Context, actionId: Int, text: String?): String {
        val values = context.resources.getIntArray(R.array.symbol_action_values)
        val names = context.resources.getStringArray(R.array.symbol_action_names)
        val index = values.indexOf(actionId)
        val baseName = if (index >= 0) names[index] else "未知"
        return if (actionId == 0) "$baseName: ${text?.replace("\n", "\\n")}" else baseName
    }
}