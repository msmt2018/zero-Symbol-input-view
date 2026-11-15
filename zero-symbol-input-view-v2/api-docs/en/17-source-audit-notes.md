# 17 — Source Audit Notes and Known API Boundaries

This note records findings from the repository tree inventory and targeted review of the V2 module's public API facade, action runtime, extension registry, widget entry point, Maven publication configuration, documentation index and existing tests. It is not a claim that every Android runtime path has been executed on a device.

## Scope inspected

The V2 module tree contains 164 tracked files in the inspected main-branch tree, including 141 Kotlin/Java/Gradle/XML source and test/resource files and 17 Markdown API documentation files. Production API files reviewed in detail include:
- `build.gradle.kts`;
- public Drawer facade files under `com.zerostudio.idea.widget.drawer.*`;
- `SymbolActionExecutionApi.kt`;
- `SymbolDrawerView.kt`;
- `SymbolDrawerExtensionRegistry.kt`;
- the editor-neutral action/document API and relevant API documentation.

The repository also contains JVM tests, Java compile fixtures and Android instrumentation tests. Static repository inspection cannot replace running the Gradle build and device tests.

## Confirmed API boundary: Kotlin typealias vs Java ABI

Most generic Drawer facade entries are Kotlin `typealias` declarations pointing to implementation classes in the historical `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.*` packages. This is valid for Kotlin source imports but does not emit new JVM classes under the facade package. Java consumers must use the actual JVM class package unless the API is physically migrated or wrapped in real public JVM declarations.

The extension facade contains real JVM declarations for selected entry points such as `SymbolDrawerBuiltInFeatures`; theme extension types also have Java-facing declarations and compile fixtures. Do not generalize those exceptions to every typealias.

## Confirmed design constraints

- The V2 Maven publication is declared by the V2 module's own `maven-publish` configuration with artifact ID `zero-symbol-input-view-v2`.
- The action host is synchronous and does not switch threads. Host implementations must obey the editor's threading contract.
- Composite actions are sequential and may partially complete; they are not automatically transactional.
- The generic document API uses UTF-16 offsets and end-exclusive ranges. Default transaction hooks do not provide atomic rollback unless the host overrides them.
- The extension registry stores runtime providers/handlers. Registrations and observers require explicit lifecycle cleanup.
- The custom theme provider currently has a limited consumer scope; registration alone does not imply that all manager/settings/cell surfaces are recolored.
- The generic Drawer facade is transitional; its Kotlin aliases are not a physical package relocation.

## Documentation synchronization actions in this branch

- Added an English companion API documentation set under `api-docs/en/` (README plus pages 01–17).
- Added bilingual API documentation links to the V2 module README and API documentation index.
- Kept the Chinese documentation as the detailed source reference; no runtime source code was modified in this documentation branch.

## Remaining validation required before release

Run the repository's CI and, where available:
1. `./gradlew :zero-symbol-input-view-v2:compileDebugKotlin`
2. `./gradlew :zero-symbol-input-view-v2:test`
3. `./gradlew :zero-symbol-input-view-v2:assembleRelease`
4. `./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest` on an emulator/device
5. Java compile fixtures for the intended public Java surface
6. Maven publication verification of the V2 POM, AAR and sources JAR
7. Host integration tests for editor thread affinity, undo/transaction behavior, clipboard and Fragment recreation

A green JVM test suite alone does not establish correct gesture arbitration, Insets, nested scrolling, accessibility or editor integration on real Android devices.
