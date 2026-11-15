package com.zerostudio.idea.widget.drawer.action;

/** Compile-only proof that Java hosts can implement the document contract. */
final class JavaTextDocumentCompileFixture implements SymbolTextDocument {
    private String text = "";
    private int cursor;

    @Override public boolean isEditable() { return true; }
    @Override public int getTextLength() { return text.length(); }
    @Override public int getLineCount() { return 1; }
    @Override public int getCursorOffset() { return cursor; }
    @Override public int getSelectionStart() { return cursor; }
    @Override public int getSelectionEnd() { return cursor; }
    @Override public int lineForOffset(int offset) { return 0; }
    @Override public int lineStartOffset(int line) { return 0; }
    @Override public int lineEndOffset(int line) { return text.length(); }
    @Override public String lineText(int line) { return text; }
    @Override public String textBetween(int start, int end) { return text.substring(start, end); }
    @Override public char charAt(int offset) { return text.charAt(offset); }
    @Override public boolean replace(int start, int end, String replacement) {
        text = text.substring(0, start) + replacement + text.substring(end);
        cursor = start + replacement.length();
        return true;
    }
    @Override public void setSelection(int start, int end) { cursor = end; }

    SymbolActionHost createHost() {
        return new SymbolTextDocumentActionHost(
            this,
            (document, operation) -> false,
            () -> false,
            (extensionId, payload) -> false
        );
    }
}
