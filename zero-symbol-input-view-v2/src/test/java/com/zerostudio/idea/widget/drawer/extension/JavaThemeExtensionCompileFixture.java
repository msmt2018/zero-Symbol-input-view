package com.zerostudio.idea.widget.drawer.extension;

import android.content.Context;
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings;

/**
 * Compile-time fixture proving that the public theme extension API exposes
 * real JVM types consumable from Java (not Kotlin typealias declarations).
 */
final class JavaThemeExtensionCompileFixture implements SymbolDrawerThemeExtension {
    @Override
    public String getId() {
        return "sample.java.theme";
    }

    @Override
    public String getTitle() {
        return "Java theme";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public SymbolDrawerTheme resolve(Context context, SymbolDrawerSettings settings) {
        return new SymbolDrawerTheme(
            0xFF6750A4,
            0xFFFFFFFF,
            0xFFFFFBFE,
            0xFF1C1B1F,
            0xFF49454F,
            0xFF79747E,
            0xFFFFFBFE,
            0xFFEADDFF
        );
    }
}
