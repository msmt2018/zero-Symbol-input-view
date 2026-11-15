/*
 * Scoped extension registry for independent symbol toolbar APIs.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.extension

import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry keyed by stable namespaced IDs.
 * Registration returns a disposable handle; replacing an existing ID is
 * rejected to avoid silently changing another extension's behavior.
 */
class SymbolToolbarExtensionRegistry {
    private val handlers = ConcurrentHashMap<String, SymbolToolbarActionHandler>()

    fun registerAction(
        id: String,
        handler: SymbolToolbarActionHandler
    ): AutoCloseable {
        require(id.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9_.-]{1,127}"))) {
            "extension action ID must be a stable namespaced identifier"
        }
        check(handlers.putIfAbsent(id, handler) == null) {
            "action handler already registered: $id"
        }
        return AutoCloseable { handlers.remove(id, handler) }
    }

    fun findAction(id: String): SymbolToolbarActionHandler? = handlers[id]

    fun registeredActionIds(): Set<String> = handlers.keys.toSortedSet()

    fun clear() = handlers.clear()
}
