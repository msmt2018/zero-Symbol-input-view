/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/
package io.github.rosemoe.sora.app


import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolDrawerDefaultGroups
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.AndroidLegacySymbolDataMigration
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerManagerScreen
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsScreen
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository

import android.content.DialogInterface
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.write
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.dingyi222666.monarch.languages.JavaLanguage
import io.github.dingyi222666.monarch.languages.KotlinLanguage
import io.github.dingyi222666.monarch.languages.PythonLanguage
import io.github.dingyi222666.monarch.languages.TypescriptLanguage
import io.github.rosemoe.sora.app.databinding.ActivityMainBinding
import io.github.rosemoe.sora.app.lsp.LspTestActivity
import io.github.rosemoe.sora.app.lsp.LspTestJavaActivity
import io.github.rosemoe.sora.app.tests.TestActivity
import io.github.rosemoe.sora.app.tests.paged.PagedEditActivity
import io.github.rosemoe.sora.app.tests.drawer.DrawerApiLabActivity
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.InlayHintClickEvent
import io.github.rosemoe.sora.event.KeyBindingEvent
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SideIconClickEvent
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.JavaLanguageSpec
import io.github.rosemoe.sora.lang.TsLanguageJava
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.styling.color.ConstColor
import io.github.rosemoe.sora.lang.styling.inlayHint.ColorInlayHint
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.TextInlayHint
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.monarch.MonarchColorScheme
import io.github.rosemoe.sora.langs.monarch.MonarchLanguage
import io.github.rosemoe.sora.langs.monarch.registry.MonarchGrammarRegistry
import io.github.rosemoe.sora.langs.monarch.registry.dsl.monarchLanguages
import io.github.rosemoe.sora.langs.monarch.registry.model.ThemeSource
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.text.ContentIO
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.util.regex.RegexBackrefGrammar
import io.github.rosemoe.sora.utils.CrashHandler
import io.github.rosemoe.sora.utils.codePointStringAt
import io.github.rosemoe.sora.utils.escapeCodePointIfNecessary
import io.github.rosemoe.sora.utils.toast
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions
import io.github.rosemoe.sora.widget.SelectionMovement
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.ext.EditorSpanInteractionHandler
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub
import io.github.rosemoe.sora.widget.schemes.SchemeNotepadXX
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.zip.ZipFile
import java.util.regex.PatternSyntaxException
import com.zerostudio.idea.widget.drawer.action.SymbolActionExecutor
import com.zerostudio.idea.widget.drawer.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SharedPreferencesSymbolDrawerSettingsRepository


/**
 * Demo and debug Activity for the code editor
 */
class MainActivity : AppCompatActivity() {

    companion object {
        init {
            // Load tree-sitter libraries
            System.loadLibrary("android-tree-sitter")
            System.loadLibrary("tree-sitter-java")
            AsyncIncrementalAnalyzeManager.setUseShallowCopyByDefault(true)
        }

        private const val TAG = "MainActivity"
        const val LOG_FILE = "crash-journal.log"

        /**
         * Symbols to be displayed in symbol input view
         */
        val SYMBOLS = arrayOf(
            "->", "{", "}", "(", ")",
            ",", ".", ";", "\"", "?",
            "+", "-", "*", "/", "<",
            ">", "[", "]", ":"
        )

        /**
         * Texts to be committed to editor for symbols above
         */
        val SYMBOL_INSERT_TEXT = arrayOf(
            "\t", "{}", "}", "(", ")",
            ",", ".", ";", "\"", "?",
            "+", "-", "*", "/", "<",
            ">", "[", "]", ":"
        )

    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var searchMenu: PopupMenu
    private var searchOptions = SearchOptions(false, false)
    private var undo: MenuItem? = null
    private var redo: MenuItem? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.INSTANCE.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)

        // The XML binding is retained only as a source for the existing CodeEditor
        // widget and non-UI compatibility helpers. The Activity content itself is
        // rendered by Compose; no XML root or legacy toolbar is attached.
        (binding.editor.parent as? android.view.ViewGroup)?.removeView(binding.editor)
        searchMenu = PopupMenu(this, View(this)).apply {
            inflate(R.menu.menu_search_options)
            setOnMenuItemClickListener { item ->
                item.isChecked = !item.isChecked
                if (item.isChecked) {
                    when (item.itemId) {
                        R.id.search_option_regex ->
                            menu.findItem(R.id.search_option_whole_word)?.isChecked = false
                        R.id.search_option_whole_word ->
                            menu.findItem(R.id.search_option_regex)?.isChecked = false
                    }
                }
                computeSearchOptions()
                tryCommitSearch()
                true
            }
        }

        val editor = binding.editor
        val typeface = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")
        editor.apply {
            registerInlayHintRenderers(
                TextInlayHintRenderer.DefaultInstance,
                ColorInlayHintRenderer.DefaultInstance
            )
            typefaceText = typeface
            props.stickyScroll = true
            setLineSpacing(2f, 1.1f)
            nonPrintablePaintingFlags =
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or
                    CodeEditor.FLAG_DRAW_LINE_SEPARATOR or
                    CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION or
                    CodeEditor.FLAG_DRAW_SOFT_WRAP
            subscribeAlways<SelectionChangeEvent> { updatePositionText() }
            subscribeAlways<PublishSearchResultEvent> { updatePositionText() }
            subscribeAlways<ContentChangeEvent> {
                postDelayedInLifecycle(::updateBtnState, 50)
            }
            subscribeAlways<SideIconClickEvent> { toast(R.string.tip_side_icon) }
            subscribeAlways<InlayHintClickEvent> { toast(R.string.tip_inlay_hint) }
            subscribeAlways<TextSizeChangeEvent> { event ->
                Log.d(TAG, "TextSizeChangeEvent: " + event.oldTextSize + " -> " + event.newTextSize)
            }
            subscribeAlways<KeyBindingEvent> { event ->
                if (event.eventType == EditorKeyEvent.Type.DOWN) {
                    toast("Keybinding event: " + generateKeybindingString(event), Toast.LENGTH_LONG)
                }
            }
            searcher.replaceOptions = EditorSearcher.ReplaceOptions(true)
            EditorSpanInteractionHandler(this)
            getComponent<EditorAutoCompletion>().setEnabledAnimation(true)
        }

        setupTextmate()
        setupMonarch()
        ensureTextmateTheme()
        editor.setEditorLanguage(TextMateLanguage.create("source.java", true))

        val savedText = savedInstanceState?.getString("text")
        if (savedText != null) {
            val textSize = savedInstanceState.getFloat("font.size")
            if (textSize > 0f) editor.textSizePx = textSize
            editor.setText(savedText)
            val left = savedInstanceState.getInt("position.left").coerceIn(0, editor.text.length)
            val right = savedInstanceState.getInt("position.right").coerceIn(0, editor.text.length)
            val leftPos = editor.text.indexer.getCharPosition(left.coerceAtMost(right))
            val rightPos = editor.text.indexer.getCharPosition(right.coerceAtLeast(left))
            editor.setSelectionRegion(leftPos.line, leftPos.column, rightPos.line, rightPos.column, false)
            editor.scroller.startScroll(savedInstanceState.getInt("scroll.x"), savedInstanceState.getInt("scroll.y"), 0, 0, 0)
            editor.scroller.abortAnimation()
            editor.postInvalidate()
        } else {
            openAssetsFile("samples/sample.txt")
        }
        updatePositionText()
        updateBtnState()
        switchThemeIfRequired(this, editor)
        computeSearchOptions()

        val actionExecutor = SymbolActionExecutor()
        val actionHost = SoraCodeEditorSymbolActionHost(editor) {
            // The Compose drawer routes the built-in OpenManager action to its
            // native manager surface; this host callback remains a safe fallback.
            Toast.makeText(this, "请从符号抽屉打开符号管理", Toast.LENGTH_SHORT).show()
        }
        val symbolGroupRepository = SharedPreferencesSymbolGroupRepository(this)
        AndroidLegacySymbolDataMigration.importIfV2Empty(this, symbolGroupRepository)
        val symbolSettingsRepository = SharedPreferencesSymbolDrawerSettingsRepository(this)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            androidx.compose.material3.MaterialTheme {
                MainActivityComposeContent(
                    editor = editor,
                    onSearchChanged = { query ->
                        binding.searchEditor.setText(query)
                        tryCommitSearch()
                    },
                    onReplaceChanged = { replacement -> binding.replaceEditor.setText(replacement) },
                    onNext = { runCatching { editor.searcher.gotoNext() } },
                    onPrevious = { runCatching { editor.searcher.gotoPrevious() } },
                    onReplace = { runCatching { editor.searcher.replaceCurrentMatch(binding.replaceEditor.text.toString()) } },
                    onReplaceAll = { runCatching { editor.searcher.replaceAll(binding.replaceEditor.text.toString()) } },
                    onUndo = { editor.undo(); updateBtnState() },
                    onRedo = { editor.redo(); updateBtnState() },
                    onFormat = { editor.formatCodeAsync() },
                    onChooseLanguage = ::chooseLanguage,
                    onChooseTheme = ::chooseTheme,
                    onOpenLogs = ::openLogs,
                    onLoadLargeFile = { openAssetsFile("samples/big_sample.txt") },
                    onMenuAction = { itemId ->
                        val menuHost = PopupMenu(this, View(this))
                        menuHost.inflate(R.menu.menu_main)
                        menuHost.menu.findItem(itemId)?.let(::onOptionsItemSelected)
                    },
                    groupRepository = symbolGroupRepository,
                    settingsRepository = symbolSettingsRepository,
                    onSymbolAction = { symbol ->
                        val result = actionExecutor.execute(symbol.shortAction, actionHost)
                        if (result !is SymbolActionResult.Executed) {
                            Toast.makeText(this, "符号动作执行失败：" + result, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    /**
     * Generate new [SearchOptions] for text searching in editor
     */
    private fun computeSearchOptions() {
        val caseInsensitive = !searchMenu.menu.findItem(R.id.search_option_match_case)!!.isChecked
        var type = SearchOptions.TYPE_NORMAL
        val regex = searchMenu.menu.findItem(R.id.search_option_regex)!!.isChecked
        if (regex) {
            type = SearchOptions.TYPE_REGULAR_EXPRESSION
        }
        val wholeWord = searchMenu.menu.findItem(R.id.search_option_whole_word)!!.isChecked
        if (wholeWord) {
            type = SearchOptions.TYPE_WHOLE_WORD
        }
        searchOptions = SearchOptions(type, caseInsensitive, RegexBackrefGrammar.DEFAULT)
    }

    /**
     * Commit a text search to editor
     */
    private fun tryCommitSearch() {
        val query = binding.searchEditor.editableText
        if (query.isNotEmpty()) {
            try {
                binding.editor.searcher.search(
                    query.toString(),
                    searchOptions
                )
            } catch (e: PatternSyntaxException) {
                // Regex error
            }
        } else {
            binding.editor.searcher.stopSearch()
        }
    }

    /**
     * Setup Textmate. Load our grammars and themes from assets
     */
    private fun setupTextmate() {
        // Add assets file provider so that files in assets can be loaded
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                applicationContext.assets // use application context
            )
        )
        loadDefaultTextMateThemes()
        loadDefaultTextMateLanguages()
    }


    /**
     * Load default textmate themes
     */
    private /*suspend*/ fun loadDefaultTextMateThemes() /*= withContext(Dispatchers.IO)*/ {
        val themes = arrayOf("darcula", "ayu-dark", "quietlight", "solarized_dark")
        val themeRegistry = ThemeRegistry.getInstance()
        themes.forEach { name ->
            val path = "textmate/$name.json"
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                    ), name
                ).apply {
                    if (name != "quietlight") {
                        isDark = true
                    }
                }
            )
        }

        themeRegistry.setTheme("quietlight")
    }

    /**
     * Load default languages from JSON configuration
     *
     * @see loadDefaultLanguagesWithDSL Load by Kotlin DSL
     */
    private /*suspend*/ fun loadDefaultTextMateLanguages() /*= withContext(Dispatchers.Main)*/ {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    /**
     * Setup monarch. Load our grammars and themes from assets
     */
    private fun setupMonarch() {
        // Add assets file provider so that files in assets can be loaded
        io.github.rosemoe.sora.langs.monarch.registry.FileProviderRegistry.addProvider(
            io.github.rosemoe.sora.langs.monarch.registry.provider.AssetsFileResolver(
                applicationContext.assets // use application context
            )
        )
        loadDefaultMonarchThemes()
        loadDefaultMonarchLanguages()
    }


    /**
     * Load default monarch themes
     *
     */
    private /*suspend*/ fun loadDefaultMonarchThemes() /*= withContext(Dispatchers.IO)*/ {
        val themes = arrayOf("darcula", "ayu-dark", "quietlight", "solarized_dark")

        themes.forEach { name ->
            val path = "textmate/$name.json"
            io.github.rosemoe.sora.langs.monarch.registry.ThemeRegistry.loadTheme(
                io.github.rosemoe.sora.langs.monarch.registry.model.ThemeModel(
                    ThemeSource(path, name)
                ).apply {
                    if (name != "quietlight") {
                        isDark = true
                    }
                }, false
            )
        }

        io.github.rosemoe.sora.langs.monarch.registry.ThemeRegistry.setTheme("quietlight")
    }

    /**
     * Load default languages from Monarch
     */
    private fun loadDefaultMonarchLanguages() {
        MonarchGrammarRegistry.INSTANCE.loadGrammars(
            monarchLanguages {
                language("java") {
                    monarchLanguage = JavaLanguage
                    defaultScopeName()
                    languageConfiguration = "textmate/java/language-configuration.json"
                }
                language("kotlin") {
                    monarchLanguage = KotlinLanguage
                    defaultScopeName()
                    languageConfiguration = "textmate/kotlin/language-configuration.json"
                }
                language("python") {
                    monarchLanguage = PythonLanguage
                    defaultScopeName()
                    languageConfiguration = "textmate/python/language-configuration.json"
                }
                language("typescript") {
                    monarchLanguage = TypescriptLanguage
                    defaultScopeName()
                    languageConfiguration = "textmate/javascript/language-configuration.json"
                }
            }
        )
    }

    private fun loadDefaultLanguagesWithDSL() {
        GrammarRegistry.getInstance().loadGrammars(
            languages {
                language("java") {
                    grammar = "textmate/java/syntaxes/java.tmLanguage.json"
                    defaultScopeName()
                    languageConfiguration = "textmate/java/language-configuration.json"
                }
                language("kotlin") {
                    grammar = "textmate/kotlin/syntaxes/Kotlin.tmLanguage"
                    defaultScopeName()
                    languageConfiguration = "textmate/kotlin/language-configuration.json"
                }
                language("python") {
                    grammar = "textmate/python/syntaxes/python.tmLanguage.json"
                    defaultScopeName()
                    languageConfiguration = "textmate/python/language-configuration.json"
                }
            }
        )
    }

    /**
     * Re-apply color scheme
     */
    private fun resetColorScheme() {
        binding.editor.apply {
            val colorScheme = this.colorScheme
            // reset
            this.colorScheme = colorScheme
        }
    }

    /**
     * Add diagnostic items to editor. For debug only.
     */
    private fun setupDiagnostics() {
        val editor = binding.editor
        val container = DiagnosticsContainer()
        for (i in 0 until editor.text.lineCount) {
            val index = editor.text.getCharIndex(i, 0)
            container.addDiagnostic(
                DiagnosticRegion(
                    index,
                    index + editor.text.getColumnCount(i),
                    DiagnosticRegion.SEVERITY_ERROR
                )
            )
        }
        editor.diagnostics = container
    }

    /**
     * Ensure the editor uses a [TextMateColorScheme]
     */
    private fun ensureTextmateTheme() {
        val editor = binding.editor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is TextMateColorScheme) {
            editorColorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.colorScheme = editorColorScheme
        }
    }

    /**
     * Ensure the editor uses a [MonarchColorScheme]
     */
    private fun ensureMonarchTheme() {
        val editor = binding.editor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is MonarchColorScheme) {
            editorColorScheme =
                MonarchColorScheme.create(io.github.rosemoe.sora.langs.monarch.registry.ThemeRegistry.currentTheme)
            editor.colorScheme = editorColorScheme
            switchThemeIfRequired(this, editor)
        }
    }

    private fun generateKeybindingString(event: KeyBindingEvent): String {
        val sb = StringBuilder()
        if (event.isCtrlPressed) {
            sb.append("Ctrl + ")
        }

        if (event.isAltPressed) {
            sb.append("Alt + ")
        }

        if (event.isShiftPressed) {
            sb.append("Shift + ")
        }

        sb.append(KeyEvent.keyCodeToString(event.keyCode))
        return sb.toString()
    }

    /**
     * Open file from assets, and set editor text
     */
    private fun openAssetsFile(name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val text = ContentIO.createFrom(assets.open(name))
            withContext(Dispatchers.Main) {
                binding.editor.setText(text, null)

                updatePositionText()
                updateBtnState()

                if ("big_sample" !in name) {
                    binding.editor.inlayHints = InlayHintsContainer().also {
                        it.add(ColorInlayHint(10, 30, ConstColor("#f44336")))
                        it.add(TextInlayHint(29, 7, "^DigitTens"))
                        it.add(TextInlayHint(100, 1, "^Numbers"))
                    }
                }
            }
        }
    }

    /**
     * Update buttons state for undo/redo
     */
    private fun updateBtnState() {
        undo?.isEnabled = binding.editor.canUndo()
        redo?.isEnabled = binding.editor.canRedo()
    }

    /**
     * Update editor position tracker text
     */
    private fun updatePositionText() {
        val cursor = binding.editor.cursor
        var text =
            (1 + cursor.leftLine).toString() + ":" + cursor.leftColumn + ";" + cursor.left + " "

        text += if (cursor.isSelected) {
            "(" + (cursor.right - cursor.left) + " chars)"
        } else {
            val content = binding.editor.text
            if (content.getColumnCount(cursor.leftLine) == cursor.leftColumn) {
                "(<" + content.getLine(cursor.leftLine).lineSeparator.let {
                    if (it == LineSeparator.NONE) {
                        "EOF"
                    } else {
                        it.name
                    }
                } + ">)"
            } else {
                "(" + content.getLine(cursor.leftLine)
                    .codePointStringAt(cursor.leftColumn)
                    .escapeCodePointIfNecessary() + ")"
            }
        }

        // Indicator for text matching
        val searcher = binding.editor.searcher
        if (searcher.hasQuery()) {
            val idx = searcher.currentMatchedPositionIndex
            val count = searcher.matchedPositionCount
            val matchText = if (count == 0) {
                "no match"
            } else if (count == 1) {
                "1 match"
            } else {
                "$count matches"
            }
            text += if (idx == -1) {
                "($matchText)"
            } else {
                "(${idx + 1} of $matchText)"
            }
        }

        binding.positionDisplay.text = text
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.write {
            putString("text", binding.editor.text.toString())
            putFloat("font.size", binding.editor.textSizePx)
            putInt("position.left", binding.editor.cursor.left)
            putInt("position.right", binding.editor.cursor.right)
            putInt("scroll.x", binding.editor.offsetX)
            putInt("scroll.y", binding.editor.offsetY)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        undo = menu.findItem(R.id.text_undo)
        redo = menu.findItem(R.id.text_redo)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.editor.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val editor = binding.editor
        when (id) {
            R.id.open_drawer_api_lab -> startActivity(Intent(this, DrawerApiLabActivity::class.java))
            R.id.open_test_activity -> startActivity<TestActivity>()
            R.id.open_paged_edit -> startActivity<PagedEditActivity>()
            R.id.open_lsp_activity -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.not_supported))
                        .setMessage(getString(R.string.dialog_api_warning_msg))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.dialog_lsp_entry_title)
                        .setMessage(R.string.dialog_lsp_entry_msg)
                        .setPositiveButton(R.string.choice_yes) { _, _ ->
                            startActivity<LspTestActivity>()
                        }
                        .setNegativeButton(R.string.choice_no) { _, _ ->
                            startActivity<LspTestJavaActivity>()
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .show()
                }
            }

            R.id.text_undo -> editor.undo()
            R.id.text_redo -> editor.redo()
            R.id.goto_end -> editor.setSelection(
                editor.text.lineCount - 1,
                editor.text.getColumnCount(editor.text.lineCount - 1)
            )

            R.id.move_up -> editor.moveSelection(SelectionMovement.UP)
            R.id.move_down -> editor.moveSelection(SelectionMovement.DOWN)
            R.id.home -> editor.moveSelection(SelectionMovement.LINE_START)
            R.id.end -> editor.moveSelection(SelectionMovement.LINE_END)
            R.id.move_left -> editor.moveSelection(SelectionMovement.LEFT)
            R.id.move_right -> editor.moveSelection(SelectionMovement.RIGHT)
            R.id.magnifier -> {
                item.isChecked = !item.isChecked
                editor.getComponent(Magnifier::class.java).isEnabled = item.isChecked
            }

            R.id.useIcu -> {
                item.isChecked = !item.isChecked
                editor.props.useICULibToSelectWords = item.isChecked
            }

            R.id.ln_panel_fixed -> chooseLineNumberPanelPosition()

            R.id.ln_panel_follow -> {
                val themes = arrayOf(
                    getString(R.string.top),
                    getString(R.string.center),
                    getString(R.string.bottom)
                )
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.fixed)
                    .setSingleChoiceItems(themes, -1) { dialog: DialogInterface, which: Int ->
                        editor.lnPanelPositionMode = LineInfoPanelPositionMode.FOLLOW
                        when (which) {
                            0 -> editor.lnPanelPosition = LineInfoPanelPosition.TOP
                            1 -> editor.lnPanelPosition = LineInfoPanelPosition.CENTER
                            2 -> editor.lnPanelPosition = LineInfoPanelPosition.BOTTOM
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            R.id.code_format -> editor.formatCodeAsync()
            R.id.switch_language -> chooseLanguage()
            R.id.search_panel_st -> toggleSearchPanel(item)

            R.id.search_am -> {
                binding.replaceEditor.setText("")
                binding.searchEditor.setText("")
                editor.searcher.stopSearch()
                editor.beginSearchMode()
            }

            R.id.switch_colors -> chooseTheme()

            R.id.text_wordwrap -> {
                item.isChecked = !item.isChecked
                editor.isWordwrap = item.isChecked
            }

            R.id.completionAnim -> {
                item.isChecked = !item.isChecked
                editor.getComponent<EditorAutoCompletion>()
                    .setEnabledAnimation(item.isChecked)
            }

            R.id.open_logs -> openLogs()
            R.id.clear_logs -> clearLogs()

            R.id.editor_line_number -> {
                editor.isLineNumberEnabled = !editor.isLineNumberEnabled
                item.isChecked = editor.isLineNumberEnabled
            }

            R.id.pin_line_number -> {
                editor.setPinLineNumber(!editor.isLineNumberPinned)
                item.isChecked = editor.isLineNumberPinned
            }

            R.id.load_test_file -> {
                openAssetsFile("samples/big_sample.txt")
            }

            R.id.softKbdEnabled -> {
                editor.isSoftKeyboardEnabled = !editor.isSoftKeyboardEnabled
                item.isChecked = editor.isSoftKeyboardEnabled
            }

            R.id.disableSoftKbdOnHardKbd -> {
                editor.isDisableSoftKbdIfHardKbdAvailable =
                    !editor.isDisableSoftKbdIfHardKbdAvailable
                item.isChecked = editor.isDisableSoftKbdIfHardKbdAvailable
            }

            R.id.switch_typeface -> chooseTypeface()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun chooseTypeface() {
        val fonts = arrayOf(
            "JetBrains Mono",
            "Ubuntu",
            "Roboto"
        )
        val assetsPaths = arrayOf(
            "JetBrainsMono-Regular.ttf",
            "Ubuntu-Regular.ttf",
            "Roboto-Regular.ttf"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(android.R.string.dialog_alert_title)
            .setSingleChoiceItems(fonts, -1) { dialog: DialogInterface, which: Int ->
                if (which in assetsPaths.indices) {
                    binding.editor.typefaceText =
                        Typeface.createFromAsset(assets, assetsPaths[which])
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun chooseLineNumberPanelPosition() {
        val editor = binding.editor
        val themes = arrayOf(
            getString(R.string.top),
            getString(R.string.bottom),
            getString(R.string.left),
            getString(R.string.right),
            getString(R.string.center),
            getString(R.string.top_left),
            getString(R.string.top_right),
            getString(R.string.bottom_left),
            getString(R.string.bottom_right)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fixed)
            .setSingleChoiceItems(themes, -1) { dialog: DialogInterface, which: Int ->
                editor.lnPanelPositionMode = LineInfoPanelPositionMode.FIXED
                when (which) {
                    0 -> editor.lnPanelPosition = LineInfoPanelPosition.TOP
                    1 -> editor.lnPanelPosition = LineInfoPanelPosition.BOTTOM
                    2 -> editor.lnPanelPosition = LineInfoPanelPosition.LEFT
                    3 -> editor.lnPanelPosition = LineInfoPanelPosition.RIGHT
                    4 -> editor.lnPanelPosition = LineInfoPanelPosition.CENTER
                    5 -> editor.lnPanelPosition =
                        LineInfoPanelPosition.TOP or LineInfoPanelPosition.LEFT

                    6 -> editor.lnPanelPosition =
                        LineInfoPanelPosition.TOP or LineInfoPanelPosition.RIGHT

                    7 -> editor.lnPanelPosition =
                        LineInfoPanelPosition.BOTTOM or LineInfoPanelPosition.LEFT

                    8 -> editor.lnPanelPosition =
                        LineInfoPanelPosition.BOTTOM or LineInfoPanelPosition.RIGHT
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun toggleSearchPanel(item: MenuItem) {
        if (binding.searchPanel.visibility == View.GONE) {
            binding.apply {
                replaceEditor.setText("")
                searchEditor.setText("")
                editor.searcher.stopSearch()
                searchPanel.visibility = View.VISIBLE
                item.isChecked = true
            }
        } else {
            binding.searchPanel.visibility = View.GONE
            binding.editor.searcher.stopSearch()
            item.isChecked = false
        }
    }

    private fun openLogs() {
        runCatching {
            openFileInput(LOG_FILE).reader().readText()
        }.onSuccess {
            binding.editor.setText(it)
        }.onFailure {
            toast(it.toString())
        }
    }

    private fun clearLogs() {
        runCatching {
            openFileOutput(LOG_FILE, MODE_PRIVATE)?.use {}
        }.onFailure {
            toast(it.toString())
        }.onSuccess {
            toast(R.string.deleting_log_success)
        }
    }

    private fun chooseLanguage() {
        val editor = binding.editor
        val languageOptions = arrayOf(
            "Java",
            "TextMate Java",
            "TextMate Kotlin",
            "TextMate Python",
            "TextMate Html",
            "TextMate JavaScript",
            "TextMate MarkDown",
            "TM Language from file",
            "Tree-sitter Java",
            "Monarch Java",
            "Monarch Kotlin",
            "Monarch Python",
            "Monarch TypeScript",
            "Text"
        )
        val tmLanguages = mapOf(
            "TextMate Java" to Pair("source.java", "source.java"),
            "TextMate Kotlin" to Pair("source.kotlin", "source.kotlin"),
            "TextMate Python" to Pair("source.java", "source.java"),
            "TextMate Html" to Pair("text.html.basic", "text.html.basic"),
            "TextMate JavaScript" to Pair("source.js", "source.js"),
            "TextMate MarkDown" to Pair("text.html.markdown", "text.html.markdown")
        )

        val monarchLanguages = mapOf(
            "Monarch Java" to "source.java",
            "Monarch Kotlin" to "source.kotlin",
            "Monarch Python" to "source.python",
            "Monarch TypeScript" to "source.typescript"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.switch_language)
            .setSingleChoiceItems(languageOptions, -1) { dialog: DialogInterface, which: Int ->
                when (val selected = languageOptions[which]) {
                    in tmLanguages -> {
                        val info = tmLanguages[selected]!!
                        try {
                            ensureTextmateTheme()
                            val editorLanguage = editor.editorLanguage
                            val language = if (editorLanguage is TextMateLanguage) {
                                editorLanguage.updateLanguage(info.first)
                                editorLanguage
                            } else {
                                TextMateLanguage.create(info.second, true)
                            }
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    in monarchLanguages -> {
                        val info = monarchLanguages[selected]!!

                        try {
                            ensureMonarchTheme()

                            val editorLanguage = editor.editorLanguage

                            val language = if (editorLanguage is MonarchLanguage) {
                                editorLanguage.updateLanguage(info)
                                editorLanguage
                            } else {
                                MonarchLanguage.create(info, true)
                            }
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    else -> {
                        when (selected) {
                            "Java" -> editor.setEditorLanguage(JavaLanguage())
                            "Text" -> editor.setEditorLanguage(EmptyLanguage())
                            "TM Language from file" -> loadTMLLauncher.launch("*/*")
                            "Tree-sitter Java" -> {
                                editor.setEditorLanguage(
                                    TsLanguageJava(
                                        JavaLanguageSpec(
                                            highlightScmSource = assets.open("tree-sitter-queries/java/highlights.scm")
                                                .reader().readText(),
                                            codeBlocksScmSource = assets.open("tree-sitter-queries/java/blocks.scm")
                                                .reader().readText(),
                                            bracketsScmSource = assets.open("tree-sitter-queries/java/brackets.scm")
                                                .reader().readText(),
                                            localsScmSource = assets.open("tree-sitter-queries/java/locals.scm")
                                                .reader().readText()
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun chooseTheme() {
        val editor = binding.editor
        val themes = arrayOf(
            "Default",
            "GitHub",
            "Eclipse",
            "Darcula",
            "VS2019",
            "NotepadXX",
            "QuietLight for TM(VSCode)",
            "Darcula for TM",
            "Ayu Dark for VSCode",
            "Solarized(Dark) for TM(VSCode)",
            "TM theme from file"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.color_scheme)
            .setSingleChoiceItems(themes, -1) { dialog: DialogInterface, which: Int ->
                when (which) {
                    0 -> editor.colorScheme = EditorColorScheme()
                    1 -> editor.colorScheme = SchemeGitHub()
                    2 -> editor.colorScheme = SchemeEclipse()
                    3 -> editor.colorScheme = SchemeDarcula()
                    4 -> editor.colorScheme = SchemeVS2019()
                    5 -> editor.colorScheme = SchemeNotepadXX()
                    6 -> try {
                        ensureTextmateTheme()
                        ThemeRegistry.getInstance().setTheme("quietlight")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    7 -> try {
                        ensureTextmateTheme()
                        ThemeRegistry.getInstance().setTheme("darcula")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    8 -> try {
                        ensureTextmateTheme()
                        ThemeRegistry.getInstance().setTheme("ayu-dark")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    9 -> try {
                        ensureTextmateTheme()
                        ThemeRegistry.getInstance().setTheme("solarized_dark")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    10 -> loadTMTLauncher.launch("*/*")
                }
                resetColorScheme()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun gotoNext(view: View) {
        try {
            binding.editor.searcher.gotoNext()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun gotoPrev(view: View) {
        try {
            binding.editor.searcher.gotoPrevious()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun replace(view: View) {
        try {
            binding.editor.searcher.replaceCurrentMatch(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun replaceAll(view: View) {
        try {
            binding.editor.searcher.replaceAll(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun showSearchOptions(view: View) {
        searchMenu.show()
    }

    private val loadTMLLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult


            val editorLanguage = binding.editor.editorLanguage

            val language = if (editorLanguage is TextMateLanguage) {
                editorLanguage.updateLanguage(
                    DefaultGrammarDefinition.withGrammarSource(
                        IGrammarSource.fromInputStream(
                            contentResolver.openInputStream(result),
                            result.path, null
                        ),
                    )
                )
                editorLanguage
            } else {
                TextMateLanguage.create(
                    DefaultGrammarDefinition.withGrammarSource(
                        IGrammarSource.fromInputStream(
                            contentResolver.openInputStream(result),
                            result.path, null
                        ),
                    ), true
                )
            }
            binding.editor.setEditorLanguage(language)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val loadTMTLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult

            ensureTextmateTheme()

            ThemeRegistry.getInstance().loadTheme(
                IThemeSource.fromInputStream(
                    contentResolver.openInputStream(result), result.path,
                    null
                )
            )

            resetColorScheme()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
    }

}

@Composable
private fun MainActivityComposeContent(
    editor: CodeEditor,
    onSearchChanged: (String) -> Unit,
    onReplaceChanged: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFormat: () -> Unit,
    onChooseLanguage: () -> Unit,
    onChooseTheme: () -> Unit,
    onOpenLogs: () -> Unit,
    onLoadLargeFile: () -> Unit,
    onMenuAction: (Int) -> Unit,
    groupRepository: SymbolGroupRepository,
    settingsRepository: SymbolDrawerSettingsRepository,
    onSymbolAction: (SymbolEntry) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var showManager by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // Activity Result ownership stays in the host app. The Compose library
    // receives only one-shot URI callbacks and does not depend on Activity APIs.
    var pendingOpenDocumentResult by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }
    var pendingCreateDocumentResult by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }
    val openJsonDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingOpenDocumentResult?.invoke(uri)
        pendingOpenDocumentResult = null
    }
    val createJsonDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        pendingCreateDocumentResult?.invoke(uri)
        pendingCreateDocumentResult = null
    }
    var showSearchPanel by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var groups by remember(groupRepository) {
        mutableStateOf(groupRepository.loadGroups().ifEmpty {
            SymbolDrawerDefaultGroups.createGroups().also(groupRepository::saveGroups)
        })
    }
    var drawerSettings by remember(settingsRepository) {
        mutableStateOf(settingsRepository.load().settings)
    }
    DisposableEffect(groupRepository) {
        val observer = (groupRepository as? com.zerostudio.idea.widget.editor.symbolinput.v2.data.ObservableSymbolGroupRepository)
            ?.observeGroups {
                groups = groupRepository.loadGroups()
            }
        onDispose { observer?.close() }
    }
    Column(Modifier.fillMaxSize()) {
        // Keep the editor's original compact action-bar structure: primary
        // actions remain in the toolbar, while secondary commands live in the
        // overflow menu instead of occupying permanent full-width rows.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Button(onClick = onUndo) { Text("撤销") }
            Button(onClick = onRedo) { Text("重做") }
            androidx.compose.material3.TextButton(onClick = { showSearchPanel = !showSearchPanel }) {
                Text(if (showSearchPanel) "关闭搜索" else "搜索")
            }
            androidx.compose.foundation.layout.Box {
                androidx.compose.material3.TextButton(onClick = { overflowMenuExpanded = true }) {
                    Text("更多 ⋮")
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = overflowMenuExpanded,
                    onDismissRequest = { overflowMenuExpanded = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("格式化") },
                        onClick = { overflowMenuExpanded = false; onFormat() }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("选择语言") },
                        onClick = { overflowMenuExpanded = false; onChooseLanguage() }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换主题") },
                        onClick = { overflowMenuExpanded = false; onChooseTheme() }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("符号设置") },
                        onClick = { overflowMenuExpanded = false; showSettings = true }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("打开调试文档") },
                        onClick = { overflowMenuExpanded = false; onLoadLargeFile() }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("查看日志") },
                        onClick = { overflowMenuExpanded = false; onOpenLogs() }
                    )
                    androidx.compose.material3.HorizontalDivider()
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("光标移至行尾") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.goto_end) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("光标上移") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.move_up) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("光标下移") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.move_down) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("光标左移") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.move_left) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("光标右移") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.move_right) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("光标移至行首") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.home) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("光标移至文档末尾") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.end) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换自动换行") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.text_wordwrap) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换行号显示") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.editor_line_number) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换软键盘") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.softKbdEnabled) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换硬键盘时隐藏软键盘") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.disableSoftKbdOnHardKbd) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("固定行号面板") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.ln_panel_fixed) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("行号面板跟随滚动") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.ln_panel_follow) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换行号固定显示") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.pin_line_number) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换放大镜") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.magnifier) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换 ICU") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.useIcu) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换补全动画") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.completionAnim) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("清理崩溃日志") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.clear_logs) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("启动搜索操作模式") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.search_am) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换语言") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.switch_language) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换配色方案") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.switch_colors) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("切换字体") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.switch_typeface) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("抽屉 API 测试") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.open_drawer_api_lab) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("打开测试 Activity") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.open_test_activity) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("分页编辑测试") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.open_paged_edit) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("LSP 测试") },
                        onClick = { overflowMenuExpanded = false; onMenuAction(R.id.open_lsp_activity) }
                    )
                }
            }
        }
        if (showSearchPanel) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it; onSearchChanged(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("搜索内容") }
                    )
                    OutlinedTextField(
                        value = replacement,
                        onValueChange = { replacement = it; onReplaceChanged(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("替换为") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.TextButton(onClick = onPrevious) { Text("上一个") }
                    androidx.compose.material3.TextButton(onClick = onNext) { Text("下一个") }
                    androidx.compose.material3.TextButton(onClick = onReplace) { Text("替换") }
                    androidx.compose.material3.TextButton(onClick = onReplaceAll) { Text("全部替换") }
                }
            }
        }
        AndroidView(
            factory = { editor },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        SymbolDrawer(
            groups = groups,
            modifier = Modifier.fillMaxWidth(),
            settings = drawerSettings,
            onSymbolClick = { symbol ->
                if (symbol.shortAction == SymbolAction.OpenManager) {
                    showManager = true
                } else {
                    onSymbolAction(symbol)
                }
            }
        )
        Spacer(Modifier.height(4.dp))
    }
    if (showManager) {
        Dialog(onDismissRequest = { showManager = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                shape = MaterialTheme.shapes.large
            ) {
                SymbolDrawerManagerScreen(
                    repository = groupRepository,
                    onClose = { showManager = false },
                    searchIncludesActionPayload = drawerSettings.searchIncludesActionPayload,
                    onOpenSettings = { showSettings = true },
                    requestOpenDocument = { onResult ->
                        pendingOpenDocumentResult = onResult
                        openJsonDocumentLauncher.launch(arrayOf("application/json"))
                    },
                    requestCreateDocument = { fileName, onResult ->
                        pendingCreateDocumentResult = onResult
                        createJsonDocumentLauncher.launch(fileName)
                    }
                )
            }
        }
    }
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                shape = MaterialTheme.shapes.large
            ) {
                SymbolDrawerSettingsScreen(
                    settings = drawerSettings,
                    groupRepository = groupRepository,
                    onSettingsChange = { updated ->
                        drawerSettings = updated
                        settingsRepository.save(updated)
                    },
                    onReset = {
                        settingsRepository.reset()
                        drawerSettings = settingsRepository.load().settings
                    },
                    requestOpenDocument = { onResult ->
                        pendingOpenDocumentResult = onResult
                        openJsonDocumentLauncher.launch(arrayOf("*/*"))
                    },
                    requestCreateDocument = { fileName, onResult ->
                        pendingCreateDocumentResult = onResult
                        createJsonDocumentLauncher.launch(fileName)
                    }
                )
            }
        }
    }
}
