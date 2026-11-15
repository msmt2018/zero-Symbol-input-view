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
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 底部抽屉 + 分页容器的基础抽象组件。
 *
 * 该类用于兼容仍需要 TabLayout + ViewPager2 形态的场景，
 * 同时将布局改为纯代码创建，避免依赖外部 XML 结构。
 *
 * 抽屉绑定只负责同步 BottomSheetBehavior 的展开进度，不接管宿主的触摸事件、
 * 拖动手势或符号管理逻辑。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
abstract class BottomSheetTabPagerContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    protected val viewPager: ViewPager2
    protected val tabLayout: TabLayout
    protected val tabRow: View

    private var tabMediator: TabLayoutMediator? = null
    private var boundTabTitles: List<String>? = null
    private var bottomSheetView: View? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var registeredBottomSheetCallback: BottomSheetBehavior.BottomSheetCallback? = null
    private var isBottomSheetCallbackRegistered = false
    private var initialCollapsePending = false
    private var pendingInitialCollapse: Runnable? = null

    init {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        tabLayout = TabLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
            tabMode = TabLayout.MODE_SCROLLABLE
        }
        tabRow = tabLayout
        viewPager = ViewPager2(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0)
            offscreenPageLimit = 1
            isSaveEnabled = false
        }
        root.addView(tabLayout)
        root.addView(viewPager)
        addView(root)
        setExpansionFraction(0f)
    }

    /** 根据抽屉滑动进度更新分页栏显隐效果。 */
    protected fun setExpansionFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped <= 0f) {
            tabRow.visibility = View.GONE
        } else {
            tabRow.visibility = View.VISIBLE
            tabRow.alpha = clamped
            tabRow.translationY = (1f - clamped) * -8f * resources.displayMetrics.density
        }
    }

    /**
     * 绑定并监听底部抽屉状态变化。
     *
     * 保留原有参数和 BottomSheetBehavior 配置，避免破坏既有调用方。
     * 监听器仅在本容器 attached 时注册；临时 detach 时解绑但保留绑定目标，
     * 重新 attach 后恢复监听。重复调用会先完整释放旧绑定。
     */
    open fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) {
        clearBottomSheetBinding()

        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetView = bottomSheet
        bottomSheetBehavior = behavior
        behavior.saveFlags = BottomSheetBehavior.SAVE_NONE
        behavior.isHideable = false
        behavior.isDraggable = true
        behavior.skipCollapsed = false
        behavior.isFitToContents = true

        registeredBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> setExpansionFraction(0f)
                    BottomSheetBehavior.STATE_EXPANDED -> setExpansionFraction(1f)
                    BottomSheetBehavior.STATE_HIDDEN -> behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                setExpansionFraction(slideOffset.coerceIn(0f, 1f))
            }
        }

        initialCollapsePending = true
        if (isAttachedToWindow) {
            registerBottomSheetCallbackIfNeeded()
            scheduleInitialCollapse()
        }
    }

    /** 页面恢复时将抽屉重置到折叠态。 */
    fun onHostResume() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /** 将标题列表绑定到 TabLayout 与 ViewPager2。 */
    protected fun bindTabs(titles: List<String>) {
        boundTabTitles = titles.toList()
        attachTabMediatorIfPossible()
    }

    private fun attachTabMediatorIfPossible() {
        detachTabMediatorSafely()
        val titles = boundTabTitles ?: return
        if (titles.isEmpty()) {
            tabLayout.removeAllTabs()
            return
        }
        // TabLayoutMediator.attach() requires an adapter. A subclass may install
        // its adapter after this view is reattached, then call bindTabs again.
        if (viewPager.adapter == null) return

        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles.getOrNull(position) ?: ("Tab " + (position + 1))
        }.apply { attach() }
    }

    private fun registerBottomSheetCallbackIfNeeded() {
        val behavior = bottomSheetBehavior ?: return
        val callback = registeredBottomSheetCallback ?: return
        if (isBottomSheetCallbackRegistered) return

        behavior.addBottomSheetCallback(callback)
        isBottomSheetCallbackRegistered = true
    }

    private fun unregisterBottomSheetCallback() {
        val behavior = bottomSheetBehavior
        val callback = registeredBottomSheetCallback
        if (isBottomSheetCallbackRegistered && behavior != null && callback != null) {
            behavior.removeBottomSheetCallback(callback)
        }
        isBottomSheetCallbackRegistered = false
    }

    private fun cancelInitialCollapse() {
        val target = bottomSheetView
        val runnable = pendingInitialCollapse
        if (target != null && runnable != null) {
            target.removeCallbacks(runnable)
        }
        pendingInitialCollapse = null
    }

    private fun scheduleInitialCollapse() {
        if (!initialCollapsePending || !isAttachedToWindow) return
        val target = bottomSheetView ?: return
        val behavior = bottomSheetBehavior ?: return

        cancelInitialCollapse()
        val runnable = object : Runnable {
            override fun run() {
                if (pendingInitialCollapse !== this) return
                if (bottomSheetView !== target || bottomSheetBehavior !== behavior || !isAttachedToWindow) {
                    return
                }
                if (!target.isAttachedToWindow) {
                    // The sheet may have detached between posting and execution.
                    // Re-posting while detached queues the task for its next attach.
                    target.post(this)
                    return
                }
                pendingInitialCollapse = null
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                initialCollapsePending = false
            }
        }
        pendingInitialCollapse = runnable
        target.post(runnable)
    }

    private fun clearBottomSheetBinding() {
        cancelInitialCollapse()
        unregisterBottomSheetCallback()
        registeredBottomSheetCallback = null
        bottomSheetBehavior = null
        bottomSheetView = null
        initialCollapsePending = false
    }

    private fun detachTabMediatorSafely() {
        val mediator = tabMediator ?: return
        try {
            mediator.detach()
        } catch (_: IllegalStateException) {
            // The ViewPager2 may already have released its adapter during teardown.
        }
        tabMediator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachTabMediatorIfPossible()
        registerBottomSheetCallbackIfNeeded()
        scheduleInitialCollapse()
    }

    override fun onDetachedFromWindow() {
        cancelInitialCollapse()
        unregisterBottomSheetCallback()
        detachTabMediatorSafely()
        // Keep the target and callback so a temporary detach/attach cycle can rebind.
        super.onDetachedFromWindow()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
