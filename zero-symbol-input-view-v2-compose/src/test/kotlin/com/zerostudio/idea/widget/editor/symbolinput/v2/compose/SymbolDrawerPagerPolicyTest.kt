/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SymbolDrawerPagerPolicyTest {
    @Test
    fun disabledPrefetchKeepsOnlyVisiblePageComposed() {
        assertEquals(
            0,
            pageBeyondViewportCount(SymbolDrawerSettings(prefetchAdjacentGroups = false, pageCacheLimit = 9))
        )
    }

    @Test
    fun cacheBudgetControlsSymmetricAdjacentPageComposition() {
        assertEquals(0, pageBeyondViewportCount(SymbolDrawerSettings(pageCacheLimit = 1)))
        assertEquals(1, pageBeyondViewportCount(SymbolDrawerSettings(pageCacheLimit = 3)))
        assertEquals(4, pageBeyondViewportCount(SymbolDrawerSettings(pageCacheLimit = 9)))
    }
}
