package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerSettingsTest {
    @Test fun settingsCodecRoundTripsAllValues() {
        val value = SymbolDrawerSettings(
            columns = 6,
            themeMode = ThemeMode.DARK,
            showActionHints = true,
            reduceMotion = true,
            pageCacheLimit = 5
        )
        assertEquals(value, SymbolDrawerSettingsCodec.decode(SymbolDrawerSettingsCodec.encode(value)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingFieldsInSettingsBackup() {
        SymbolDrawerSettingsCodec.decode("""{"schemaVersion":1,"columns":7}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownSettingsFields() {
        val json = SymbolDrawerSettingsCodec.encode(SymbolDrawerSettings()).dropLast(1) + ""","unknown":true}"""
        SymbolDrawerSettingsCodec.decode(json)
    }

    @Test fun drawerConfigHonorsReducedMotion() {
        val config = SymbolDrawerSettings(animationDurationMs = 350, reduceMotion = true).toDrawerConfig()
        assertEquals(0L, config.animationDurationMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeColumns() {
        SymbolDrawerSettings(columns = 0)
    }
}
