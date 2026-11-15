package com.zerostudio.idea.widget.drawer.action;

/**
 * Compile-only fixture proving that Java consumers can use the public action
 * identity, handler and registry as real JVM declarations (not Kotlin aliases).
 */
final class JavaActionRegistryCompileFixture {
    static AutoCloseable registerJavaHandler() {
        SymbolActionRegistry<String> registry = new SymbolActionRegistry<>();
        SymbolActionId id = SymbolActionId.extension("com.example/editor:paste");
        AutoCloseable extension = registry.register(id, (context, payload) ->
                "editor".equals(context) && "clipboard".equals(payload));
        AutoCloseable legacy = registry.register(54, (context, payload) ->
                "editor".equals(context) && payload == null);
        return () -> {
            extension.close();
            legacy.close();
            registry.clear();
        };
    }
}
