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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.drawer.core.DrawerAnchorStrategies
import com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationConfig
import com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationPlacement
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.panel.drawerPanelContent
import com.zerostudio.idea.widget.drawer.style.DrawerDimensions
import com.zerostudio.idea.widget.drawer.style.DrawerShape
import com.zerostudio.idea.widget.drawer.style.DrawerStyle

/** Compose-native API laboratory with four proportional anchors and a real audio-player example. */
class DrawerApiLabActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    DrawerApiLabScreen()
                }
            }
        }
    }
}

@Composable
private fun DrawerApiLabScreen() {
    val context = LocalContext.current
    val player = remember(context) { DrawerDemoPlayerController(context.applicationContext) }
    val playerState = remember(player) { mutableStateOf(player.state) }
    val shuffleState = remember { mutableStateOf(true) }
    var selectedPage by remember { mutableStateOf("player") }
    var selectedAnchor by remember { mutableStateOf("hidden") }
    val drawerState = com.zerostudio.idea.widget.drawer.rememberDrawerContainerState()

    DisposableEffect(player) {
        val subscription = player.addListener { playerState.value = it }
        onDispose {
            subscription.close()
            player.close()
        }
    }

    val config = remember {
        DrawerConfig(
            anchorStrategy = DrawerAnchorStrategies.fourStageWithHidden(),
            style = DrawerStyle(
                dimensions = DrawerDimensions(
                    peekHeightDp = 54f,
                    expandedMaxHeightDp = 900f,
                    horizontalPaddingDp = 20f,
                    verticalPaddingDp = 12f,
                    panelSpacingDp = 12f,
                    handleWidthDp = 42f,
                    handleHeightDp = 4f
                ),
                shape = DrawerShape(
                    topStartRadiusDp = 28f,
                    topEndRadiusDp = 28f,
                    bottomStartRadiusDp = 0f,
                    bottomEndRadiusDp = 0f
                ),
                drawScrim = true
            ),
            enableDrag = true,
            enableNestedScroll = true,
            enableEdgeToEdge = true,
            dismissOnScrimTap = true,
            navigationConfig = DrawerNavigationConfig(
                enabled = true,
                placement = DrawerNavigationPlacement.TOP
            ),
            presentationMode = DrawerPresentationMode.FULL_SCREEN
        )
    }

    // Keep page identities stable while the playback position publishes
    // progress updates. The page content reads the remembered state holders
    // when it is composed, so the player remains live without rebuilding the
    // navigation model every 450ms.
    val pages = remember(player) {
        listOf(
            DrawerPage(
                "player",
                "播放器",
                drawerPanelContent {
                    MusicPlayerPanel(
                        player = player,
                        state = playerState.value,
                        shuffleEnabled = shuffleState.value,
                        onShuffleChanged = { shuffleState.value = it }
                    )
                }
            ),
            DrawerPage(
                "api",
                "API 说明",
                drawerPanelContent {
                    DrawerApiReferencePanel()
                }
            )
        )
    }

    BackHandler(enabled = drawerState.expanded) {
        drawerState.collapse()
        selectedAnchor = "hidden"
    }

    DrawerContainer(
        config = config,
        state = drawerState,
        title = "ZERO AUDIO  /  DRAWER API",
        pages = pages,
        selectedPageId = selectedPage,
        onPageSelected = { selectedPage = it.id },
        onDismiss = { selectedAnchor = "hidden" },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                "Drawer API Lab",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "四档可拖拽抽屉 · 原生 Compose · 音乐播放器实战",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("屏幕高度锚点", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "选择档位后可继续直接拖动抽屉；100% 档从状态栏下缘开始，内容避开底部系统导航区域。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnchorButton("0%", "hidden", selectedAnchor, Modifier.weight(1f)) {
                            selectedAnchor = it
                            drawerState.controller.moveToAnchor(it)
                        }
                        AnchorButton("30%", "peek", selectedAnchor, Modifier.weight(1f)) {
                            selectedAnchor = it
                            drawerState.controller.moveToAnchor(it)
                        }
                        AnchorButton("50%", "half", selectedAnchor, Modifier.weight(1f)) {
                            selectedAnchor = it
                            drawerState.controller.moveToAnchor(it)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnchorButton("80%", "large", selectedAnchor, Modifier.weight(1f)) {
                            selectedAnchor = it
                            drawerState.controller.moveToAnchor(it)
                        }
                        AnchorButton("100%", "expanded", selectedAnchor, Modifier.weight(1f)) {
                            selectedAnchor = it
                            drawerState.controller.moveToAnchor(it)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            selectedAnchor = "hidden"
                            drawerState.collapse()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("完全隐藏抽屉（0%）")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("真实业务示例", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "打开抽屉中的「播放器」页面，体验网络音频下载到应用缓存、播放控制、进度拖动和随机切歌。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            selectedPage = "player"
                            selectedAnchor = "large"
                            drawerState.controller.moveToAnchor("large")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("打开音乐播放器")
                    }
                }
            }

            Text(
                "抽屉默认处于 0% 完全隐藏状态；打开后可在 30% / 50% / 80% / 100% 四个稳定锚点之间切换。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnchorButton(
    label: String,
    anchorId: String,
    selectedAnchor: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val selected = selectedAnchor == anchorId
    Button(
        onClick = { onSelect(anchorId) },
        modifier = modifier
    ) {
        Text(if (selected) "✓ $label" else label)
    }
}

@Composable
private fun MusicPlayerPanel(
    player: DrawerDemoPlayerController,
    state: DrawerDemoPlayerState,
    shuffleEnabled: Boolean,
    onShuffleChanged: (Boolean) -> Unit
) {
    val track = player.currentTrack()
    val duration = state.durationMs.coerceAtLeast(0)
    val progress = state.positionMs.coerceIn(0, duration.coerceAtLeast(1)).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF182A4A),
                            Color(0xFF5C4B83),
                            Color(0xFFB36B70)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ZERO", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(
                    "AUDIO",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Text("CURATED AMBIENT SESSIONS", color = Color.White.copy(alpha = 0.76f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("NOW PLAYING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(track.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(track.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Slider(
            value = progress,
            onValueChange = { player.seekTo(it.toInt()) },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            enabled = duration > 0
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelSmall)
            Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = player::previousTrack) { Text("上一首") }
            Button(
                onClick = player::togglePlayback,
                enabled = !state.isPreparing
            ) {
                Text(
                    when {
                        state.isPreparing -> "缓存中…"
                        state.isPlaying -> "暂停"
                        else -> "播放"
                    }
                )
            }
            OutlinedButton(onClick = { player.nextTrack(shuffleEnabled) }) { Text("下一首") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("随机播放", style = MaterialTheme.typography.titleSmall)
                Text(
                    "从已缓存或可下载的 CC0 曲目中随机切换",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = shuffleEnabled, onCheckedChange = onShuffleChanged)
        }

        Button(
            onClick = player::playRandomTrack,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isPreparing
        ) {
            Text("随机播放一首")
        }

        if (state.error != null) {
            Text(
                state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                if (state.isPreparing) "正在下载并缓存音频…" else "首次播放时下载到应用缓存；后续播放优先读取本地文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()
        Text("演示曲库", style = MaterialTheme.typography.titleMedium)
        player.availableTracks().forEachIndexed { index, item ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (index == state.trackIndex) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else MaterialTheme.colorScheme.surfaceContainer
                ),
                onClick = { player.playTrack(index) }
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerApiReferencePanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("四档锚点 API", style = MaterialTheme.typography.titleLarge)
        Text("DrawerAnchorStrategies.fourStageWithHidden() 提供 hidden(0%) / peek(30%) / half(50%) / large(80%) / expanded(100%) 五个锚点。")
        Text("每个锚点都根据父容器实际测量高度计算，并由 DrawerStateHolder 统一管理拖动、吸附和动画。")
        HorizontalDivider()
        Text("全屏与系统栏", style = MaterialTheme.typography.titleMedium)
        Text("FULL_SCREEN 模式使用真实窗口可用高度，不再受普通抽屉 expandedMaxHeightDp 上限影响。顶部从状态栏下缘开始，底部内容避开导航栏。")
        HorizontalDivider()
        Text("交互与业务集成", style = MaterialTheme.typography.titleMedium)
        Text("宿主只需持有 DrawerContainerState、配置锚点策略，并通过 DrawerPage / drawerPanelContent 提供任意 Compose 页面。")
        Text("本示例播放器将网络音频下载到应用 cacheDir，再由 MediaPlayer 播放；抽屉框架本身不依赖播放器或 Activity 生命周期 API。")
    }
}

private fun formatDuration(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
