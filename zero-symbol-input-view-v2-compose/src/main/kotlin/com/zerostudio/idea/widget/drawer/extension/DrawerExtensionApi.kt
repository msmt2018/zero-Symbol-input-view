package com.zerostudio.idea.widget.drawer.extension

typealias SymbolDrawerExtensionRegistry = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerExtensionRegistry
typealias SymbolDrawerExtensionSnapshot = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerExtensionSnapshot
typealias SymbolDrawerActionExtension = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerActionExtension
typealias SymbolGroupIndicatorExtension = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolGroupIndicatorExtension
typealias SymbolGroupIndicatorState = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolGroupIndicatorState
typealias SymbolDrawerSettingsTabExtension = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerSettingsTabExtension
typealias SymbolDrawerSettingExtension = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerSettingExtension
typealias SymbolDrawerSettingsScope = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerSettingsScope
typealias SymbolDrawerItemRenderer = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerItemRenderer
typealias SymbolDrawerItemRenderState = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerItemRenderState
typealias SymbolDrawerLegacyGroupProvider = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerLegacyGroupProvider
typealias LegacySymbolGroup = com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup
typealias LegacySymbolItem = com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem
typealias SymbolDrawerSettings = com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
typealias SymbolDrawerSettingsRepository = com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository
typealias SymbolActionHost = com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
typealias SymbolActionHandler<H> = com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHandler<H>

object SymbolDrawerBuiltInFeatures {
    const val SEARCH = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerBuiltInFeatures.SEARCH
    const val FAVORITES = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerBuiltInFeatures.FAVORITES
    const val RECENT = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerBuiltInFeatures.RECENT
    const val SETTINGS = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerBuiltInFeatures.SETTINGS
    const val MANAGER = com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerBuiltInFeatures.MANAGER
}
