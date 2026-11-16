/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Compose-only renderer covering every built-in V2 group-indicator style. */
@Composable
internal fun BuiltInGroupIndicator(
    groups: List<SymbolGroup>,
    selectedIndex: Int,
    settings: SymbolDrawerSettings,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (groups.isEmpty()) return
    val style = settings.indicatorStyle
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    if (style == GroupIndicatorStyle.TEXT_TABS || style == GroupIndicatorStyle.TEXT) {
        Row(modifier.fillMaxWidth().height(settings.indicatorHeightDp.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            groups.forEachIndexed { index, group ->
                val selected = index == selectedIndex
                Surface(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).clickable { onSelect(index) },
                    color = if (selected) primary.copy(alpha=.14f) else Color.Transparent, shape=RoundedCornerShape(10.dp)) {
                    Box(Modifier.padding(horizontal=4.dp), contentAlignment=Alignment.Center) {
                        Text(if(settings.showGroupTitles) group.title else (index+1).toString(),
                            color=if(selected) primary else inactive,
                            style=if(selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                            maxLines=1, softWrap=false)
                    }
                }
            }
        }
        return
    }
    Canvas(modifier.fillMaxWidth().height(settings.indicatorHeightDp.dp)
        .semantics { contentDescription = "Group " + (selectedIndex+1) + " of " + groups.size }
        .pointerInput(groups.size) {
            detectTapGestures { tap ->
                if (size.width > 0) {
                    val index = (tap.x / size.width * groups.size).toInt().coerceIn(groups.indices)
                    onSelect(index)
                }
            }
        }) {
        val count=groups.size
        val slot=size.width/count
        val cy=size.height/2f
        val radius=(size.height*.22f).coerceAtLeast(2.dp.toPx())
        val idle=inactive.copy(alpha=.48f)
        val active=primary
        fun cx(i:Int)=slot*(i+.5f)
        fun bar(x:Float,y:Float,w:Float,h:Float,color:Color,alpha:Float=1f) {
            drawRoundRect(color.copy(alpha=alpha),androidx.compose.ui.geometry.Offset(x,y),
                androidx.compose.ui.geometry.Size(w.coerceAtLeast(0f),h.coerceAtLeast(0f)),
                androidx.compose.ui.geometry.CornerRadius(h/2f))
        }
        when(style) {
            GroupIndicatorStyle.PILL -> {
                bar(cx(selectedIndex)-slot*.38f,cy-radius*1.6f,slot*.76f,radius*3.2f,active,.16f)
                drawCircle(active,radius*.62f,androidx.compose.ui.geometry.Offset(cx(selectedIndex),cy))
            }
            GroupIndicatorStyle.DOTS,GroupIndicatorStyle.PULSE_DOTS,GroupIndicatorStyle.ORBIT_DOTS -> groups.indices.forEach { i ->
                val r=if(i==selectedIndex) radius*(if(style==GroupIndicatorStyle.PULSE_DOTS)1.2f else 1f) else radius*.48f
                drawCircle(if(i==selectedIndex)active else idle,r,androidx.compose.ui.geometry.Offset(cx(i),cy))
                if(i==selectedIndex && style==GroupIndicatorStyle.ORBIT_DOTS)
                    drawCircle(active,r*1.8f,androidx.compose.ui.geometry.Offset(cx(i),cy),style=Stroke(1.2.dp.toPx()))
            }
            GroupIndicatorStyle.UNDERLINE,GroupIndicatorStyle.PROGRESS_LINE,GroupIndicatorStyle.SEGMENTED_TRACK,
            GroupIndicatorStyle.DOUBLE_TRACK,GroupIndicatorStyle.MORPHING_BAR,GroupIndicatorStyle.VERTICAL_RAIL,
            GroupIndicatorStyle.STEPPER -> groups.indices.forEach { i ->
                val h=if(i==selectedIndex)radius*.58f else radius*.3f
                val y=if(style==GroupIndicatorStyle.UNDERLINE)size.height-h else cy-h/2f
                bar(slot*i+slot*.12f,y,slot*.76f,h,if(i==selectedIndex)active else idle)
                if(style==GroupIndicatorStyle.DOUBLE_TRACK) bar(slot*i+slot*.12f,cy+radius*.7f,slot*.76f,1.dp.toPx(),idle)
                if(style==GroupIndicatorStyle.STEPPER) drawCircle(if(i<=selectedIndex)active else idle,radius*.28f,androidx.compose.ui.geometry.Offset(cx(i),cy-radius))
            }
            GroupIndicatorStyle.GOOEY_METABALL,GroupIndicatorStyle.ELASTIC_WORM,GroupIndicatorStyle.SPRING_BLOB,
            GroupIndicatorStyle.LIQUID_MERGE,GroupIndicatorStyle.FLUID_TRAIL -> {
                groups.indices.forEach { drawCircle(idle,radius*.34f,androidx.compose.ui.geometry.Offset(cx(it),cy)) }
                val stretch=if(style==GroupIndicatorStyle.ELASTIC_WORM)1.7f else 1.25f
                bar(cx(selectedIndex)-radius*stretch,cy-radius*.68f,radius*stretch*2,radius*1.36f,active)
                if(style==GroupIndicatorStyle.GOOEY_METABALL || style==GroupIndicatorStyle.LIQUID_MERGE || style==GroupIndicatorStyle.FLUID_TRAIL) {
                    if(selectedIndex>0)drawCircle(active,radius*.48f,androidx.compose.ui.geometry.Offset(cx(selectedIndex)-slot*.28f,cy))
                    if(selectedIndex<count-1)drawCircle(active,radius*.48f,androidx.compose.ui.geometry.Offset(cx(selectedIndex)+slot*.28f,cy))
                }
            }
            GroupIndicatorStyle.VERNIER_TICKS,GroupIndicatorStyle.MECHANICAL_RULER,GroupIndicatorStyle.WHEEL_ROLLER,
            GroupIndicatorStyle.CAMERA_DIAL,GroupIndicatorStyle.MINIMAL_DASHES,GroupIndicatorStyle.SQUARE_TICKS -> groups.indices.forEach { i ->
                val extent=if(i==selectedIndex)radius*1.5f else radius
                drawLine(if(i==selectedIndex)active else idle,androidx.compose.ui.geometry.Offset(cx(i),cy-extent),
                    androidx.compose.ui.geometry.Offset(cx(i),cy+extent),if(i==selectedIndex)2.5.dp.toPx() else 1.dp.toPx())
                if(style==GroupIndicatorStyle.WHEEL_ROLLER && i==selectedIndex)
                    drawCircle(active,radius*.8f,androidx.compose.ui.geometry.Offset(cx(i),cy),style=Stroke(1.2.dp.toPx()))
                if(style==GroupIndicatorStyle.SQUARE_TICKS)
                    drawRect(if(i==selectedIndex)active else idle,androidx.compose.ui.geometry.Offset(cx(i)-1.dp.toPx(),cy-extent),androidx.compose.ui.geometry.Size(2.dp.toPx(),extent*2))
            }
            GroupIndicatorStyle.RADIAL_RING,GroupIndicatorStyle.RADAR_SWEEP,GroupIndicatorStyle.HUD_ARC,GroupIndicatorStyle.ARC_NODES -> {
                val r=(size.height*.38f).coerceAtMost(size.width*.28f)
                val c=androidx.compose.ui.geometry.Offset(size.width/2f,cy)
                drawCircle(idle.copy(alpha=.35f),r,c,style=Stroke(1.5.dp.toPx()))
                groups.indices.forEach { i ->
                    val a=-PI/2+2*PI*i/count
                    val p=androidx.compose.ui.geometry.Offset(c.x+cos(a).toFloat()*r,c.y+sin(a).toFloat()*r)
                    drawCircle(if(i==selectedIndex)active else idle,if(i==selectedIndex)3.5.dp.toPx() else 2.dp.toPx(),p)
                }
                val a=-PI/2+2*PI*selectedIndex/count
                drawLine(active,c,androidx.compose.ui.geometry.Offset(c.x+cos(a).toFloat()*r,c.y+sin(a).toFloat()*r),2.dp.toPx())
            }
            GroupIndicatorStyle.ELASTIC_WAVE,GroupIndicatorStyle.SINE_WAVE,GroupIndicatorStyle.BOUNCE_TRACK,GroupIndicatorStyle.SPRING_SLIDER -> {
                val path=Path()
                groups.indices.forEach { i ->
                    val y=cy+if(style==GroupIndicatorStyle.SINE_WAVE)sin(i*.9).toFloat()*radius*.55f else 0f
                    val p=androidx.compose.ui.geometry.Offset(cx(i),y)
                    if(i==0)path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y)
                    drawCircle(if(i==selectedIndex)active else idle,if(i==selectedIndex)radius*.5f else radius*.25f,p)
                }
                drawPath(path,active.copy(alpha=.7f),style=Stroke(1.5.dp.toPx()))
            }
            GroupIndicatorStyle.DIAMOND_MARKERS,GroupIndicatorStyle.TRIANGLE_POINTER,GroupIndicatorStyle.HEXAGON_NODES,
            GroupIndicatorStyle.GLOWING_NODES -> groups.indices.forEach { i ->
                val x=cx(i);val r=if(i==selectedIndex)radius else radius*.55f;val path=Path()
                when(style) {
                    GroupIndicatorStyle.DIAMOND_MARKERS->{path.moveTo(x,cy-r);path.lineTo(x+r,cy);path.lineTo(x,cy+r);path.lineTo(x-r,cy);path.close()}
                    GroupIndicatorStyle.TRIANGLE_POINTER->{path.moveTo(x,cy-r);path.lineTo(x+r,cy+r);path.lineTo(x-r,cy+r);path.close()}
                    else->{for(n in 0..5){val a=PI/3*n-PI/2;val px=x+cos(a).toFloat()*r;val py=cy+sin(a).toFloat()*r;if(n==0)path.moveTo(px,py)else path.lineTo(px,py)};path.close()}
                }
                if(style==GroupIndicatorStyle.GLOWING_NODES && i==selectedIndex)drawCircle(active.copy(alpha=.2f),r*1.8f,androidx.compose.ui.geometry.Offset(x,cy))
                drawPath(path,if(i==selectedIndex)active else idle)
            }
            GroupIndicatorStyle.STACKED_CARDS,GroupIndicatorStyle.MINI_PREVIEW,GroupIndicatorStyle.THUMBNAIL_STRIP,
            GroupIndicatorStyle.NUMBER_BADGES,GroupIndicatorStyle.DIGITAL_COUNTER -> groups.indices.forEach { i ->
                val w=slot*.56f;val h=radius*1.7f;val x=cx(i)-w/2f;val y=cy-h/2f
                if(style==GroupIndicatorStyle.STACKED_CARDS && i==selectedIndex)bar(x-3.dp.toPx(),y+3.dp.toPx(),w,h,idle)
                bar(x,y,w,h,if(i==selectedIndex)active else idle,if(i==selectedIndex)1f else .65f)
                if(style==GroupIndicatorStyle.THUMBNAIL_STRIP || style==GroupIndicatorStyle.MINI_PREVIEW)
                    bar(x+2.dp.toPx(),y+2.dp.toPx(),w-4.dp.toPx(),h*.35f,Color.White,.3f)
                if(style==GroupIndicatorStyle.NUMBER_BADGES || style==GroupIndicatorStyle.DIGITAL_COUNTER)
                    drawCircle(if(i==selectedIndex)active else idle,radius*.45f,androidx.compose.ui.geometry.Offset(cx(i),cy))
            }
            else -> groups.indices.forEach { i -> bar(slot*i+2.dp.toPx(),cy-radius*.25f,slot-4.dp.toPx(),radius*.5f,if(i==selectedIndex)active else idle) }
        }
    }
}
