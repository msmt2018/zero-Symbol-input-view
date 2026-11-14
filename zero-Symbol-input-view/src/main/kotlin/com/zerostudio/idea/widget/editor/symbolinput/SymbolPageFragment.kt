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

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * 单个符号分组页面。
 *
 * 负责渲染指定分组的网格列表，并将点击/长按动作分发给 `SymbolActionExecutor`。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
class SymbolPageFragment : Fragment() {

    companion object {
        private const val ARG_GROUP_INDEX = "arg_group_index"
        private const val ARG_VIEW_ID = "arg_view_id"

        /**
         * 创建并初始化页面实例。
         */
        fun newInstance(groupIndex: Int, viewId: Int): SymbolPageFragment {
            val fragment = SymbolPageFragment()
            val args = Bundle()
            args.putInt(ARG_GROUP_INDEX, groupIndex)
            args.putInt(ARG_VIEW_ID, viewId)
            fragment.arguments = args
            return fragment
        }
    }

    private var groupIndex: Int = 0
    private lateinit var viewModel: SymbolInputViewModel

    /**
     * 初始化界面、数据与交互事件。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupIndex = arguments?.getInt(ARG_GROUP_INDEX) ?: 0
    }

    /**
     * 创建页面视图并绑定网格适配器。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val viewId = arguments?.getInt(ARG_VIEW_ID) ?: 0
        val key = "SymbolInputViewModel_$viewId"
        
        viewModel = ViewModelProvider(requireActivity()).get(key, SymbolInputViewModel::class.java)

        val rv = RecyclerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setHasFixedSize(true)
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutManager = GridLayoutManager(context, 8)
            clipToPadding = false
            val horizontal = (8 * resources.displayMetrics.density).roundToInt()
            val vertical = (2 * resources.displayMetrics.density).roundToInt()
            setPadding(horizontal, vertical, horizontal, vertical)
        }

        val group = viewModel.groups.getOrNull(groupIndex)
        if (group != null) {
            rv.adapter = SymbolAdapter(group.items)
        }

        return rv
    }

    private inner class SymbolAdapter(private val items: List<SymbolItem>) : RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder>() {

        inner class SymbolViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)

    /**
         * 执行 onCreateViewHolder 方法。
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = GridLayoutManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                minHeight = (34 * resources.displayMetrics.density).roundToInt()
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                isClickable = true
                isFocusable = true

                val tvColor = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, tvColor, true)
                setTextColor(if (tvColor.resourceId != 0) context.getColor(tvColor.resourceId) else tvColor.data)

                val tvBg = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tvBg, true)
                setBackgroundResource(tvBg.resourceId)
            }
            return SymbolViewHolder(tv)
        }

    /**
         * 执行 onBindViewHolder 方法。
         */
        override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
            val item = items[position]
            holder.tv.text = item.display

            holder.tv.setOnClickListener {
                viewModel.editor?.let { ed ->
                    SymbolActionExecutor.execute(ed, item.shortAction, item.shortText, viewModel.onOpenManagerListener)
                }
            }

            holder.tv.setOnLongClickListener {
                if (item.longAction != null) {
                    viewModel.editor?.let { ed ->
                        SymbolActionExecutor.execute(ed, item.longAction!!, item.longText, viewModel.onOpenManagerListener)
                    }
                    true
                } else {
                    false
                }
            }
        }

    /**
         * 执行 getItemCount 方法。
         */
        override fun getItemCount(): Int = items.size
    }
}
