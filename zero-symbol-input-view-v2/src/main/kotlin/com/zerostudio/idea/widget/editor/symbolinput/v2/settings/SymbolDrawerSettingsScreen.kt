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

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.text.InputType
import com.google.android.material.tabs.TabLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.HostThemeColors

/**
 * Settings-only surface. Symbol-group editing stays in the dedicated manager
 * dialog; this screen owns the two backup/import workflows.
 */
class SymbolDrawerSettingsScreen(
    context: Context,
    private val repository: SymbolDrawerSettingsRepository =
        SharedPreferencesSymbolDrawerSettingsRepository(context),
    private val groupRepository: SymbolGroupRepository =
        SharedPreferencesSymbolGroupRepository(context)
) : LinearLayout(context) {

    var onSettingsChanged: ((SymbolDrawerSettings) -> Unit)? = null
    var onGroupsChanged: ((List<SymbolGroup>) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    private var current = repository.load().settings.copy(themeMode = ThemeMode.SYSTEM)
    private var activePage: LinearLayout? = null
    private var pendingImport: ImportKind = ImportKind.SETTINGS
    private val content = LinearLayout(context).apply { orientation = VERTICAL }
    private val documentPicker = JsonDocumentPicker(
        context,
        onImported = ::handleImportedJson,
        onError = { onError?.invoke(it); toast("文件处理失败：${it.message ?: "未知错误"}") }
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(surfaceColor())
        setPadding(dp(12), dp(12), dp(12), dp(8))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        render()
    }

    fun reload() {
        current = repository.load().settings.copy(themeMode = ThemeMode.SYSTEM)
        render()
        onSettingsChanged?.invoke(current)
    }

    fun getSettings(): SymbolDrawerSettings = current

    private fun render() {
        content.removeAllViews()
        content.setBackgroundColor(surfaceColor())
        activePage = null

        content.addView(LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = surfaceBackground(surfaceColor(), 0.10f, 22)
            addView(TextView(context).apply {
                text = "符号抽屉设置"
                textSize = 23f
                setTextColor(primaryColor())
                setTypeface(typeface, Typeface.BOLD)
            }, matchWrap())
            addView(TextView(context).apply {
                text = "按类别管理外观、布局、交互、性能与数据"
                textSize = 13f
                setTextColor(onSurfaceColor())
                alpha = 0.72f
                setPadding(0, dp(5), 0, 0)
            }, matchWrap())
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val tabs = TabLayout(context).apply {
            tabMode = TabLayout.MODE_SCROLLABLE
            tabGravity = TabLayout.GRAVITY_START
            setSelectedTabIndicatorColor(primaryColor())
            setTabTextColors(onSurfaceColor(), primaryColor())
            setBackgroundColor(surfaceColor())
        }
        content.addView(tabs, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))

        val pageHost = FrameLayout(context)
        content.addView(pageHost, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        val builders: List<Pair<String, () -> Unit>> = listOf(
            "外观" to {
                section("主题与显示")
                addChoice("主题模式", listOf(ThemeMode.SYSTEM), ThemeMode.SYSTEM) {
                    update { copy(themeMode = ThemeMode.SYSTEM) }
                }
                addChoice("分组指示器样式", GroupIndicatorStyle.values().toList(), current.indicatorStyle) {
                    update { copy(indicatorStyle = it) }
                }
                addSwitch("显示分组名称", current.showGroupTitles) { update { copy(showGroupTitles = it) } }
                addSwitch("显示符号标签", current.showSymbolLabels) { update { copy(showSymbolLabels = it) } }
                addSwitch("显示动作提示", current.showActionHints) { update { copy(showActionHints = it) } }
            },
            "布局" to {
                section("尺寸与间距")
                addSeek("每行列数", current.columns, 1, 20) { update { copy(columns = it) } }
                addSeek("折叠时显示行数", current.collapsedRows, 1, 10) { update { copy(collapsedRows = it) } }
                addSeek("单元格高度 (dp)", current.cellHeightDp, 10, 96) { update { copy(cellHeightDp = it) } }
                addSeek("横向间距 (dp)", current.cellHorizontalSpacingDp, -10, 24) { update { copy(cellHorizontalSpacingDp = it) } }
                addSeek("纵向间距 (dp)", current.cellVerticalSpacingDp, -10, 24) { update { copy(cellVerticalSpacingDp = it) } }
                addSeek("分组栏高度 (dp)", current.indicatorHeightDp, 0, 64) { update { copy(indicatorHeightDp = it) } }
                addSeek("最大展开高度 (dp)", current.maxExpandedHeightDp, 110, 1200) { update { copy(maxExpandedHeightDp = it) } }
                addSeek("最小触摸目标 (dp)", current.minimumTouchTargetDp, 32, 80) { update { copy(minimumTouchTargetDp = it) } }
            },
            "交互" to {
                section("手势与动作")
                addSeek("长按触发时间 (ms)", current.longPressTimeoutMs, 250, 1000) { update { copy(longPressTimeoutMs = it) } }
                addSeek("展开甩动阈值 (dp/s)", current.flingVelocityDpPerSecond, 100, 6000) { update { copy(flingVelocityDpPerSecond = it) } }
                addSwitch("执行动作后自动收起", current.autoCollapseAfterAction) { update { copy(autoCollapseAfterAction = it) } }
                section("动画与触觉")
                addSeek("动画时长 (ms)", current.animationDurationMs, 0, 1200) { update { copy(animationDurationMs = it) } }
                addSwitch("减少动态效果", current.reduceMotion) { update { copy(reduceMotion = it) } }
                addChoice("触觉反馈", HapticMode.values().toList(), current.hapticMode) { update { copy(hapticMode = it) } }
            },
            "性能与搜索" to {
                section("性能")
                addSeek("页面缓存数量", current.pageCacheLimit, 1, 9) { update { copy(pageCacheLimit = it) } }
                addSwitch("预加载相邻分组", current.prefetchAdjacentGroups) { update { copy(prefetchAdjacentGroups = it) } }
                section("搜索")
                addSwitch("搜索时包含动作内容", current.searchIncludesActionPayload) {
                    update { copy(searchIncludesActionPayload = it) }
                }
            },
            "数据管理" to {
                addDataManagementCard(
                    "设置备份",
                    "备份当前外观、布局、交互与性能选项；导入设置不会修改符号分组。",
                    listOf(
                        "复制设置 JSON" to ::copySettingsBackup,
                        "导出设置文件" to ::exportSettingsFile,
                        "粘贴导入设置" to ::promptImportSettings,
                        "从文件导入设置" to { pendingImport = ImportKind.SETTINGS; documentPicker.openJson() }
                    )
                )
                addDataManagementCard(
                    "符号组备份",
                    "包含分组、符号内容及其短按/长按动作。导入会替换当前全部符号组。",
                    listOf(
                        "复制符号组 JSON" to ::copyGroupsBackup,
                        "导出符号组文件" to ::exportGroupsFile,
                        "粘贴导入符号组" to ::promptImportGroups,
                        "从文件导入符号组" to { pendingImport = ImportKind.GROUPS; documentPicker.openJson() }
                    )
                )
                addView(TextView(context).apply {
                    text = "数据安全提示：两类备份相互独立。导入前会校验 JSON 结构、字段类型、动作编号和数据范围；建议在覆盖数据前先导出备份。"
                    textSize = 12f
                    setTextColor(onSurfaceColor())
                    alpha = 0.72f
                    setPadding(dp(4), dp(12), dp(4), dp(8))
                }, matchWrap())
                activePage?.addView(actionButton("恢复默认设置") {
                    runCatching {
                        val defaults = SymbolDrawerSettings()
                        repository.save(defaults)
                        current = defaults
                        render()
                        onSettingsChanged?.invoke(current)
                        toast("设置已恢复默认并保存")
                    }.onFailure(::reportFailure)
                }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(50)).apply {
                    topMargin = dp(14)
                })
            }
        )

        builders.forEachIndexed { index, entry ->
            tabs.addTab(tabs.newTab().setText(entry.first), index == 0)
            val page = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(dp(16), dp(8), dp(16), dp(28))
            }
            val scroll = ScrollView(context).apply {
                isFillViewport = true
                addView(page, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                visibility = if (index == 0) View.VISIBLE else View.GONE
            }
            pageHost.addView(scroll, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            activePage = page
            entry.second.invoke()
        }
        activePage = null
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                for (i in 0 until pageHost.childCount) {
                    pageHost.getChildAt(i).visibility = if (i == tab.position) View.VISIBLE else View.GONE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun addDataManagementCard(
        title: String,
        description: String,
        actions: List<Pair<String, () -> Unit>>
    ) {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(12))
            background = surfaceBackground(surfaceColor(), 0.10f, 18)
        }
        card.addView(TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(onSurfaceColor())
            setTypeface(typeface, Typeface.BOLD)
        }, matchWrap())
        card.addView(TextView(context).apply {
            text = description
            textSize = 12f
            setTextColor(onSurfaceColor())
            alpha = 0.72f
            setPadding(0, dp(5), 0, dp(12))
        }, matchWrap())
        actions.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            pair.forEachIndexed { index, action ->
                row.addView(actionButton(action.first, action.second), LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    if (index == 0) marginEnd = dp(5) else marginStart = dp(5)
                })
            }
            card.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            })
        }
        activePage?.addView(card, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        })
    }

    private fun actionButton(title: String, action: () -> Unit) =
        com.google.android.material.button.MaterialButton(context).apply {
            text = title
            textSize = 12f
            isAllCaps = false
            cornerRadius = dp(12)
            strokeWidth = dp(1)
            strokeColor = android.content.res.ColorStateList.valueOf(primaryColor())
            backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor())
            setTextColor(primaryColor())
            insetTop = 0
            insetBottom = 0
            minHeight = dp(48)
            setOnClickListener { runCatching(action).onFailure(::reportFailure) }
        }

    private fun section(title: String) {
        activePage?.addView(TextView(context).apply {
            text = title
            textSize = 17f
            setTextColor(onSurfaceColor())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(22), dp(2), dp(10))
        }, matchWrap())
    }

    private fun addSwitch(title: String, checked: Boolean, changed: (Boolean) -> Unit) {
        activePage?.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(5), dp(10), dp(5))
            background = surfaceBackground(surfaceColor(), 0.08f, 14)
            addView(TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(onSurfaceColor())
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(Switch(context).apply {
                isChecked = checked
                setOnCheckedChangeListener { _, value -> changed(value) }
            }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })
    }

    private fun <T : Enum<T>> addChoice(title: String, values: List<T>, selected: T, changed: (T) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = surfaceBackground(surfaceColor(), 0.08f, 14)
        }
        row.addView(label(title), matchWrap())
        val spinner = Spinner(context)
        val initialIndex = values.indexOf(selected).coerceAtLeast(0)
        spinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            values.map { value ->
                when (value) {
                    ThemeMode.SYSTEM -> "跟随宿主 Activity"
                    else -> value.name.lowercase().replace('_', ' ')
                }
            }
        )
        spinner.setSelection(initialIndex, false)
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            private var lastPosition = initialIndex
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == lastPosition) return
                lastPosition = position
                values.getOrNull(position)?.let(changed)
            }
        }
        row.addView(spinner, matchWrap())
        activePage?.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })
    }

    private fun addSeek(title: String, value: Int, minimum: Int, maximum: Int, changed: (Int) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = surfaceBackground(surfaceColor(), 0.05f, 12)
        }
        val valueLabel = label("$title：$value")
        row.addView(valueLabel, matchWrap())
        row.addView(SeekBar(context).apply {
            max = maximum - minimum
            progress = (value - minimum).coerceIn(0, max)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    valueLabel.text = "$title：${progress + minimum}"
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) { changed((bar?.progress ?: 0) + minimum) }
            })
        }, matchWrap())
        activePage?.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })
    }

    private fun copySettingsBackup() = copyJson(
        "Zero Symbol Drawer settings", SymbolDrawerSettingsCodec.encode(current), "设置备份 JSON 已复制"
    )

    private fun exportSettingsFile() = documentPicker.saveJson(
        "symbol-drawer-settings.json", SymbolDrawerSettingsCodec.encode(current)
    )

    private fun copyGroupsBackup() = copyJson(
        "Zero Symbol Drawer groups", SymbolGroupBundleCodec.encode(groupRepository.loadGroups()), "符号组 JSON 已复制"
    )

    private fun exportGroupsFile() = documentPicker.saveJson(
        "symbol-drawer-groups.json", SymbolGroupBundleCodec.encode(groupRepository.loadGroups())
    )

    private fun copyJson(label: String, json: String, message: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, json))
        toast(message)
    }

    private fun promptImportSettings() {
        promptJson("导入设置备份", "粘贴设置 JSON。导入成功后会覆盖当前设置。") { json ->
            importSettings(json)
        }
    }

    private fun promptImportGroups() {
        promptJson("导入符号组", "粘贴 V1 符号组 JSON。导入成功后会替换全部分组。") { json ->
            confirmGroupImport(json)
        }
    }

    private fun promptJson(title: String, message: String, imported: (String) -> Unit) {
        val input = EditText(context).apply {
            hint = "粘贴 JSON"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 6
            gravity = Gravity.TOP
        }
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("继续") { _, _ ->
                runCatching { imported(input.text.toString()) }.onFailure(::reportFailure)
            }
            .show()
    }

    private fun handleImportedJson(json: String) {
        when (pendingImport) {
            ImportKind.SETTINGS -> importSettings(json)
            ImportKind.GROUPS -> confirmGroupImport(json)
        }
    }

    private fun importSettings(json: String) {
        val next = SymbolDrawerSettingsCodec.decodeStrict(json)
        repository.save(next)
        current = next.copy(themeMode = ThemeMode.SYSTEM)
        render()
        onSettingsChanged?.invoke(current)
        toast("设置备份已校验并导入")
    }

    private fun confirmGroupImport(json: String) {
        // Decode and validate before asking for confirmation. Invalid files never
        // reach the repository and cannot partially overwrite the current data.
        val imported = SymbolGroupBundleCodec.decode(json)
        AlertDialog.Builder(context)
            .setTitle("确认替换符号组")
            .setMessage("已校验 ${imported.size} 个分组。确定替换当前全部符号组吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("导入") { _, _ ->
                runCatching {
                    groupRepository.saveGroups(imported)
                    onGroupsChanged?.invoke(imported)
                    toast("符号组已导入")
                }.onFailure(::reportFailure)
            }
            .show()
    }

    private fun update(transform: SymbolDrawerSettings.() -> SymbolDrawerSettings) {
        runCatching {
            val next = current.transform().copy(themeMode = ThemeMode.SYSTEM)
            // SharedPreferences commit() is synchronous; return only after the
            // local persistent snapshot has been written successfully.
            repository.save(next)
            current = next
            onSettingsChanged?.invoke(next)
        }.onFailure(::reportFailure)
    }

    private fun label(value: String) = TextView(context).apply {
        text = value
        textSize = 14f
        setTextColor(onSurfaceColor())
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun reportFailure(error: Throwable) {
        onError?.invoke(error)
        toast(error.message ?: "操作失败")
    }

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    private fun surfaceColor() = HostThemeColors.surface(context)
    private fun onSurfaceColor() = HostThemeColors.onSurface(context)
    private fun primaryColor() = HostThemeColors.primary(context)

    private fun surfaceBackground(fill: Int, strokeAlpha: Float, radius: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), (HostThemeColors.outline(context) and 0x00ffffff) or
            ((strokeAlpha.coerceIn(0f, 1f) * 255).toInt() shl 24))
    }

    private fun matchWrap() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private enum class ImportKind { SETTINGS, GROUPS }
}
