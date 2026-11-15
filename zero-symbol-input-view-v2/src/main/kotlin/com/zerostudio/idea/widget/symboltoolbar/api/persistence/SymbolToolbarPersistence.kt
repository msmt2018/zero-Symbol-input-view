/*
 * Versioned JSON persistence for declarative symbol toolbar extensions.
 * Executable Kotlin lambdas are deliberately runtime-only; stable action IDs
 * and JSON arguments are persisted and rebound by installed plugins on restore.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.persistence

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

const val SYMBOL_TOOLBAR_JSON_SCHEMA = "zero.symbol-toolbar"
const val SYMBOL_TOOLBAR_JSON_VERSION = 1

/** JSON codec for plugin-owned settings. The codec must not serialize executable code. */
interface SymbolToolbarJsonCodec<T : Any> {
    val typeId: String
    val version: Int get() = 1
    val valueClass: Class<T>
    fun encode(value: T, gson: Gson): JsonElement
    fun decode(json: JsonElement, gson: Gson): T
}

/** Serializable action reference. Its implementation is resolved by actionId at runtime. */
data class PersistedSymbolAction(
    val id: String,
    val actionId: String,
    val arguments: JsonObject = JsonObject(),
    val enabled: Boolean = true
) {
    init {
        require(id.isNotBlank())
        require(actionId.isNotBlank())
    }
}

/** Both gesture lists are persisted independently and retain their order. */
data class PersistedSymbolItemActions(
    val itemId: String,
    val click: List<PersistedSymbolAction> = emptyList(),
    val longPress: List<PersistedSymbolAction> = emptyList()
) {
    init {
        require(itemId.isNotBlank())
        require(click.map { it.id }.distinct().size == click.size)
        require(longPress.map { it.id }.distinct().size == longPress.size)
    }
}

/** Namespaced plugin payload; unknown plugin payloads can be preserved during import/export. */
data class PersistedPluginData(
    val pluginId: String,
    val typeId: String,
    val version: Int,
    val payload: JsonElement
) {
    init {
        require(pluginId.isNotBlank() && typeId.isNotBlank() && version > 0)
    }
}

/** Complete portable snapshot. The host may add its own toolbar/group/item JSON payloads. */
data class SymbolToolbarDocument(
    val schema: String = SYMBOL_TOOLBAR_JSON_SCHEMA,
    val version: Int = SYMBOL_TOOLBAR_JSON_VERSION,
    val toolbarId: String,
    val actions: List<PersistedSymbolItemActions> = emptyList(),
    val pluginData: List<PersistedPluginData> = emptyList(),
    val metadata: JsonObject = JsonObject()
) {
    init {
        require(schema == SYMBOL_TOOLBAR_JSON_SCHEMA) { "Unsupported document schema: $schema" }
        require(version == SYMBOL_TOOLBAR_JSON_VERSION) { "Unsupported document version: $version" }
        require(toolbarId.isNotBlank())
        require(actions.map { it.itemId }.distinct().size == actions.size)
        require(pluginData.map { it.pluginId to it.typeId }.distinct().size == pluginData.size)
    }
}

sealed interface SymbolToolbarImportResult {
    data class Success(val document: SymbolToolbarDocument, val unknownPluginData: List<PersistedPluginData>) : SymbolToolbarImportResult
    data class Failure(val reason: String, val cause: Throwable? = null) : SymbolToolbarImportResult
}

/** Storage boundary keeps persistence independent from Activity, View and editor instances. */
interface SymbolToolbarDocumentStore {
    fun read(toolbarId: String): String?
    fun write(toolbarId: String, json: String)
    fun delete(toolbarId: String)
}

/** Register plugin codecs before importing. Duplicate codec IDs are rejected. */
class SymbolToolbarJsonCodecRegistry {
    private val codecs = ConcurrentHashMap<String, SymbolToolbarJsonCodec<out Any>>()

    fun <T : Any> register(codec: SymbolToolbarJsonCodec<T>): AutoCloseable {
        require(codec.typeId.isNotBlank())
        require(codec.version > 0)
        check(codecs.putIfAbsent(codec.typeId, codec) == null) {
            "JSON codec already registered: ${codec.typeId}"
        }
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) codecs.remove(codec.typeId, codec)
        }
    }

    fun find(typeId: String): SymbolToolbarJsonCodec<out Any>? = codecs[typeId]
    fun registeredTypes(): Set<String> = codecs.keys.toSortedSet()
}

/**
 * Coordinates validation, JSON import/export and automatic writes. Call save()
 * after changing declarative data; observe() can be used by controls/settings
 * screens to persist every committed snapshot.
 */
class SymbolToolbarPersistence(
    private val store: SymbolToolbarDocumentStore,
    val codecs: SymbolToolbarJsonCodecRegistry = SymbolToolbarJsonCodecRegistry(),
    private val gson: Gson = Gson()
) {
    private val observers = CopyOnWriteArrayList<(SymbolToolbarDocument) -> Unit>()

    fun export(document: SymbolToolbarDocument): String =
        gson.toJson(documentToJson(document))

    fun save(document: SymbolToolbarDocument) {
        val json = export(document)
        store.write(document.toolbarId, json)
        observers.forEach { listener -> runCatching { listener(document) } }
    }

    fun restore(toolbarId: String): SymbolToolbarImportResult {
        val json = store.read(toolbarId)
            ?: return SymbolToolbarImportResult.Failure("No saved toolbar document: $toolbarId")
        return import(json, expectedToolbarId = toolbarId)
    }

    fun import(json: String, expectedToolbarId: String? = null): SymbolToolbarImportResult =
        try {
            val root = JsonParser.parseString(json).asJsonObject
            val schema = root.get("schema")?.asString
                ?: return SymbolToolbarImportResult.Failure("Missing schema")
            val version = root.get("version")?.asInt
                ?: return SymbolToolbarImportResult.Failure("Missing version")
            if (schema != SYMBOL_TOOLBAR_JSON_SCHEMA) {
                return SymbolToolbarImportResult.Failure("Unsupported schema: $schema")
            }
            if (version != SYMBOL_TOOLBAR_JSON_VERSION) {
                return SymbolToolbarImportResult.Failure("Unsupported document version: $version")
            }
            val document = gson.fromJson(root, SymbolToolbarDocument::class.java)
            if (expectedToolbarId != null && document.toolbarId != expectedToolbarId) {
                return SymbolToolbarImportResult.Failure("Toolbar ID mismatch")
            }
            val unknown = document.pluginData.filter { codecs.find(it.typeId) == null }
            SymbolToolbarImportResult.Success(document, unknown)
        } catch (error: Throwable) {
            SymbolToolbarImportResult.Failure("Invalid toolbar JSON", error)
        }

    fun importAndSave(json: String, expectedToolbarId: String? = null): SymbolToolbarImportResult {
        val result = import(json, expectedToolbarId)
        if (result is SymbolToolbarImportResult.Success) save(result.document)
        return result
    }

    fun delete(toolbarId: String) = store.delete(toolbarId)

    fun observe(listener: (SymbolToolbarDocument) -> Unit): AutoCloseable {
        observers += listener
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) observers.remove(listener)
        }
    }

    private fun documentToJson(document: SymbolToolbarDocument): JsonObject {
        val root = gson.toJsonTree(document).asJsonObject
        // Explicitly preserve JsonElement payloads and metadata as native JSON.
        root.add("metadata", document.metadata.deepCopy())
        val plugins = JsonArray()
        document.pluginData.forEach { item ->
            plugins.add(JsonObject().apply {
                addProperty("pluginId", item.pluginId)
                addProperty("typeId", item.typeId)
                addProperty("version", item.version)
                add("payload", item.payload.deepCopy())
            })
        }
        root.add("pluginData", plugins)
        return root
    }
}

/** Small JVM-testable store; Android hosts can implement the same contract with DataStore/Room. */
class InMemorySymbolToolbarDocumentStore : SymbolToolbarDocumentStore {
    private val values = ConcurrentHashMap<String, String>()
    override fun read(toolbarId: String): String? = values[toolbarId]
    override fun write(toolbarId: String, json: String) { values[toolbarId] = json }
    override fun delete(toolbarId: String) { values.remove(toolbarId) }
}
