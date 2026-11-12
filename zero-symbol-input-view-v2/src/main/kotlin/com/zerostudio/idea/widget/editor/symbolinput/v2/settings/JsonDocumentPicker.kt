package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lifecycle-bound Storage Access Framework picker for JSON import/export.
 * The host Activity remains in control of document access; no storage
 * permission or direct filesystem path is required.
 */
internal class JsonDocumentPicker(
    context: Context,
    private val onImported: (String) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private val activity = context.findComponentActivity()
    private var pendingExport: String? = null
    private val openLauncher: ActivityResultLauncher<Array<String>>?
    private val saveLauncher: ActivityResultLauncher<String>?

    init {
        val host = activity
        if (host == null) {
            openLauncher = null
            saveLauncher = null
        } else {
            val suffix = nextId.incrementAndGet()
            val registry = host.activityResultRegistry
            // Use the registry overload without a LifecycleOwner: settings
            // dialogs are commonly opened after the Activity is already
            // RESUMED, when lifecycle-bound registration is no longer legal.
            openLauncher = registry.register(
                "zero-symbol-json-open-$suffix",
                ActivityResultContracts.OpenDocument()
            ) { uri -> if (uri != null) read(uri) }
            saveLauncher = registry.register(
                "zero-symbol-json-save-$suffix",
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri -> if (uri != null) write(uri) }
            host.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        openLauncher?.unregister()
                        saveLauncher?.unregister()
                        source.lifecycle.removeObserver(this)
                    }
                }
            })
        }
    }

    fun openJson() {
        val launcher = openLauncher ?: error("JSON file picker requires a ComponentActivity host")
        launcher.launch(arrayOf("application/json", "text/json", "text/plain"))
    }

    fun saveJson(fileName: String, json: String) {
        pendingExport = json
        val launcher = saveLauncher ?: error("JSON file picker requires a ComponentActivity host")
        launcher.launch(fileName)
    }

    private fun read(uri: Uri) {
        runCatching {
            val input = activity?.contentResolver?.openInputStream(uri)
                ?: error("Unable to open selected JSON file")
            input.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_IMPORT_BYTES) { "Selected JSON file exceeds the 16 MiB import limit" }
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
        }.onSuccess(onImported).onFailure(onError)
    }

    private fun write(uri: Uri) {
        runCatching {
            val payload = pendingExport ?: error("No JSON export is pending")
            val output = activity?.contentResolver?.openOutputStream(uri, "wt")
                ?: error("Unable to create selected JSON file")
            output.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }
            pendingExport = null
        }.onSuccess { }.onFailure(onError)
    }

    private fun Context.findComponentActivity(): ComponentActivity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is ComponentActivity) return current
            current = current.baseContext
        }
        return current as? ComponentActivity
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 16 * 1024 * 1024
        val nextId = AtomicInteger()
    }
}
