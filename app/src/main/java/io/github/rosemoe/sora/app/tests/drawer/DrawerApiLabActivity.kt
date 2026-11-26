/*
 * This file is part of Zero Symbol Input View.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.rosemoe.sora.app.tests.drawer

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.rememberDrawerContainerState
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.drawer.core.DrawerAnchorStrategies
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationConfig
import com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationPlacement
import com.zerostudio.idea.widget.drawer.panel.drawerPanelContent
import com.zerostudio.idea.widget.drawer.preset.DrawerPreset
import com.zerostudio.idea.widget.drawer.preset.DrawerPresets
import com.zerostudio.idea.widget.drawer.style.DrawerThemeMode

/** Compose-native playground for the public bottom-sheet and drawer APIs. */
class DrawerApiLabActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme { DrawerApiLabScreen() }
        }
    }
}

@Composable
private fun DrawerApiLabScreen() {
    val drawerState = rememberDrawerContainerState()
    var config by remember { mutableStateOf(DrawerPresets.config(DrawerPreset.STANDARD)) }
    var selectedPage by remember { mutableStateOf("overview") }
    var dragEnabled by remember { mutableStateOf(true) }
    var nestedScrollEnabled by remember { mutableStateOf(true) }
    var scrimEnabled by remember { mutableStateOf(true) }
    var peekHeight by remember { mutableFloatStateOf(config.style.dimensions.peekHeightDp) }
    var expandedHeight by remember { mutableFloatStateOf(config.style.dimensions.expandedMaxHeightDp) }
    var cornerRadius by remember { mutableFloatStateOf(config.style.shape.topStartRadiusDp) }
    var settleDuration by remember { mutableFloatStateOf(config.style.motion.settleDurationMs.toFloat()) }
    val pages = remember {
        listOf(
            DrawerPage("overview", "概览", drawerPanelContent {
                Text("Compose 抽屉概览", style = MaterialTheme.typography.titleLarge)
                Text("页面内容由 Composition 管理，可直接放置任意 Compose UI。")
            }),
            DrawerPage("logs", "日志", drawerPanelContent {
                Text("运行日志", style = MaterialTheme.typography.titleLarge)
                repeat(12) { index -> Text("[" + index + "] Drawer state / gesture / animation event") }
            }),
            DrawerPage("tools", "工具", drawerPanelContent {
                Text("工具面板", style = MaterialTheme.typography.titleLarge)
                Text("页面切换、拖动、嵌套滚动和状态配置均由 Compose 驱动。")
            })
        )
    }

    BackHandler(enabled = drawerState.expanded) { drawerState.collapse() }

    DrawerContainer(
        config = config,
        state = drawerState,
        title = "Compose Bottom Drawer",
        pages = pages,
        selectedPageId = selectedPage,
        onPageSelected = { selectedPage = it.id },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("公共抽屉 API 实验室", style = MaterialTheme.typography.headlineSmall)
            Text("纯 Compose 测试入口：实时调整锚点、手势、主题、尺寸、导航和底部面板。")
            Text("锚点与展开状态", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    config = config.copy(presentationMode = DrawerPresentationMode.BOTTOM_SHEET,
                        anchorStrategy = DrawerAnchorStrategies.existingHeight())
                    drawerState.controller.setAnchorStrategy(config.anchorStrategy)
                    drawerState.controller.refreshAnchors()
                    drawerState.collapse()
                }) { Text("Peek") }
                Button(onClick = {
                    config = config.copy(presentationMode = DrawerPresentationMode.BOTTOM_SHEET,
                        anchorStrategy = DrawerAnchorStrategies.threeStage(0.5f, false))
                    drawerState.controller.setAnchorStrategy(config.anchorStrategy)
                    drawerState.controller.refreshAnchors()
                    drawerState.controller.moveToAnchor("half", true)
                }) { Text("Half") }
                Button(onClick = {
                    config = config.copy(presentationMode = DrawerPresentationMode.BOTTOM_SHEET,
                        anchorStrategy = DrawerAnchorStrategies.threeStage(0.5f, false))
                    drawerState.controller.setAnchorStrategy(config.anchorStrategy)
                    drawerState.controller.refreshAnchors()
                    drawerState.expand()
                }) { Text("Expanded") }
            }
            Button(onClick = {
                config = config.copy(presentationMode = DrawerPresentationMode.FULL_SCREEN,
                    anchorStrategy = DrawerAnchorStrategies.threeStage(0.5f, true))
                drawerState.controller.setAnchorStrategy(config.anchorStrategy)
                drawerState.controller.refreshAnchors()
                drawerState.expand()
            }, modifier = Modifier.fillMaxWidth()) { Text("全屏抽屉") }
            Button(onClick = {
                config = DrawerPresets.config(DrawerPreset.STANDARD)
                drawerState.controller.setAnchorStrategy(config.anchorStrategy)
                drawerState.controller.refreshAnchors()
                drawerState.collapse()
            }, modifier = Modifier.fillMaxWidth()) { Text("恢复标准底部抽屉") }

            Text("尺寸参数", style = MaterialTheme.typography.titleMedium)
            SliderSetting("Peek 高度", peekHeight, 40f..120f) {
                peekHeight = it
                config = config.copy(style = config.style.copy(
                    dimensions = config.style.dimensions.copy(peekHeightDp = it)))
            }
            SliderSetting("最大展开高度", expandedHeight, 320f..900f) {
                expandedHeight = it
                config = config.copy(style = config.style.copy(
                    dimensions = config.style.dimensions.copy(expandedMaxHeightDp = it.coerceAtLeast(peekHeight))))
            }
            SliderSetting("顶部圆角", cornerRadius, 0f..40f) {
                cornerRadius = it
                config = config.copy(style = config.style.copy(
                    shape = config.style.shape.copy(topStartRadiusDp = it, topEndRadiusDp = it),
                    dimensions = config.style.dimensions.copy(cornerRadiusDp = it)))
            }
            SliderSetting("动画时长", settleDuration, 0f..700f) {
                settleDuration = it
                config = config.copy(style = config.style.copy(
                    motion = config.style.motion.copy(settleDurationMs = it.toLong())))
            }

            Text("交互开关", style = MaterialTheme.typography.titleMedium)
            ToggleSetting("允许拖动", dragEnabled) {
                dragEnabled = it
                config = config.copy(enableDrag = it)
            }
            ToggleSetting("允许嵌套滚动", nestedScrollEnabled) {
                nestedScrollEnabled = it
                config = config.copy(enableNestedScroll = it)
            }
            ToggleSetting("显示遮罩", scrimEnabled) {
                scrimEnabled = it
                config = config.copy(style = config.style.copy(drawScrim = it))
            }
            ToggleSetting("点击遮罩折叠", config.dismissOnScrimTap) {
                config = config.copy(dismissOnScrimTap = it)
            }

            Text("导航配置", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { config = config.copy(navigationConfig =
                    DrawerNavigationConfig(placement = DrawerNavigationPlacement.TOP)) }) { Text("导航置顶") }
                Button(onClick = { config = config.copy(navigationConfig =
                    DrawerNavigationConfig(placement = DrawerNavigationPlacement.BOTTOM)) }) { Text("导航置底") }
                Button(onClick = { config = config.copy(navigationConfig =
                    DrawerNavigationConfig(placement = DrawerNavigationPlacement.HIDDEN)) }) { Text("隐藏导航") }
            }

            Text("主题", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { config = config.copy(style = config.style.copy(
                    themeMode = DrawerThemeMode.FOLLOW_HOST)) }) { Text("跟随宿主") }
                Button(onClick = { config = config.copy(style = config.style.copy(
                    themeMode = DrawerThemeMode.LIGHT)) }) { Text("浅色") }
                Button(onClick = { config = config.copy(style = config.style.copy(
                    themeMode = DrawerThemeMode.DARK)) }) { Text("深色") }
            }
            Spacer(Modifier.height(180.dp))
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label + "：" + value.toInt())
        Slider(value = value.coerceIn(range), onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
