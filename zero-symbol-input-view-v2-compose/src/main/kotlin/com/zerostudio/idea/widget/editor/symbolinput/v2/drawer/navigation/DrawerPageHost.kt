/*
 * This file is part of zero-Symbol-input-view-V2.
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

package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/**
 * Composition-native page host. Page content is composed by HorizontalPager;
 * off-screen composition and disposal are managed by Compose itself.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerPageHost(
    pages: List<DrawerPage>,
    selectedPageId: String?,
    onPageSelected: (DrawerPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = pages.indexOfFirst { it.id == selectedPageId }
        .coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceAtMost((pages.size - 1).coerceAtLeast(0)),
        pageCount = { pages.size }
    )

    LaunchedEffect(selectedPageId, pages.size) {
        if (pages.isNotEmpty()) {
            val target = pages.indexOfFirst { it.id == selectedPageId }
                .takeIf { it >= 0 } ?: 0
            if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pagerState.currentPage, pages) {
        pages.getOrNull(pagerState.currentPage)?.let(onPageSelected)
    }

    if (pages.isEmpty()) {
        Box(modifier.fillMaxSize())
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            key = { index -> pages[index].id }
        ) { index ->
            pages[index].content.content.invoke(this)
        }
    }
}
