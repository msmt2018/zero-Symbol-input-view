/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * This file is distributed under the terms of all licenses listed above.
 * See the corresponding license texts in the repository.
 *
 * Author: android_zero
 * https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataMigration
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.drawer.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolDrawerDefaults
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerManagerScreen
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SharedPreferencesSymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsScreen

/**
 * Ready-to-use View V2 symbol toolbar with its manager and settings surfaces.
 *
 * Hosts can add this single view and configure [onSymbolAction]. Repository
 * setup, legacy migration, settings persistence, manager synchronization and
 * dialog ownership are handled here. Existing SymbolDrawerView APIs remain
 * available and are not modified by this facade.
 */
class SymbolDrawerToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val groupRepository = SharedPreferencesSymbolGroupRepository(context.applicationContext)
    private val settingsRepository = SharedPreferencesSymbolDrawerSettingsRepository(context.applicationContext)
    private var managerDialog: AlertDialog? = null
    private var settingsDialog: AlertDialog? = null
    private var actionBinding: AutoCloseable? = null

    /** Receives non-successful typed action dispatch results. */
    var onActionFailure: ((SymbolEntry, Boolean, SymbolActionResult) -> Unit)? = null

    val drawer: SymbolDrawerView = SymbolDrawerView(context).also { view ->
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    /** Called for a short-clicked or long-pressed symbol action. */
    var onSymbolAction: SymbolDrawerView.OnSymbolActionListener?
        get() = drawer.onSymbolActionListener
        set(value) {
            drawer.onSymbolActionListener = value
        }

    /**
     * Binds the toolbar to an editor-neutral action host. The returned handle
     * can be closed from the host's onDestroy; detaching this view also closes it.
     */
    fun bindActionHost(host: SymbolActionHost): AutoCloseable {
        actionBinding?.close()
        val listener = SymbolDrawerView.OnSymbolActionListener { symbol, isLongPress ->
            val action = if (isLongPress) symbol.longAction else symbol.shortAction
            if (action != null) {
                val result = drawer.executeAction(action, host)
                if (result !is SymbolActionResult.Executed) {
                    onActionFailure?.invoke(symbol, isLongPress, result)
                }
            }
        }
        drawer.onSymbolActionListener = listener
        lateinit var handle: AutoCloseable
        handle = AutoCloseable {
            if (drawer.onSymbolActionListener === listener) {
                drawer.onSymbolActionListener = null
            }
            if (actionBinding === handle) actionBinding = null
        }
        actionBinding = handle
        return handle
    }

    init {
        LegacySymbolDataMigration.importIfV2Empty(context, groupRepository)
        if (groupRepository.loadGroups().isEmpty()) {
            groupRepository.saveGroups(SymbolDrawerDefaults.createGroups(context))
        }
        drawer.setRepository(groupRepository)
        drawer.applySettings(settingsRepository.load().settings)
        drawer.onOpenManagerListener = ::showManager
        drawer.onOpenSettingsListener = ::showSettings
    }

    private fun showManager() {
        if (managerDialog?.isShowing == true) return
        val screen = SymbolDrawerManagerScreen(
            context = context,
            repository = groupRepository,
            liveDrawer = drawer
        )
        managerDialog = AlertDialog.Builder(context)
            .setView(screen)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnDismissListener { managerDialog = null }
                dialog.show()
            }
    }

    private fun showSettings() {
        if (settingsDialog?.isShowing == true) return
        val screen = SymbolDrawerSettingsScreen(
            context = context,
            repository = settingsRepository,
            groupRepository = groupRepository
        ).apply {
            onSettingsChanged = drawer::applySettings
            onGroupsChanged = { updated ->
                groupRepository.saveGroups(updated)
                drawer.submitGroups(updated, preserveSelectedGroup = true)
            }
        }
        settingsDialog = AlertDialog.Builder(context)
            .setView(screen)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .also { dialog ->
                dialog.setOnDismissListener { settingsDialog = null }
                dialog.show()
            }
    }

    override fun onDetachedFromWindow() {
        actionBinding?.close()
        actionBinding = null
        managerDialog?.dismiss()
        managerDialog = null
        settingsDialog?.dismiss()
        settingsDialog = null
        drawer.onOpenManagerListener = null
        drawer.onOpenSettingsListener = null
        super.onDetachedFromWindow()
    }
}
