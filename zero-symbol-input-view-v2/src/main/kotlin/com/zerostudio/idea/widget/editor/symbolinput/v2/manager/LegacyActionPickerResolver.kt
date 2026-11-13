package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionAdapter
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionCatalog
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction

/**
 * Resolves a V1 picker entry only when the persisted legacy ID + payload still
 * decode to the exact typed action. A stale V1 hint must never replace a newer
 * V2 action merely because both happen to share the same action class.
 */
internal object LegacyActionPickerResolver {
    fun legacyActionIndex(
        action: SymbolAction?,
        legacyId: Int?,
        legacyPayload: String?
    ): Int? {
        if (action == null) return null
        // Historical clipboard IDs and retired function-key placeholders remain
        // losslessly preserved, but are not offered as new picker choices.
        if (legacyId == 1 || legacyId == 2) return null
        if (legacyId != null) {
            val index = LegacySymbolActionCatalog.values.indexOf(legacyId)
            if (index < 0) return null
            val decoded = LegacySymbolActionAdapter.convert(
                LegacySymbolAction(legacyId, legacyPayload)
            )
            return index.takeIf { decoded == action }
        }
        return when (action) {
            is SymbolAction.InsertText ->
                LegacySymbolActionCatalog.values.indexOf(0).takeIf { it >= 0 }
            is SymbolAction.EditorCommand ->
                if (action.commandId == "line.replace") {
                    LegacySymbolActionCatalog.values.indexOf(36).takeIf { it >= 0 }
                } else {
                    LegacySymbolActionCatalog.values.indexOfFirst { id ->
                        LegacySymbolActionAdapter.convert(LegacySymbolAction(id)) == action
                    }.takeIf { it >= 0 }
                }
            else -> LegacySymbolActionCatalog.values.indexOfFirst { id ->
                LegacySymbolActionAdapter.convert(LegacySymbolAction(id)) == action
            }.takeIf { it >= 0 }
        }
    }
}
