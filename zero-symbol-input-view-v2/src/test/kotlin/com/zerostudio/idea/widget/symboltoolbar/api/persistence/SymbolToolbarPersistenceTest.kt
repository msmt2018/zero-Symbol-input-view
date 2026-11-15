package com.zerostudio.idea.widget.symboltoolbar.api.persistence

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolToolbarPersistenceTest {
    @Test
    fun exportsAndRestoresIndependentGestureListsAndPluginJson() {
        val store = InMemorySymbolToolbarDocumentStore()
        val persistence = SymbolToolbarPersistence(store)
        val payload = JsonObject().apply {
            addProperty("theme", "midnight")
            addProperty("density", 0.9)
        }
        val document = SymbolToolbarDocument(
            toolbarId = "editor.symbols",
            actions = listOf(
                PersistedSymbolItemActions(
                    itemId = "arrow",
                    click = listOf(PersistedSymbolAction("tap-1", "plugin.insert", JsonObject().apply {
                        addProperty("text", " -> ")
                    })),
                    longPress = listOf(PersistedSymbolAction("hold-1", "plugin.template"))
                )
            ),
            pluginData = listOf(PersistedPluginData("my.plugin", "my.plugin.settings", 1, payload))
        )

        persistence.save(document)
        val exported = persistence.export(document)
        val restored = persistence.restore("editor.symbols")

        assertTrue(exported.contains("\"schema\":\"zero.symbol-toolbar\""))
        assertTrue(restored is SymbolToolbarImportResult.Success)
        val actual = (restored as SymbolToolbarImportResult.Success).document
        assertEquals(document.toolbarId, actual.toolbarId)
        assertEquals("plugin.insert", actual.actions.single().click.single().actionId)
        assertEquals("plugin.template", actual.actions.single().longPress.single().actionId)
        assertEquals("midnight", actual.pluginData.single().payload.asJsonObject.get("theme").asString)
    }

    @Test
    fun unknownPluginPayloadIsPreservedForRoundTrip() {
        val persistence = SymbolToolbarPersistence(InMemorySymbolToolbarDocumentStore())
        val json = """{
          "schema":"zero.symbol-toolbar",
          "version":1,
          "toolbarId":"test",
          "actions":[],
          "pluginData":[{"pluginId":"missing.plugin","typeId":"missing.settings","version":3,"payload":{"x":7}}],
          "metadata":{}
        }"""
        val imported = persistence.import(json)
        assertTrue(imported is SymbolToolbarImportResult.Success)
        val success = imported as SymbolToolbarImportResult.Success
        assertEquals(1, success.unknownPluginData.size)
        assertEquals(7, success.unknownPluginData.single().payload.asJsonObject.get("x").asInt)
        assertTrue(persistence.export(success.document).contains("\"x\":7"))
    }

    @Test
    fun rejectsUnsupportedSchemaAndToolbarMismatch() {
        val persistence = SymbolToolbarPersistence(InMemorySymbolToolbarDocumentStore())
        val unsupported = persistence.import("""{"schema":"other","version":1,"toolbarId":"x"}""")
        assertTrue(unsupported is SymbolToolbarImportResult.Failure)
        val mismatch = persistence.import(
            """{"schema":"zero.symbol-toolbar","version":1,"toolbarId":"x","actions":[],"pluginData":[],"metadata":{}}""",
            expectedToolbarId = "y"
        )
        assertTrue(mismatch is SymbolToolbarImportResult.Failure)
    }

    @Test
    fun pluginCodecCanBeRegisteredAndDisposed() {
        data class Settings(val enabled: Boolean)
        val registry = SymbolToolbarJsonCodecRegistry()
        val codec = object : SymbolToolbarJsonCodec<Settings> {
            override val typeId = "test.settings"
            override val valueClass = Settings::class.java
            override fun encode(value: Settings, gson: Gson) = gson.toJsonTree(value)
            override fun decode(json: com.google.gson.JsonElement, gson: Gson) =
                gson.fromJson(json, Settings::class.java)
        }
        val registration = registry.register(codec)
        assertEquals(codec, registry.find("test.settings"))
        registration.close()
        assertEquals(null, registry.find("test.settings"))
    }
}
