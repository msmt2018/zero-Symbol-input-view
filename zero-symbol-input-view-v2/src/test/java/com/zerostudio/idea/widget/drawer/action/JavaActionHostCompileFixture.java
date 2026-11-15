package com.zerostudio.idea.widget.drawer.action;

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation;
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget;
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind;
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform;
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction;

/** Compile-only fixture for the Java-facing, editor-neutral action API. */
final class JavaActionHostCompileFixture {
    static SymbolActionResult execute(SymbolAction action) {
        SymbolActionHost host = new SymbolActionHost() {
            @Override public boolean insertText(String template) { return true; }
            @Override public boolean replaceSelection(String text) { return true; }
            @Override public boolean surroundSelection(String prefix, String suffix) { return true; }
            @Override public boolean moveCursor(CursorTarget target) { return true; }
            @Override public boolean delete(DeleteKind kind) { return true; }
            @Override public boolean transformSelection(SelectionTransform transform) { return true; }
            @Override public boolean executeEditorCommand(String commandId, String payload) { return false; }
            @Override public boolean performClipboard(ClipboardOperation operation) { return true; }
            @Override public boolean openManager() { return true; }
            @Override public boolean executeExtension(String extensionId, String payload) { return false; }
        };
        SymbolActionExecutor executor = new SymbolActionExecutor();
        return executor.execute(action, host, SymbolActionCancellation.NEVER);
    }
}
