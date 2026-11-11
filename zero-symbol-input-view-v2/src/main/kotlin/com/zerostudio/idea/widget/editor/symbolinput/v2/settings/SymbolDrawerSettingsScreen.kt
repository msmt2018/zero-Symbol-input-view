package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
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
import android.widget.TextView

/**
 * First-party programmatic settings screen. It can be embedded by any host
 * Activity without imposing a navigation or Activity theme dependency.
 */
class SymbolDrawerSettingsScreen(
    context: Context,
    private val repository: SymbolDrawerSettingsRepository =
        SharedPreferencesSymbolDrawerSettingsRepository(context)
) : ScrollView(context) {

    var onSettingsChanged: ((SymbolDrawerSettings) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    private var current = repository.load().settings
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(16), dp(20), dp(32))
    }

    init {
        isFillViewport = true
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        render()
    }

    fun reload() {
        current = repository.load().settings
        render()
    }

    fun getSettings(): SymbolDrawerSettings = current

    private fun render() {
        content.removeAllViews()
        content.addView(TextView(context).apply {
            text = "符号抽屉设置"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        }, matchWrap())
        content.addView(TextView(context).apply {
            text = "设置会立即保存。宿主收到变更后可将新配置应用到抽屉。"
            textSize = 13f
            alpha = 0.72f
            setPadding(0, dp(6), 0, dp(16))
        }, matchWrap())

        section("外观")
        addChoice("主题模式", ThemeMode.values().toList(), current.themeMode) { update { copy(themeMode = it) } }
        addChoice("分组指示器样式", GroupIndicatorStyle.values().toList(), current.indicatorStyle) { update { copy(indicatorStyle = it) } }
        addSwitch("显示分组名称", current.showGroupTitles) { update { copy(showGroupTitles = it) } }
        addSwitch("显示符号标签", current.showSymbolLabels) { update { copy(showSymbolLabels = it) } }
        addSwitch("显示动作提示", current.showActionHints) { update { copy(showActionHints = it) } }

        section("布局")
        addSeek("每行列数", current.columns, 1, 20) { update { copy(columns = it) } }
        addSeek("折叠时显示行数", current.collapsedRows, 1, 10) { update { copy(collapsedRows = it) } }
        addSeek("单元格高度 (dp)", current.cellHeightDp, 20, 96) { update { copy(cellHeightDp = it) } }
        addSeek("横向间距 (dp)", current.cellHorizontalSpacingDp, 0, 24) { update { copy(cellHorizontalSpacingDp = it) } }
        addSeek("纵向间距 (dp)", current.cellVerticalSpacingDp, 0, 24) { update { copy(cellVerticalSpacingDp = it) } }
        addSeek("分组栏高度 (dp)", current.indicatorHeightDp, 0, 64) { update { copy(indicatorHeightDp = it) } }
        addSeek("最大展开高度 (dp)", current.maxExpandedHeightDp, 120, 1200) { update { copy(maxExpandedHeightDp = it) } }
        addSeek("最小触摸目标 (dp)", current.minimumTouchTargetDp, 32, 80) { update { copy(minimumTouchTargetDp = it) } }

        section("手势与动作")
        addSeek("长按触发时间 (ms)", current.longPressTimeoutMs, 250, 1000) { update { copy(longPressTimeoutMs = it) } }
        addSeek("展开甩动阈值 (dp/s)", current.flingVelocityDpPerSecond, 100, 6000) { update { copy(flingVelocityDpPerSecond = it) } }
        addSwitch("执行动作后自动收起", current.autoCollapseAfterAction) { update { copy(autoCollapseAfterAction = it) } }

        section("动画与触觉")
        addSeek("动画时长 (ms)", current.animationDurationMs, 0, 1200) { update { copy(animationDurationMs = it) } }
        addSwitch("减少动态效果", current.reduceMotion) { update { copy(reduceMotion = it) } }
        addChoice("触觉反馈", HapticMode.values().toList(), current.hapticMode) { update { copy(hapticMode = it) } }

        section("性能")
        addSeek("页面缓存数量", current.pageCacheLimit, 1, 9) { update { copy(pageCacheLimit = it) } }
        addSwitch("预加载相邻分组", current.prefetchAdjacentGroups) { update { copy(prefetchAdjacentGroups = it) } }

        section("搜索")
        addSwitch("搜索时包含动作内容", current.searchIncludesActionPayload) { update { copy(searchIncludesActionPayload = it) } }

        section("数据与备份")
        val backupRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        backupRow.addView(Button(context).apply {
            text = "复制设置备份"
            setOnClickListener {
                runCatching {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("Zero Symbol Drawer settings", SymbolDrawerSettingsCodec.encode(current))
                    )
                    Toast.makeText(context, "设置备份 JSON 已复制", Toast.LENGTH_SHORT).show()
                }.onFailure { onError?.invoke(it) }
            }
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        backupRow.addView(Button(context).apply {
            text = "导入设置备份"
            setOnClickListener { promptImportBackup() }
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        content.addView(backupRow, matchWrap())

        content.addView(Button(context).apply {
            text = "恢复默认设置"
            setOnClickListener {
                runCatching {
                    repository.reset()
                    current = SymbolDrawerSettings()
                    render()
                    onSettingsChanged?.invoke(current)
                }.onFailure { onError?.invoke(it) }
            }
        }, matchWrap())
    }

    private fun section(title: String) {
        content.addView(TextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(8))
        }, matchWrap())
    }

    private fun addSwitch(title: String, checked: Boolean, changed: (Boolean) -> Unit) {
        content.addView(Switch(context).apply {
            text = title
            isChecked = checked
            textSize = 14f
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, value -> changed(value) }
        }, matchWrap())
    }

    private fun <T : Enum<T>> addChoice(title: String, values: List<T>, selected: T, changed: (T) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))
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
        content.addView(row, matchWrap())
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
        content.addView(row, matchWrap())
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
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun matchWrap() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
