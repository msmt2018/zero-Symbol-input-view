/*
 * Android SharedPreferences-backed document store.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.persistence

import android.content.Context
import android.content.SharedPreferences

/**
 * Durable local storage implementation. Use applicationContext so the store
 * never retains an Activity. JSON writes are committed synchronously to ensure
 * save() reports persistence failures to its caller.
 */
class SharedPreferencesSymbolToolbarDocumentStore(
    context: Context,
    preferencesName: String = "zero_symbol_toolbar_documents"
) : SymbolToolbarDocumentStore {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun read(toolbarId: String): String? = preferences.getString(key(toolbarId), null)

    override fun write(toolbarId: String, json: String) {
        check(preferences.edit().putString(key(toolbarId), json).commit()) {
            "Failed to persist symbol toolbar document: $toolbarId"
        }
    }

    override fun delete(toolbarId: String) {
        check(preferences.edit().remove(key(toolbarId)).commit()) {
            "Failed to delete symbol toolbar document: $toolbarId"
        }
    }

    private fun key(toolbarId: String): String {
        require(toolbarId.isNotBlank()) { "toolbarId must not be blank" }
        return "toolbar:$toolbarId"
    }
}
