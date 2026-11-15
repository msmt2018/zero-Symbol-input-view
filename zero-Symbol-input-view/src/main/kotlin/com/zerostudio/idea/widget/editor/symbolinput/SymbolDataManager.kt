/*
 * This file is part of ZeroStudio.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 符号数据与配置管理器。
 *
 * 提供分组数据的读取/保存、UI 设置读取/持久化以及偏好项变更判定能力。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
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
    private const val KEY_REMEMBER_LAST_PAGE = "symbol_remember_last_page"
    private const val KEY_LAST_EXPANDED = "symbol_last_expanded"
    private const val KEY_LAST_PAGE_INDEX = "symbol_last_page_index"
    val gson = Gson()

    /**
     * 加载符号数据。优先从 SharedPreferences(用户自定义存储) 读取。
     * 若未找到（例如首次启动）则使用内置默认配置，并主动写入缓存，避免后续 I/O。
     */
    fun loadData(context: Context): MutableList<SymbolGroup> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DATA, null)

        if (json.isNullOrEmpty()) {
            val defaults = SymbolDefaults.createFallbackGroups()
            saveData(context, defaults)
            return SymbolDefaults.deepCopy(defaults)
        }

        return try {
            val listType = object : TypeToken<MutableList<SymbolGroup>>() {}.type
            gson.fromJson<MutableList<SymbolGroup>>(json, listType)
                ?.takeIf { it.isNotEmpty() }
                ?: SymbolDefaults.createFallbackGroups().also { saveData(context, it) }
        } catch (e: Exception) {
            e.printStackTrace()
            SymbolDefaults.createFallbackGroups().also { saveData(context, it) }
        }
    }

    /**
     * 保存用户修改后的分组数据到本地持久化存储。
     */
    fun saveData(context: Context, data: List<SymbolGroup>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DATA, gson.toJson(data)).apply()
    }

    /**
     * 读取并构建当前 UI 配置。
     */
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
            enableAdvancedActions = prefs.getBoolean(KEY_ADVANCED_ACTIONS, true),
            rememberLastPage = prefs.getBoolean(KEY_REMEMBER_LAST_PAGE, true)
        )
    }

    /**
     * 持久化 UI 配置。
     */
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
            .putBoolean(KEY_REMEMBER_LAST_PAGE, settings.rememberLastPage)
            .apply()
    }

    /**
     * 执行 setLastExpanded 方法。
     */
    fun setLastExpanded(context: Context, expanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LAST_EXPANDED, expanded).apply()
    }

    /**
     * 执行 getLastExpanded 方法。
     */
    fun getLastExpanded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LAST_EXPANDED, false)
    }

    /**
     * 执行 setLastPageIndex 方法。
     */
    fun setLastPageIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_PAGE_INDEX, index.coerceAtLeast(0)).apply()
    }

    /**
     * 执行 getLastPageIndex 方法。
     */
    fun getLastPageIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_PAGE_INDEX, 0).coerceAtLeast(0)
    }


    /**
     * 判断某个首选项键是否会影响符号输入控件 UI，需要触发刷新。
     */
    fun shouldTriggerUiRefresh(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        return key in setOf(
            KEY_DATA,
            KEY_COLLAPSED_ROWS,
            KEY_SYMBOLS_PER_ROW,
            KEY_INDICATOR_STYLE,
            KEY_REMEMBER_EXPANDED,
            KEY_UNIFORM_GROUP_HEIGHT,
            KEY_TEXT_SIZE,
            KEY_SHOW_DRAG_HANDLE,
            KEY_ADVANCED_ACTIONS,
            KEY_REMEMBER_LAST_PAGE
        )
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
