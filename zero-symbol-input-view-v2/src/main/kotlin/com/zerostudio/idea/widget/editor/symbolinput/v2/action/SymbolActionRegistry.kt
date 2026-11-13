/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.action

/**
 * Stable action identity. The namespace separates built-ins, imported numeric
 * V1 IDs, app actions and plugin actions without changing persisted V1 numbers.
 */
data class SymbolActionId(val namespace: String, val value: String) {
    init {
        require(namespace.matches(ID_PART)) { "Invalid action namespace" }
        require(value.matches(ID_PART)) { "Invalid action ID" }
    }

    companion object {
        private val ID_PART = Regex("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")

        @JvmStatic fun legacy(numericId: Int) =
            SymbolActionId("legacy", numericId.toString())

        @JvmStatic fun extension(extensionId: String) =
            SymbolActionId("extension", extensionId)
    }
}

/** Runtime method registered for an action ID. The context is host-defined. */
fun interface SymbolActionHandler<C> {
    fun execute(context: C, payload: String?): Boolean
}

/**
 * Context-scoped reverse lookup from stable IDs to executable functions.
 *
 * The registry stores function references only for the current process. Persist
 * SymbolActionId plus payload, never a lambda; after recreation the host
 * re-registers its handlers. A registry should be owned by the editor/session
 * context that the handlers operate on, not by a process-global singleton.
 */
class SymbolActionRegistry<C> {
    private data class Entry<C>(
        val token: Any,
        val handler: SymbolActionHandler<C>
    )

    private val lock = Any()
    private val entries = LinkedHashMap<SymbolActionId, Entry<C>>()

    /** Register or replace a handler. Closing the returned handle removes only this registration. */
    fun register(
        id: SymbolActionId,
        handler: SymbolActionHandler<C>
    ): AutoCloseable {
        val token = Any()
        synchronized(lock) {
            entries[id] = Entry(token, handler)
        }
        var closed = false
        return AutoCloseable {
            synchronized(lock) {
                if (!closed) {
                    closed = true
                    if (entries[id]?.token === token) entries.remove(id)
                }
            }
        }
    }

    /** Convenience overload preserving existing numeric V1 action IDs. */
    fun register(
        legacyNumericId: Int,
        handler: SymbolActionHandler<C>
    ): AutoCloseable = register(SymbolActionId.legacy(legacyNumericId), handler)

    /** Reverse lookup: obtain the function registered for an ID. */
    fun get(id: SymbolActionId): SymbolActionHandler<C>? =
        synchronized(lock) { entries[id]?.handler }

    fun get(legacyNumericId: Int): SymbolActionHandler<C>? =
        get(SymbolActionId.legacy(legacyNumericId))

    /** Returns null only when no handler exists; false means the handler declined. */
    fun execute(id: SymbolActionId, context: C, payload: String? = null): Boolean? =
        get(id)?.execute(context, payload)

    fun execute(legacyNumericId: Int, context: C, payload: String? = null): Boolean? =
        execute(SymbolActionId.legacy(legacyNumericId), context, payload)

    fun unregister(id: SymbolActionId): Boolean = synchronized(lock) {
        entries.remove(id) != null
    }

    fun clear() = synchronized(lock) { entries.clear() }

    val size: Int
        get() = synchronized(lock) { entries.size }
}
