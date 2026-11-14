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

/**
 * 默认符号数据提供器。
 *
 * 用于在首次启动或数据损坏时构建可用的回退分组集合。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
object SymbolDefaults {

    /**
     * 创建内置默认分组。
     */
    fun createFallbackGroups(): MutableList<SymbolGroup> {
        return mutableListOf(
            SymbolGroup(
                "commonlyUsed", mutableListOf(
                    SymbolItem(18, "←", null, 16, null),
                    SymbolItem(20, "↑", null, 23, null),
                    SymbolItem(19, "→", null, 17, null),
                    SymbolItem(0, "\"", "\""),
                    SymbolItem(0, "'", "'"),
                    SymbolItem(0, ".", "."),
                    SymbolItem(0, ",", ","),
                    SymbolItem(0, "/", "/"),
                    SymbolItem(0, "//", "//"),
                    SymbolItem(0, ":", ":"),
                    SymbolItem(0, ";", ";"),
                    SymbolItem(21, "↓", null, 24, null),
                    SymbolItem(0, "#", "#"),
                    SymbolItem(0, "+", "+"),
                    SymbolItem(0, "-", "-"),
                    SymbolItem(0, "*", "*"),
                    SymbolItem(0, "=", "="),
                    SymbolItem(0, "|", "|"),
                    SymbolItem(0, "~", "~"),
                    SymbolItem(0, "(", "("),
                    SymbolItem(0, ")", ")"),
                    SymbolItem(0, "(\"", "(\""),
                    SymbolItem(0, "\")", "\")"),
                    SymbolItem(0, "{", "{"),
                    SymbolItem(0, "}", "}"),
                    SymbolItem(0, "()", "()", 0, "()"),
                    SymbolItem(0, "[]", "[]", 0, "[]"),
                    SymbolItem(0, "{}", "{}", 0, "{}"),
                    SymbolItem(0, "<", "<"),
                    SymbolItem(0, ">", ">"),
                    SymbolItem(0, "\\", "\\"),
                    SymbolItem(0, "$", "$"),
                    SymbolItem(0, "&", "&"),
                    SymbolItem(0, "/*", "/**"),
                    SymbolItem(0, "*/", "*/"),
                    SymbolItem(0, "\n", "\n"),
                    SymbolItem(0, "\t", "\t"),
                    SymbolItem(0, "\\", "\\"),
                    SymbolItem(22, "settings")
                )
            )
        )
    }

    /**
     * 对分组数据执行深拷贝，避免引用共享带来的副作用。
     */
    fun deepCopy(groups: List<SymbolGroup>): MutableList<SymbolGroup> {
        return groups.map { group ->
            SymbolGroup(
                group.name,
                group.items.map { item ->
                    SymbolItem(item.shortAction, item.display, item.shortText, item.longAction, item.longText)
                }.toMutableList()
            )
        }.toMutableList()
    }
}
