/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.zerostudio.idea.widget.drawer.action.SymbolActionRegistry
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocument

/**
 * JVM-visible compatibility facade.
 *
 * The implementation lives in the editor-neutral drawer.action API. Keeping
 * this object preserves the historical JVM class and static entry points
 * while Kotlin callers can migrate to the public package.
 */
object GenericSymbolTextActions {
    @JvmStatic
    fun createRegistry(): SymbolActionRegistry<SymbolTextDocument> =
        com.zerostudio.idea.widget.drawer.action.GenericSymbolTextActions.createRegistry()

    @JvmStatic
    fun supportedCommandIds(): Set<String> =
        com.zerostudio.idea.widget.drawer.action.GenericSymbolTextActions.supportedCommandIds()
}
