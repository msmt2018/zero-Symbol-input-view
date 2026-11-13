package io.github.rosemoe.sora.app.tests.drawer

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.DrawerContainer
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchor
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorState
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorStrategies
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationPlacement
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerIndicatorShape
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.preset.DrawerPreset
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.preset.DrawerPresets
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerDimensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerMotion
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerShape
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerTypography
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeMode

/** Interactive playground for the reusable, business-neutral drawer APIs. */
class DrawerApiLabActivity : AppCompatActivity() {

    private lateinit var drawer: DrawerContainer
    private lateinit var status: TextView
    private lateinit var playerController: DrawerDemoPlayerController
    private var demoDimensions = DrawerDimensions()
    private var filterSummary = "全部内容"
    private var customPalette = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val standard = DrawerPresets.config(DrawerPreset.STANDARD)
        demoDimensions = standard.style.dimensions
        drawer = DrawerContainer(this, standard.copy(peekContent = null, expandedContent = null))
        playerController = DrawerDemoPlayerController(this) { playerState ->
            if (::status.isInitialized) {
                status.text = "播放器：" +
                    (playerState.error ?: if (playerState.isPreparing) "准备中" else if (playerState.isPlaying) "播放中" else "暂停")
            }
        }
        setContentView(drawer)
        drawer.setTitle("")
        drawer.setMainContent(buildDashboard())
        // Start with only the real handle visible. Each scenario supplies its own panels.
        drawer.setPeekContent(null)
        drawer.controller.setExpandedContent(null)
        drawer.controller.setDragHandleEnabled(true)
        drawer.controller.setDragEnabled(true)
        drawer.controller.setPages(samplePages())
        drawer.controller.addStateListener { state ->
            if (::status.isInitialized) {
                status.text = "当前锚点：" + state.currentAnchorId +
                    " · 当前高度：" + state.currentHeightPx + "px" +
                    "\n拖动：" + state.isDragging + " · 动画：" + state.isAnimating
            }
        }
    }

    override fun onBackPressed() {
        if (!::drawer.isInitialized || !drawer.handleBack()) super.onBackPressed()
    }

    override fun onDestroy() {
        if (::playerController.isInitialized) playerController.close()
        super.onDestroy()
    }

    private fun buildDashboard(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(120))
        }
        scroll.addView(root)
        root.addView(TextView(this).apply {
            text = "公共抽屉 API 实验室"
            textSize = 25f
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
        })
        root.addView(TextView(this).apply {
            text = "在此页面实时修改锚点、主题、布局、导航、面板和手势参数。底部抽屉可拖动，也可使用下方控制按钮。"
            textSize = 14f
            setPadding(0, dp(8), 0, dp(16))
        })
        root.addView(section("锚点与布局"))
        root.addView(button("两档高度 · Peek / Expanded") {
            configureBottomSheet()
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.existingHeight())
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("expanded", false)
        })
        root.addView(button("三段式 · Peek / Half / Expanded") {
            configureBottomSheet()
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("half", false)
        })
        root.addView(button("三段式 · 逐级展开至 Expanded") {
            configureBottomSheet()
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("expanded", true)
        })
        root.addView(button("内容自适应 · 短内容") {
            configureBottomSheet()
            drawer.controller.setPages(emptyList())
            drawer.setPeekContent(objectPanel("内容自适应", "折叠时显示简短摘要。", 2))
            drawer.controller.setExpandedContent(objectPanel("内容自适应", "内容较少时抽屉按实际内容高度收缩。", 3))
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.contentBased())
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("content", true)
        })
        root.addView(button("内容自适应 · 长内容") {
            configureBottomSheet()
            drawer.controller.setPages(emptyList())
            drawer.setPeekContent(objectPanel("内容自适应", "折叠时显示简短摘要。", 2))
            drawer.controller.setExpandedContent(objectPanel("内容自适应", "增加内容后重新测量，锚点随内容高度变化。", 14))
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.contentBased())
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("content", true)
        })
        root.addView(button("自定义四锚点 · 真实容器尺寸") {
            configureBottomSheet()
            val maxDp = (drawer.height / resources.displayMetrics.density).toInt()
                .coerceAtLeast(360).coerceAtMost(720)
            updateDimensions { it.copy(peekHeightDp = 64f, expandedMaxHeightDp = maxDp.toFloat()) }
            val expanded = dp(maxDp)
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.custom(listOf(
                DrawerAnchor("peek", dp(64), DrawerAnchorState.PEEK),
                DrawerAnchor("compact", (expanded * 0.34f).toInt(), DrawerAnchorState.CUSTOM),
                DrawerAnchor("half", (expanded * 0.62f).toInt(), DrawerAnchorState.HALF_EXPANDED),
                DrawerAnchor("expanded", expanded, DrawerAnchorState.EXPANDED)
            )))
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("compact", false)
        })
        root.addView(button("全屏三段式 · Peek / Half / Full") {
            configureBottomSheet()
            drawer.controller.setPresentationMode(DrawerPresentationMode.FULL_SCREEN)
            updateDimensions { it.copy(peekHeightDp = 64f, expandedMaxHeightDp = 720f) }
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("expanded", true)
        })
        root.addView(button("全屏三段式 · 回到 Half") {
            drawer.controller.moveToAnchor("half", true)
        })
        root.addView(button("全屏三段式 · 回到 Peek") {
            drawer.controller.moveToAnchor("peek", true)
        })
        root.addView(button("退出全屏并恢复底部抽屉") {
            configureBottomSheet()
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("peek", false)
        })
        root.addView(section("底部抽屉业务场景"))
        root.addView(button("播放器抽屉 · 黑胶唱片 / 上一曲 / 播放 / 下一曲") {
            configureBottomSheet()
            drawer.setTitle("Now Playing")
            drawer.controller.setPages(emptyList())
            drawer.setPeekContent(DrawerDemoPlayerPanelContent(this, playerController, compact = true))
            drawer.controller.setExpandedContent(DrawerDemoPlayerPanelContent(this, playerController, compact = false))
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
            drawer.controller.setDragHandleEnabled(true)
            drawer.controller.setDragEnabled(true)
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("expanded", true)
            playerController.play()
        })
        root.addView(button("筛选抽屉 · 独立筛选表单") {
            configureBottomSheet()
            drawer.setTitle("筛选条件")
            drawer.controller.setPages(emptyList())
            filterSummary = "全部内容"
            drawer.setPeekContent(DrawerDemoFilterSummaryContent(this) { filterSummary })
            drawer.controller.setExpandedContent(DrawerDemoFilterPanelContent(this) { summary ->
                filterSummary = summary
                drawer.collapse()
            })
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.contentBased())
            drawer.controller.setDragHandleEnabled(true)
            drawer.controller.setDragEnabled(true)
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("content", true)
        })
        root.addView(button("快捷操作抽屉 · 页面导航") {
            configureBottomSheet()
            drawer.setTitle("快捷操作")
            drawer.setPeekContent(objectPanel("常用操作", "向上展开查看全部工具", 2))
            drawer.controller.setExpandedContent(null)
            drawer.controller.setPages(samplePages())
            drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
            drawer.refreshAnchors()
            drawer.controller.moveToAnchor("expanded", true)
        })
        root.addView(section("预设样式"))
        DrawerPreset.values().forEach { preset ->
            root.addView(button("应用预设：$preset") {
                val presetConfig = DrawerPresets.config(preset)
                demoDimensions = presetConfig.style.dimensions
                drawer.controller.applyConfig(presetConfig.copy(
                    peekContent = objectPanel("Peek · $preset", "Preset preview"),
                    expandedContent = null
                ))
                drawer.controller.setPages(samplePages())
                drawer.setTitle("Preset · $preset")
                if (preset == DrawerPreset.FULL_SCREEN) drawer.expand()
            })
        }
        root.addView(section("主题与颜色"))
        root.addView(button("跟随宿主 Activity 主题") { drawer.controller.setThemeMode(DrawerThemeMode.FOLLOW_HOST) })
        root.addView(button("浅色主题") { drawer.controller.setThemeMode(DrawerThemeMode.LIGHT) })
        root.addView(button("深色主题") { drawer.controller.setThemeMode(DrawerThemeMode.DARK) })
        root.addView(button("切换自定义配色") {
            customPalette = !customPalette
            drawer.controller.setColors(if (customPalette) DrawerColors(
                surface = Color.rgb(28, 34, 48), primaryText = Color.WHITE,
                secondaryText = Color.LTGRAY, accent = Color.rgb(130, 170, 255),
                outline = Color.rgb(85, 105, 145), scrim = 0x99000000.toInt()
            ) else DrawerColors())
        })
        root.addView(section("尺寸与动画"))
        root.addView(label("Peek 高度"))
        root.addView(slider(40f, 120f, 56f) { value ->
            updateDimensions { it.copy(peekHeightDp = value) }
        })
        root.addView(label("最大展开高度"))
        root.addView(slider(320f, 900f, 640f) { value ->
            updateDimensions { it.copy(expandedMaxHeightDp = value.coerceAtLeast(it.peekHeightDp)) }
        })
        root.addView(label("拖动手柄宽度"))
        root.addView(slider(20f, 72f, 36f) { value ->
            updateDimensions { it.copy(handleWidthDp = value) }
        })
        root.addView(label("拖动手柄高度"))
        root.addView(slider(2f, 14f, 4f) { value ->
            updateDimensions { it.copy(handleHeightDp = value) }
        })
        root.addView(label("拖动阻尼"))
        root.addView(slider(0.2f, 1f, 0.72f) { value ->
            drawer.controller.setMotion(DrawerMotion(dragResistance = value))
        })
        root.addView(label("吸附动画时长"))
        root.addView(slider(0f, 600f, 260f) { value ->
            drawer.controller.setMotion(DrawerMotion(settleDurationMs = value.toLong()))
        })
        root.addView(label("抽屉顶部圆角"))
        root.addView(slider(0f, 40f, 24f) { value ->
            drawer.controller.setShape(DrawerShape(
                topStartRadiusDp = value, topEndRadiusDp = value
            ))
        })
        root.addView(label("标题字号"))
        root.addView(slider(14f, 28f, 18f) { value ->
            drawer.controller.setTypography(DrawerTypography(titleSizeSp = value))
        })
        root.addView(section("交互开关"))
        root.addView(toggle("允许拖动", true) { drawer.controller.setDragEnabled(it) })
        root.addView(toggle("允许嵌套滚动", true) { drawer.controller.setNestedScrollEnabled(it) })
        root.addView(toggle("点击遮罩折叠", true) { drawer.controller.setDismissOnScrimTap(it) })
        root.addView(toggle("显示拖动手柄", true) { drawer.controller.setDragHandleEnabled(it) })
        root.addView(toggle("无障碍支持", true) { drawer.controller.setAccessibilityEnabled(it) })
        root.addView(section("页面与面板"))
        root.addView(button("导航栏置顶") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(placement = DrawerNavigationPlacement.TOP))
        })
        root.addView(button("导航栏置底") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(placement = DrawerNavigationPlacement.BOTTOM))
        })
        root.addView(button("隐藏导航栏") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(placement = DrawerNavigationPlacement.HIDDEN))
        })
        root.addView(button("胶囊形导航指示器") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(
                indicator = DrawerIndicatorStyle(shape = DrawerIndicatorShape.PILL, heightDp = 4f, widthFraction = 0.72f)
            ))
        })
        root.addView(button("恢复下划线导航指示器") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig())
        })
        root.addView(button("图标 + 标题") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(showIcons = true, showTitles = true))
            drawer.controller.setPages(samplePages())
        })
        root.addView(button("仅图标导航") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(showIcons = true, showTitles = false))
            drawer.controller.setPages(samplePages())
        })
        root.addView(button("仅标题导航") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(showIcons = false, showTitles = true))
            drawer.controller.setPages(samplePages())
        })
        root.addView(button("选中标题加粗") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(selectedTitleBold = true))
        })
        root.addView(button("折叠态也显示页面导航") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(showInPeekPanel = true))
            drawer.collapse()
        })
        root.addView(button("折叠态隐藏页面导航") {
            drawer.controller.setNavigationConfig(DrawerNavigationConfig(showInPeekPanel = false))
            drawer.collapse()
        })
        root.addView(button("恢复四个示例页面") { drawer.controller.setPages(samplePages()) })
        root.addView(button("只显示 Peek 面板") {
            drawer.controller.setExpandedContent(null)
            drawer.setPeekContent(objectPanel("Only Peek", "Expanded content is detached."))
            drawer.collapse()
        })
        root.addView(button("替换 Peek 面板内容") {
            drawer.setPeekContent(objectPanel("Replaced Peek", "The panel was replaced at runtime."))
        })
        root.addView(button("替换 Expanded 页面内容") {
            drawer.controller.setExpandedContent(objectPanel("Standalone Expanded", "No page navigation is required."))
            drawer.expand()
        })
        root.addView(button("恢复页面导航模式") {
            drawer.setPages(samplePages())
            drawer.expand()
        })
        root.addView(section("运行状态"))
        status = TextView(this).apply {
            text = "等待容器测量…"
            textSize = 12f
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        root.addView(status)
        root.addView(button("重置实验环境") {
            customPalette = false
            val standard = DrawerPresets.config(DrawerPreset.STANDARD)
            demoDimensions = standard.style.dimensions
            drawer.controller.applyConfig(standard.copy(peekContent = null, expandedContent = null))
            drawer.setTitle("")
            drawer.setPeekContent(null)
            drawer.controller.setExpandedContent(null)
            drawer.setPages(samplePages())
            drawer.collapse()
        })
        return scroll
    }


    private fun configureBottomSheet() {
        // Every demo starts from a known panel configuration. In particular,
        // content-sized examples must not inherit a previous scenario's large
        // Peek panel or expanded content.
        drawer.controller.setPresentationMode(DrawerPresentationMode.BOTTOM_SHEET)
        drawer.controller.setDragHandleEnabled(true)
        drawer.controller.setDragEnabled(true)
        drawer.setTitle("")
        drawer.setPeekContent(null)
        drawer.controller.setExpandedContent(null)
        drawer.controller.setPages(samplePages())
        drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
        val availableDp = (drawer.height / resources.displayMetrics.density).toInt().coerceAtLeast(360)
        updateDimensions {
            it.copy(
                peekHeightDp = 64f,
                expandedMaxHeightDp = availableDp.coerceAtMost(720).toFloat()
            )
        }
    }

    private fun updateDimensions(transform: (DrawerDimensions) -> DrawerDimensions) {
        demoDimensions = transform(demoDimensions)
        drawer.controller.setDimensions(demoDimensions)
    }

    private fun samplePages() = listOf(
        DrawerPage("overview", "Overview", content("Overview page", "Generic page hosting, lifecycle and page switching."),
            systemIcon(android.R.drawable.ic_menu_view)),
        DrawerPage("logs", "Logs", content("Logs page", "A simulated log stream demonstrates arbitrary page content."),
            systemIcon(android.R.drawable.ic_menu_recent_history)),
        DrawerPage("tools", "Tools", content("Tools page", "Independent tools and custom content."),
            systemIcon(android.R.drawable.ic_menu_manage)),
        DrawerPage("details", "Details", content("Details page", "Stable page IDs preserve selection across updates."),
            systemIcon(android.R.drawable.ic_menu_info_details))
    )

    private fun systemIcon(resourceId: Int) = resources.getDrawable(resourceId, theme)

    private fun content(title: String, description: String, rows: Int = 18) = object : DrawerPanelContent {
        override fun createView(): View = panel(this@DrawerApiLabActivity, title, description, rows)
        override fun createView(context: Context): View = panel(context, title, description, rows)
    }

    private fun objectPanel(title: String, description: String, rows: Int = 18) = content(title, description, rows)

    private fun panel(context: Context, title: String, description: String, rows: Int = 18): View {
        val card = MaterialCardView(context).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(1).toFloat()
            setContentPadding(dp(20), dp(18), dp(20), dp(18))
        }
        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply { text = title; textSize = 20f })
            addView(TextView(context).apply {
                text = (1..rows.coerceAtLeast(1)).joinToString("\n") { "$description\nScrollable content row $it" }
                textSize = 14f
                setPadding(0, dp(context, 10), 0, 0)
            })
        })
        return NestedScrollView(context).apply { addView(card) }
    }

    private fun section(text: String) = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(22), 0, dp(8))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setPadding(0, dp(8), 0, 0)
    }

    private fun button(text: String, action: () -> Unit) = MaterialButton(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) }
    }

    private fun slider(min: Float, max: Float, initial: Float, action: (Float) -> Unit) =
        Slider(this).apply {
            valueFrom = min
            valueTo = max
            value = initial.coerceIn(min, max)
            stepSize = if (max - min > 100f) 1f else 0f
            addOnChangeListener { _, value, fromUser -> if (fromUser) action(value) }
        }

    private fun toggle(text: String, checked: Boolean, action: (Boolean) -> Unit) =
        SwitchMaterial(this).apply {
            this.text = text
            isChecked = checked
            setOnCheckedChangeListener { _, value -> action(value) }
            setPadding(0, dp(4), 0, dp(4))
        }

    private fun resolveColor(attribute: Int, fallback: Int): Int {
        val value = android.util.TypedValue()
        return if (theme.resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) getColorCompat(value.resourceId) else value.data
        } else fallback
    }

    @Suppress("DEPRECATION")
    private fun getColorCompat(id: Int): Int = resources.getColor(id)

    private fun dp(value: Int) = dp(this, value)

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
