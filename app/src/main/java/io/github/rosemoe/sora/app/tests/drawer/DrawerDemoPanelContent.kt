package io.github.rosemoe.sora.app.tests.drawer

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import kotlin.math.min

internal class DrawerDemoPlayerPanelContent(
    private val context: Context,
    private val controller: DrawerDemoPlayerController,
    private val compact: Boolean
) : DrawerPanelContent {
    private var subscription: AutoCloseable? = null
    private var root: View? = null
    private var disc: VinylDiscView? = null
    private var discAnimator: ObjectAnimator? = null
    private var title: TextView? = null
    private var subtitle: TextView? = null
    private var status: TextView? = null
    private var playButton: MaterialButton? = null
    private var seekBar: SeekBar? = null
    private var elapsed: TextView? = null
    private var duration: TextView? = null
    private var userSeeking = false

    override fun createView(): View = createView(context)

    override fun createView(context: Context): View {
        val surface = MaterialCardView(context).apply {
            radius = dp(context, if (compact) 16 else 24).toFloat()
            cardElevation = dp(context, 1).toFloat()
            setCardBackgroundColor(0xFF202532.toInt())
            setContentPadding(
                dp(context, if (compact) 10 else 18),
                dp(context, if (compact) 8 else 18),
                dp(context, if (compact) 10 else 18),
                dp(context, if (compact) 8 else 18)
            )
        }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (compact) Gravity.CENTER_VERTICAL else Gravity.CENTER_HORIZONTAL
        }
        if (compact) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            disc = VinylDiscView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 48), dp(context, 48))
            }
            val textColumn = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(context, 10), 0, dp(context, 6), 0)
            }
            title = TextView(context).apply {
                textSize = 14f
                maxLines = 1
                setTextColor(0xFFFFFFFF.toInt())
            }
            subtitle = TextView(context).apply {
                textSize = 11f
                maxLines = 1
                setTextColor(0xFFB8C0D0.toInt())
            }
            textColumn.addView(title, LinearLayout.LayoutParams(-1, -2))
            textColumn.addView(subtitle, LinearLayout.LayoutParams(-1, -2))
            row.addView(disc)
            row.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(controlButton(context, "⏮", "上一曲") { controller.previousTrack() })
            playButton = controlButton(context, "▶", "播放或暂停") { controller.togglePlayback() }
            row.addView(playButton)
            row.addView(controlButton(context, "⏭", "下一曲") { controller.nextTrack() })
            column.addView(row, LinearLayout.LayoutParams(-1, -2))
            status = TextView(context).apply {
                textSize = 10f
                setTextColor(0xFF9EA9BC.toInt())
                setPadding(0, dp(context, 4), 0, 0)
            }
            column.addView(status)
        } else {
            disc = VinylDiscView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 184), dp(context, 184)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = dp(context, 16)
                }
            }
            title = TextView(context).apply {
                textSize = 21f
                maxLines = 1
                gravity = Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt())
            }
            subtitle = TextView(context).apply {
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(0xFFB8C0D0.toInt())
            }
            column.addView(disc)
            column.addView(title, LinearLayout.LayoutParams(-1, -2))
            column.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(context, 4)
            })
            seekBar = SeekBar(context).apply {
                max = 1000
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            val total = controller.state.durationMs
                            if (total > 0) controller.seekTo(total * progress / 1000)
                        }
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) { userSeeking = true }
                    override fun onStopTrackingTouch(bar: SeekBar?) { userSeeking = false }
                })
            }
            column.addView(seekBar, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(context, 14)
            })
            val timeRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            elapsed = timeLabel(context)
            duration = timeLabel(context).apply { gravity = Gravity.END }
            timeRow.addView(elapsed, LinearLayout.LayoutParams(0, -2, 1f))
            timeRow.addView(duration, LinearLayout.LayoutParams(0, -2, 1f))
            column.addView(timeRow)
            val controls = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(context, 12), 0, 0)
            }
            controls.addView(controlButton(context, "上一曲", "上一曲") { controller.previousTrack() })
            playButton = controlButton(context, "播放", "播放或暂停") { controller.togglePlayback() }
            controls.addView(playButton)
            controls.addView(controlButton(context, "下一曲", "下一曲") { controller.nextTrack() })
            column.addView(controls, LinearLayout.LayoutParams(-1, -2))
            status = TextView(context).apply {
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(0xFFB8C0D0.toInt())
                setPadding(0, dp(context, 10), 0, 0)
            }
            column.addView(status, LinearLayout.LayoutParams(-1, -2))
        }
        surface.addView(column)
        root = if (compact) surface else NestedScrollView(context).apply {
            isFillViewport = true
            addView(surface)
        }
        subscription = controller.addListener(::render)
        render(controller.state)
        return root!!
    }

    private fun render(state: DrawerDemoPlayerState) {
        val track = controller.currentTrack()
        title?.text = track.title
        subtitle?.text = track.subtitle
        playButton?.text = if (state.isPlaying) {
            if (compact) "Ⅱ" else "暂停"
        } else {
            if (compact) "▶" else "播放"
        }
        status?.text = when {
            state.error != null -> state.error
            state.isPreparing -> "正在下载并准备纯音乐…"
            state.isPlaying -> "正在播放 · 下拉手柄可切换迷你播放器"
            else -> "已暂停 · 可使用上一曲、播放、下一曲"
        }
        if (!userSeeking) {
            val total = state.durationMs.coerceAtLeast(0)
            val current = state.positionMs.coerceIn(0, total)
            seekBar?.progress = if (total > 0) (current * 1000L / total).toInt() else 0
        }
        elapsed?.text = formatTime(state.positionMs)
        duration?.text = formatTime(state.durationMs)
        disc?.setTrackIndex(state.trackIndex)
        val animator = discAnimator
        if (state.isPlaying) {
            if (animator == null) {
                disc?.let { vinyl ->
                    discAnimator = ObjectAnimator.ofFloat(vinyl, "rotation", 0f, 360f).apply {
                        duration = 14_000L
                        repeatCount = ValueAnimator.INFINITE
                        interpolator = LinearInterpolator()
                        start()
                    }
                }
            } else if (!animator.isStarted) animator.start()
            else if (!animator.isRunning) animator.resume()
        } else animator?.pause()
    }

    override fun onPanelVisible() {
        if (controller.state.isPlaying) discAnimator?.let {
            if (!it.isStarted) it.start() else if (!it.isRunning) it.resume()
        }
    }

    override fun onPanelHidden() { discAnimator?.pause() }

    override fun onPanelDetached() {
        subscription?.close()
        subscription = null
        discAnimator?.cancel()
        discAnimator = null
        root = null
    }

    private fun controlButton(
        context: Context,
        text: String,
        description: String,
        action: () -> Unit
    ) = MaterialButton(context).apply {
        this.text = text
        contentDescription = description
        isAllCaps = false
        minWidth = dp(context, if (compact) 36 else 84)
        minHeight = dp(context, 44)
        setPadding(dp(context, 6), dp(context, 2), dp(context, 6), dp(context, 2))
        setOnClickListener { action() }
    }

    private fun timeLabel(context: Context) = TextView(context).apply {
        textSize = 11f
        setTextColor(0xFFB8C0D0.toInt())
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds.coerceAtLeast(0) / 1000
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}

internal class VinylDiscView(context: Context) : View(context) {
    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10131A.toInt() }
    private val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF343A47.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1).toFloat()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC5A36A.toInt() }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10131A.toInt() }
    private val labelColors = intArrayOf(0xFFC5A36A.toInt(), 0xFF6E9B9A.toInt(), 0xFF9C7EA8.toInt())
    private var trackIndex = 0

    fun setTrackIndex(value: Int) {
        if (trackIndex != value) {
            trackIndex = value
            labelPaint.color = labelColors[value.mod(labelColors.size)]
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = (min(width, height) / 2f - dp(3)).coerceAtLeast(1f)
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, radius, outerPaint)
        for (index in 1..7) canvas.drawCircle(cx, cy, radius * (0.42f + index * 0.075f), groovePaint)
        canvas.drawCircle(cx, cy, radius * 0.34f, labelPaint)
        canvas.drawCircle(cx, cy, radius * 0.055f, holePaint)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}

internal class DrawerDemoFilterPanelContent(
    private val context: Context,
    private val onApply: (String) -> Unit
) : DrawerPanelContent {
    override fun createView(): View = createView(context)

    override fun createView(context: Context): View {
        val scroll = NestedScrollView(context).apply { isFillViewport = true }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 18))
        }
        scroll.addView(root)
        root.addView(section(context, "内容类型"))
        val categoryGroup = ChipGroup(context).apply {
            isSingleSelection = false
            isSelectionRequired = false
            chipSpacingHorizontal = dp(context, 6)
            chipSpacingVertical = dp(context, 4)
        }
        listOf("图片", "视频", "音频", "文档", "压缩包").forEachIndexed { index, label ->
            categoryGroup.addView(Chip(context).apply {
                text = label
                isCheckable = true
                isChecked = index == 0 || index == 3
            })
        }
        root.addView(categoryGroup)
        root.addView(section(context, "时间范围"))
        val dateRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf("今天", "近 7 天", "近 30 天").forEachIndexed { index, label ->
            dateRow.addView(MaterialButton(context).apply {
                text = label
                isAllCaps = false
                isCheckable = true
                isChecked = index == 1
                minWidth = 0
                setOnClickListener {
                    dateRow.childrenButtons().forEach { it.isChecked = it === this }
                }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(context, 4) })
        }
        root.addView(dateRow)
        root.addView(section(context, "排序方式"))
        val sortGroup = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL }
        listOf("最近修改", "名称", "文件大小").forEachIndexed { index, label ->
            sortGroup.addView(RadioButton(context).apply {
                id = View.generateViewId()
                text = label
                isChecked = index == 0
            })
        }
        root.addView(sortGroup)
        root.addView(section(context, "更多条件"))
        val switches = listOf(
            SwitchMaterial(context).apply { text = "仅显示已下载"; isChecked = false },
            SwitchMaterial(context).apply { text = "包含子目录"; isChecked = true },
            SwitchMaterial(context).apply { text = "隐藏临时文件"; isChecked = true }
        )
        switches.forEach { root.addView(it) }
        root.addView(TextView(context).apply {
            text = "筛选条件只影响本示例页面，不会读取或修改编辑器工程文件。"
            textSize = 12f
            setTextColor(0xFF747D8D.toInt())
            setPadding(0, dp(context, 12), 0, dp(context, 8))
        })
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        actions.addView(MaterialButton(context).apply {
            text = "重置"
            isAllCaps = false
            setOnClickListener {
                categoryGroup.clearCheck()
                sortGroup.check(sortGroup.getChildAt(0).id)
                switches.forEach { it.isChecked = false }
                dateRow.childrenButtons().forEachIndexed { index, button -> button.isChecked = index == 1 }
            }
        })
        actions.addView(MaterialButton(context).apply {
            text = "应用筛选"
            isAllCaps = false
            setOnClickListener {
                val selected = (0 until categoryGroup.childCount)
                    .map { categoryGroup.getChildAt(it) as Chip }
                    .filter { it.isChecked }
                    .map { it.text.toString() }
                val sort = sortGroup.findViewById<RadioButton>(sortGroup.checkedRadioButtonId)?.text?.toString().orEmpty()
                val date = dateRow.childrenButtons().firstOrNull { it.isChecked }?.text?.toString().orEmpty()
                val enabled = switches.filter { it.isChecked }.map { it.text.toString() }
                val summary = buildList {
                    if (selected.isNotEmpty()) add(selected.joinToString("、"))
                    if (date.isNotEmpty()) add(date)
                    if (sort.isNotEmpty()) add("按" + sort)
                    if (enabled.isNotEmpty()) add(enabled.joinToString("、"))
                }.joinToString(" · ").ifBlank { "全部内容" }
                onApply(summary)
            }
        })
        root.addView(actions)
        return scroll
    }

    private fun section(context: Context, title: String) = TextView(context).apply {
        text = title
        textSize = 15f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(0xFF222A38.toInt())
        setPadding(0, dp(context, 14), 0, dp(context, 6))
    }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}

internal class DrawerDemoFilterSummaryContent(
    private val context: Context,
    private val summary: () -> String
) : DrawerPanelContent {
    private var label: TextView? = null

    override fun createView(): View = createView(context)

    override fun createView(context: Context): View {
        label = TextView(context).apply {
            textSize = 13f
            setTextColor(0xFFE9EDF5.toInt())
            setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
            text = summary()
        }
        return label!!
    }

    override fun onPanelVisible() {
        label?.text = "当前条件：" + summary()
    }

    override fun onPanelDetached() { label = null }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}

private fun LinearLayout.childrenButtons(): List<MaterialButton> =
    (0 until childCount).mapNotNull { getChildAt(it) as? MaterialButton }
