package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.app.AlertDialog
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import android.text.InputType
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.FrameLayout
import com.google.android.material.tabs.TabLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerManagerScreen

/**
 * First-party programmatic settings screen. It can be embedded by any host
 * Activity without imposing a navigation or Activity theme dependency.
 */
class SymbolDrawerSettingsScreen(
    context: Context,
    private val repository: SymbolDrawerSettingsRepository =
        SharedPreferencesSymbolDrawerSettingsRepository(context),
    groupRepository: SymbolGroupRepository = SharedPreferencesSymbolGroupRepository(context)
) : LinearLayout(context) {

    var onSettingsChanged: ((SymbolDrawerSettings) -> Unit)? = null
    var onGroupsChanged: ((List<com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup>) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    private var current = repository.load().settings
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private var activePage: LinearLayout? = null
    private val symbolManager = SymbolDrawerManagerScreen(context, groupRepository, embeddedInSettings = true).apply {
        onError = { this@SymbolDrawerSettingsScreen.onError?.invoke(it) }
        onGroupsChanged = { this@SymbolDrawerSettingsScreen.onGroupsChanged?.invoke(it) }
    }

    init {
        orientation = VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(8))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        render()
    }

    fun reload() {
        current = repository.load().settings
        render()
    }

    fun getSettings(): SymbolDrawerSettings = current

    private fun render() {
        content.removeAllViews()
        activePage = null

        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = surfaceBackground(surfaceColor(), 0.10f, 22)
            addView(TextView(context).apply {
                text = "符号抽屉设置"
                textSize = 23f
                setTextColor(primaryColor())
                setTypeface(typeface, Typeface.BOLD)
            }, matchWrap())
            addView(TextView(context).apply {
                text = "按类别管理外观、布局、交互与符号动作"
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
                addChoice("主题模式", ThemeMode.values().toList(), current.themeMode) { update { copy(themeMode = it) } }
                addChoice("分组指示器样式", GroupIndicatorStyle.values().toList(), current.indicatorStyle) { update { copy(indicatorStyle = it) } }
                addSwitch("显示分组名称", current.showGroupTitles) { update { copy(showGroupTitles = it) } }
                addSwitch("显示符号标签", current.showSymbolLabels) { update { copy(showSymbolLabels = it) } }
                addSwitch("显示动作提示", current.showActionHints) { update { copy(showActionHints = it) } }
            },
            "布局" to {
                section("尺寸与间距")
                addSeek("每行列数", current.columns, 1, 20) { update { copy(columns = it) } }
                addSeek("折叠时显示行数", current.collapsedRows, 1, 10) { update { copy(collapsedRows = it) } }
                addSeek("单元格高度 (dp)", current.cellHeightDp, 20, 96) { update { copy(cellHeightDp = it) } }
                addSeek("横向间距 (dp)", current.cellHorizontalSpacingDp, 0, 24) { update { copy(cellHorizontalSpacingDp = it) } }
                addSeek("纵向间距 (dp)", current.cellVerticalSpacingDp, 0, 24) { update { copy(cellVerticalSpacingDp = it) } }
                addSeek("分组栏高度 (dp)", current.indicatorHeightDp, 0, 64) { update { copy(indicatorHeightDp = it) } }
                addSeek("最大展开高度 (dp)", current.maxExpandedHeightDp, 120, 1200) { update { copy(maxExpandedHeightDp = it) } }
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
                addSwitch("搜索时包含动作内容", current.searchIncludesActionPayload) { update { copy(searchIncludesActionPayload = it) } }
            },
            "数据管理" to {
                section("设置备份")
                val backupRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                backupRow.addView(Button(context).apply {
                    text = "复制设置备份"
                    setOnClickListener {
                        runCatching {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Zero Symbol Drawer settings", SymbolDrawerSettingsCodec.encode(current)))
                            Toast.makeText(context, "设置备份 JSON 已复制", Toast.LENGTH_SHORT).show()
                        }.onFailure { onError?.invoke(it) }
                    }
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                backupRow.addView(Button(context).apply {
                    text = "导入设置备份"
                    setOnClickListener { promptImportBackup() }
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                activePage?.addView(backupRow, matchWrap())
                activePage?.addView(Button(context).apply {
                    text = "恢复默认设置"
                    setOnClickListener {
                        runCatching {
                            repository.reset()
                            current = SymbolDrawerSettings()
                            render()
                            onSettingsChanged?.invoke(current)
                        }.onFailure { onError?.invoke(it) }
                    }
                }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(18)
                })
            },
            "符号管理" to {
                section("符号列表与动作")
                activePage?.addView(TextView(context).apply {
                    text = "每个符号独立设置短按与长按行为。动作编号和数据格式兼容第一代。"
                    textSize = 13f
                    setTextColor(onSurfaceColor())
                    alpha = 0.72f
                    setPadding(dp(2), 0, dp(2), dp(10))
                }, matchWrap())
                if (symbolManager.parent is android.view.ViewGroup) {
                    (symbolManager.parent as android.view.ViewGroup).removeView(symbolManager)
                }
                activePage?.addView(symbolManager, matchWrap())
            }
        )

        builders.forEachIndexed { index, entry ->
            tabs.addTab(tabs.newTab().setText(entry.first), index == 0)
            val page = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(8), dp(16), dp(28))
            }
            val scroll = ScrollView(context).apply {
                isFillViewport = true
                addView(page, ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
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
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
    }

    private fun <T : Enum<T>> addChoice(title: String, values: List<T>, selected: T, changed: (T) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = surfaceBackground(surfaceColor(), 0.08f, 14)
        }
        row.addView(label(title), matchWrap())
        val spinner = Spinner(context)
        var initialSelection = true
        spinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            values.map { it.name.lowercase().replace('_', ' ') }
        )
        spinner.setSelection(values.indexOf(selected).coerceAtLeast(0), false)
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (initialSelection) {
                    initialSelection = false
                    return
                }
                values.getOrNull(position)?.let(changed)
            }
        }
        row.addView(spinner, matchWrap())
        activePage?.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
    }

    private fun addSeek(title: String, value: Int, minimum: Int, maximum: Int, changed: (Int) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))
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
        activePage?.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
    }

    private fun promptImportBackup() {
        val input = EditText(context).apply {
            hint = "粘贴设置备份 JSON"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 6
            gravity = Gravity.TOP
        }
        AlertDialog.Builder(context)
            .setTitle("导入符号抽屉设置")
            .setMessage("导入会覆盖当前设置，符号分组和符号数据不会改变。")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("导入") { _, _ ->
                runCatching {
                    val next = SymbolDrawerSettingsCodec.decode(input.text.toString())
                    repository.save(next)
                    current = next
                    render()
                    onSettingsChanged?.invoke(next)
                    Toast.makeText(context, "设置已导入", Toast.LENGTH_SHORT).show()
                }.onFailure { onError?.invoke(it) }
            }
            .show()
    }

    private fun update(transform: SymbolDrawerSettings.() -> SymbolDrawerSettings) {
        runCatching {
            val next = current.transform()
            repository.save(next)
            current = next
            onSettingsChanged?.invoke(next)
        }.onFailure { onError?.invoke(it) }
    }

    private fun label(value: String) = TextView(context).apply {
        text = value
        textSize = 14f
        setTextColor(onSurfaceColor())
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun themeColor(attribute: Int, fallback: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) runCatching { resources.getColor(value.resourceId, context.theme) }.getOrDefault(fallback)
            else value.data
        } else fallback
    }

    private fun surfaceColor() = themeColor(android.R.attr.colorBackground, Color.rgb(248, 248, 250))
    private fun onSurfaceColor() = themeColor(android.R.attr.textColorPrimary, Color.rgb(28, 28, 30))
    private fun primaryColor() = themeColor(android.R.attr.colorAccent, Color.rgb(103, 80, 164))

    private fun surfaceBackground(fill: Int, strokeAlpha: Float, radius: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.argb((strokeAlpha.coerceIn(0f, 1f) * 255).toInt(), 96, 96, 110))
    }

    private fun matchWrap() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
