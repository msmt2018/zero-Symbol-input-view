/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

import org.gradle.api.publish.maven.MavenPublication
import java.util.ArrayDeque
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

android {
    namespace = "com.zerostudio.idea.widget.editor.symbolinput.v2"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures { compose = true }
    publishing {
        singleVariant("release") { withSourcesJar() }
    }
}

dependencies {
    // Existing V2 domain, persistence, JSON, action and extension contracts are
    // retained as the migration baseline while their UI adapters are replaced.
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.gson)
    api(libs.androidx.annotation)

    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    api("androidx.compose.runtime:runtime")
    api("androidx.compose.ui:ui")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation(libs.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

/**
 * Enforce a Compose-only UI source boundary across the complete library module.
 *
 * This is intentionally a source-root-scoped guard: it does not pretend that the
 * entire artifact is already free of transitional View sources.
 */
val verifyComposeUiBoundary by tasks.registering {
    group = "verification"
    description = "Rejects Android View and View-based Material UI APIs in Compose UI packages."

    val composeUiRoots = listOf(
        layout.projectDirectory.dir("src/main/kotlin").asFile,
        layout.projectDirectory.dir("src/main/java").asFile
    )

    doLast {
        val forbiddenImports = listOf(
            Regex("""^\s*import\s+android\.view\."""),
            Regex("""^\s*import\s+android\.widget\."""),
            Regex("""^\s*import\s+android\.graphics\."""),
            Regex("""^\s*import\s+android\.os\.(?:Handler|HandlerThread|Looper|Message)\b"""),
            Regex("""^\s*import\s+androidx\.compose\.ui\.viewinterop\."""),
            Regex("""^\s*import\s+androidx\.appcompat\."""),
            Regex("""^\s*import\s+androidx\.viewbinding\."""),
            Regex("""^\s*import\s+androidx\.customview\."""),
            Regex("""^\s*import\s+androidx\.recyclerview\."""),
            Regex("""^\s*import\s+androidx\.viewpager2\."""),
            Regex("""^\s*import\s+com\.google\.android\.material\."""),
            Regex("""^\s*import\s+(?:android\.app\.(?:Activity|Fragment)|androidx\.activity\.|androidx\.fragment\.|androidx\.lifecycle\.|androidx\.savedstate\.)""")
        )
        val forbiddenCalls = listOf(
            Regex("""\bAndroidView\s*\("""),
            Regex("""\bAndroidViewBinding\s*\("""),
            Regex("""\b(?:Handler|HandlerThread)\s*\("""),
            Regex("""\bLooper\.getMainLooper\s*\("""),
            Regex("""\b(?:runOnUiThread|postDelayed)\s*\("""),
            Regex("""\b(ComposeView|LayoutInflater|ViewBinding|ViewStub|ViewTreeLifecycleOwner|ViewTreeViewModelStoreOwner|ViewTreeSavedStateRegistryOwner|findViewById|setContentView)\b""")
        )

        val violations = composeUiRoots
            .filter { it.exists() }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .flatMap { source ->
                        source.readLines().mapIndexedNotNull { index, line ->
                            val trimmed = line.trimStart()
                            val isComment = trimmed.startsWith("//") ||
                                trimmed.startsWith("*") ||
                                trimmed.startsWith("/*")
                            val forbidden = !isComment && (
                                forbiddenImports.any { it.containsMatchIn(line) } ||
                                    forbiddenCalls.any { it.containsMatchIn(line) }
                                )
                            if (forbidden) {
                                "${source.relativeTo(projectDir)}:${index + 1}: $line"
                            } else null
                        }
                    }
                    .toList()
            }

        check(violations.isEmpty()) {
            buildString {
                appendLine("Compose UI boundary violation(s) detected:")
                violations.forEach { appendLine(" - $it") }
                appendLine("Use Jetpack Compose UI/Foundation/Material3 APIs instead of Android View widgets.")
            }
        }
    }
}

/**
 * Keep the drawer state and adaptive resolution engines below the Compose adapter layer.
 *
 * DrawerStateHolder, adaptive snapshot publishers and immutable anchor/geometry policies must remain
 * plain Kotlin so they can be exercised without Composition or Android UI.
 */
val verifyCoreStateEngineBoundary by tasks.registering {
    group = "verification"
    description = "Rejects Android/Compose dependencies in platform-neutral action, data, settings, navigation, and drawer state engines."

    val coreFiles = listOf(
        // Editor-neutral action execution contracts and adapters.
        "src/main/kotlin/com/zerostudio/idea/widget/drawer/action/SymbolActionExecutionApi.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/drawer/action/core/ActionExecutionEngine.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/drawer/action/SymbolActionRegistry.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/drawer/action/SymbolTextDocument.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/drawer/action/SymbolTextDocumentActionHost.kt",
        // Platform-neutral domain and repository/settings contracts.
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/model/SymbolModels.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/SymbolGroupRepository.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/settings/SymbolDrawerSettings.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/settings/SymbolDrawerSettingsRepository.kt",
        // Drawer geometry, state and navigation engines.
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/core/SynchronousSnapshotPublisher.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/navigation/DrawerPageDescriptor.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/navigation/DrawerNavigationStateEngine.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/core/DrawerStateHolder.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/core/DrawerAnchor.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/core/DrawerAnchorResolver.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/core/DrawerAnchorStrategy.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/core/DrawerNestedScrollPolicy.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/state/DrawerGeometry.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/AdaptiveWindowController.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/ConcurrentSnapshotPublisher.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/WindowAdaptiveInfo.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/AdaptiveLayoutResolver.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/AdaptiveGridResolver.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/SymbolDrawerDockingPolicy.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/SymbolDrawerDockingLayoutResolver.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/SymbolDrawerDockingCoordinator.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/adaptive/AdaptiveDimensions.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/core/DrawerState.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/core/DrawerInteractionController.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/action/BuiltinSymbolActionCatalog.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/action/SymbolActionJsonCodec.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/action/SymbolActionExecutor.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/LegacySymbolActionAdapter.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/LegacySymbolActionCatalog.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/LegacySymbolActionDefinitions.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/LegacySymbolDataMigration.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/SymbolDrawerDefaultGroups.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/SymbolDrawerDefaults.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/manager/LegacyActionPickerResolver.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/LegacySymbolDataCodec.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/SymbolGroupBundleCodec.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/SymbolGroupStorageCodec.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/SymbolGroupManager.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/data/SymbolDrawerDefaultGroups.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/settings/JsonDocumentIO.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/navigation/DrawerNavigationController.kt",
        "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/drawer/navigation/DrawerNavigationConfig.kt"
    ).map { layout.projectDirectory.file(it).asFile }

    doLast {
        val forbiddenImports = Regex("""^\s*import\s+(?:android\.|androidx\.)""")
        val forbiddenQualifiedReferences = Regex("""\b(?:android|androidx)\.[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*""")
        val violations = coreFiles.flatMap { source ->
            check(source.isFile) { "Required core state source is missing: $source" }
            source.readLines().mapIndexedNotNull { index, line ->
                val trimmed = line.trimStart()
                val isComment = trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
                if (!isComment && (
                    forbiddenImports.containsMatchIn(line) ||
                        forbiddenQualifiedReferences.containsMatchIn(line)
                )) {
                    "${source.relativeTo(projectDir)}:${index + 1}: $line"
                } else null
            }
        }
        check(violations.isEmpty()) {
            "Platform-neutral core source references Android/Compose APIs:\n" +
                violations.joinToString("\n")
        }
        logger.lifecycle("Platform-neutral core boundary clean: ${coreFiles.size} files inspected.")
    }
}

tasks.named("check") {
    dependsOn(
        verifyComposeUiBoundary,
        verifyCoreStateEngineBoundary,
        "verifyComposeArtifactBoundary",
        "verifyComposeResolvedDependencyBoundary"
    )
}

/**
 * Inspect the resolved production runtime graph, not only dependency strings
 * declared in this module. This catches View/lifecycle artifacts introduced
 * transitively by another library.
 *
 * Compose runtime itself remains the UI runtime. This module intentionally
 * does not use Android Lifecycle, Activity, Fragment, SavedState, RecyclerView,
 * ViewPager2, or View-based Material integration APIs.
 */
val verifyComposeResolvedDependencyBoundary by tasks.registering {
    group = "verification"
    description = "Rejects transitive legacy View widget and layout runtime dependencies."

    doLast {
        val runtimeConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
            .map { name ->
                configurations.findByName(name)
                    ?: error("Required runtime configuration '$name' was not created")
            }

        // Compose's Android backend legitimately brings lifecycle/runtime,
        // activity and saved-state artifacts transitively (for example through
        // ui-android and material3-android). Their presence is not evidence that
        // this library owns an Activity/Fragment/View lifecycle. Source-level
        // guards above reject those APIs; this resolved-graph guard rejects the
        // legacy View widget/toolkit stacks that Compose does not require.
        val forbiddenGroups = setOf(
            "androidx.fragment",
            "androidx.recyclerview",
            "androidx.viewpager2",
            "com.google.android.material",
            "androidx.appcompat",
            "androidx.constraintlayout",
            "androidx.coordinatorlayout",
            "androidx.drawerlayout"
        )

        val violations = runtimeConfigurations.flatMap { configuration ->
            val resolution = configuration.incoming.resolutionResult
            val components = resolution.allComponents
            val paths = linkedMapOf<String, List<String>>()
            val root = resolution.rootComponent.get()
            val queue = ArrayDeque<Pair<ResolvedComponentResult, List<String>>>()
            queue.add(Pair(root, listOf(configuration.name, root.id.displayName)))
            val visited = mutableSetOf<String>()
            while (queue.isNotEmpty()) {
                val entry = queue.removeFirst()
                val component = entry.first
                val path = entry.second
                val componentKey = component.id.displayName
                if (!visited.add(componentKey)) continue
                val module = component.id as? ModuleComponentIdentifier
                if (module != null && module.group in forbiddenGroups) {
                    paths.putIfAbsent(
                        "${configuration.name}: ${module.group}:${module.module}:${module.version}",
                        path
                    )
                }
                component.dependencies.forEach { dependency ->
                    if (dependency is ResolvedDependencyResult) {
                        val selected: ResolvedComponentResult = dependency.selected
                        queue.add(Pair(selected, path + selected.id.displayName))
                    }
                }
            }
            paths.map { (artifact, path) ->
                "$artifact\\n      via " + path.joinToString(" -> ")
            }
        }.distinct().sorted()

        check(violations.isEmpty()) {
            buildString {
                appendLine("Compose runtime dependency boundary violation(s):")
                violations.forEach { appendLine(" - $it") }
                appendLine(
                    "Remove the transitive legacy View toolkit dependency or replace the integration " +
                        "with a Compose-native API."
                )
            }
        }
        logger.lifecycle(
            "Compose runtime dependency boundary clean: " +
                "${runtimeConfigurations.size} production runtime configurations inspected."
        )
    }
}

/**
 * Whole-artifact migration inventory. This is intentionally informational while
 * legacy View sources are being replaced. The strict artifact boundary task
 * is already attached to check below.
 */
val auditComposeMigration by tasks.registering {
    group = "verification"
    description = "Reports remaining Android View UI source references and View-only dependencies."

    val sourceRoots = listOf(
        "src/main/kotlin", "src/main/java",
        "src/test/kotlin", "src/test/java",
        "src/androidTest/kotlin", "src/androidTest/java"
    ).map { layout.projectDirectory.dir(it).asFile }
    val forbiddenImports = listOf(
        Regex("""^\s*import\s+android\.view\."""),
        Regex("""^\s*import\s+android\.widget\."""),
        Regex("""^\s*import\s+android\.graphics\."""),
        Regex("""^\s*import\s+android\.os\.(?:Handler|HandlerThread|Looper|Message)\b"""),
        Regex("""^\s*import\s+androidx\.recyclerview\."""),
        Regex("""^\s*import\s+androidx\.viewpager2\."""),
        Regex("""^\s*import\s+com\.google\.android\.material\."""),
            Regex("""^\s*import\s+(?:android\.app\.(?:Activity|Fragment)|androidx\.activity\.|androidx\.fragment\.|androidx\.lifecycle\.|androidx\.savedstate\.)""")
    )
    val forbiddenQualifiedReferences = listOf(
        Regex("""\b(?:android\.(?:view|widget|graphics|os)|androidx\.(?:recyclerview|viewpager2|activity|fragment|lifecycle|savedstate|viewbinding|appcompat|customview|compose\.ui\.viewinterop)|com\.google\.android\.material)\.[A-Z][A-Za-z0-9_]*(?:\.[A-Z][A-Za-z0-9_]*)*\b""")
    )
    val forbiddenReferences = listOf(
        Regex("""\bAndroidView(?:Binding)?\s*\("""),
        Regex("""\b(ComposeView|LayoutInflater|ViewBinding|ViewStub|ViewTreeLifecycleOwner|ViewTreeViewModelStoreOwner|ViewTreeSavedStateRegistryOwner|findViewById|setContentView)\b"""),
        Regex("""\b(ViewGroup|MotionEvent|RecyclerView|ViewPager2|Activity|Fragment|Lifecycle|LifecycleOwner|LifecycleRegistry|LocalLifecycleOwner|ViewModel|ViewModelStoreOwner|ViewModelProvider|SavedStateHandle|SavedStateRegistryOwner|LifecycleEventObserver|rememberLauncherForActivityResult|Handler|HandlerThread|Looper|runOnUiThread)\b""")
    )
    val viewOnlyDependencies = listOf(
        "androidx.recyclerview:recyclerview",
        "androidx.viewpager2:viewpager2",
        "com.google.android.material:material",
        "androidx.lifecycle:lifecycle-runtime-compose",
        "androidx.activity:activity-compose",
        "androidx.activity:activity",
        "androidx.fragment:fragment",
        "androidx.lifecycle:lifecycle-runtime",
        "androidx.lifecycle:lifecycle-viewmodel",
        "androidx.lifecycle:lifecycle-viewmodel-compose",
        "androidx.lifecycle:lifecycle-viewmodel-savedstate",
        "androidx.lifecycle:lifecycle-common",
        "androidx.savedstate:savedstate"
    )

    doLast {
        val sourceFiles = sourceRoots.filter { it.exists() }
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "java") }.toList() }
        val legacyLayoutFiles = layout.projectDirectory.dir("src/main/res").asFile
            .takeIf { it.exists() }
            ?.walkTopDown()
            ?.filter { it.isFile && it.extension == "xml" && it.parentFile.name.startsWith("layout") }
            ?.toList().orEmpty()
        val manifestFile = layout.projectDirectory.file("src/main/AndroidManifest.xml").asFile
        val manifestDeclaresActivity = manifestFile.takeIf { it.isFile }?.readText()
            ?.let { Regex("""<\s*(?:activity|activity-alias)\b""").containsMatchIn(it) } ?: false
        val violations = sourceFiles.flatMap { source ->
            source.readLines().mapIndexedNotNull { index, line ->
                val trimmed = line.trimStart()
                val comment = trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
                if (!comment && (forbiddenImports.any { it.containsMatchIn(line) } ||
                        forbiddenReferences.any { it.containsMatchIn(line) } ||
                        forbiddenQualifiedReferences.any { it.containsMatchIn(line) })) {
                    "${source.relativeTo(projectDir)}:${index + 1}: ${line.trim()}"
                } else null
            }
        }
        // Match only actual Gradle dependency declarations. Searching the whole
        // script also matches the audit's own forbidden-dependency inventory and
        // makes this report permanently noisy.
        val dependencyDeclarations = file("build.gradle.kts").readLines()
        val dependencyViolations = dependencyDeclarations.mapNotNull { line ->
            val declaration = Regex(
                """^\s*(?:api|implementation|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|debugImplementation|releaseImplementation)\s*\(\s*["']([^"']+)["']"""
            ).find(line)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
            viewOnlyDependencies.firstOrNull { dependency ->
                declaration == dependency || declaration.startsWith("$dependency:")
            }?.let { dependency -> "$dependency <- ${line.trim()}" }
        }
        logger.lifecycle("Compose migration audit: ${sourceFiles.size} source files scanned")
        logger.lifecycle("Remaining legacy XML layout files: ${legacyLayoutFiles.size}")
        legacyLayoutFiles.forEach { logger.lifecycle("  - ${it.relativeTo(projectDir)}") }
        logger.lifecycle("Manifest declares Activity: $manifestDeclaresActivity")
        logger.lifecycle("Remaining View UI source findings: ${violations.size}")
        violations.forEach { logger.lifecycle("  - $it") }
        logger.lifecycle("Remaining View-only dependency declarations: ${dependencyViolations.size}")
        dependencyViolations.forEach { logger.lifecycle("  - $it") }
        if (violations.isEmpty() && dependencyViolations.isEmpty() && legacyLayoutFiles.isEmpty() && !manifestDeclaresActivity) {
            logger.lifecycle("Compose source/dependency boundary is clean.")
        }
    }
}

/** Strict artifact-wide gate, attached to check after the migration inventory task. */
val verifyComposeArtifactBoundary by tasks.registering {
    group = "verification"
    description = "Fails while any View UI source or View-only dependency remains."
    dependsOn(auditComposeMigration)
    doLast {
        val sourceRoots = listOf(
            "src/main/kotlin", "src/main/java",
        "src/test/kotlin", "src/test/java",
        "src/androidTest/kotlin", "src/androidTest/java"
        ).map { layout.projectDirectory.dir(it).asFile }
        val files = sourceRoots.filter { it.exists() }
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "java") }.toList() }
        val legacyLayoutFiles = layout.projectDirectory.dir("src/main/res").asFile
            .takeIf { it.exists() }
            ?.walkTopDown()
            ?.filter { it.isFile && it.extension == "xml" && it.parentFile.name.startsWith("layout") }
            ?.toList().orEmpty()
        val manifestFile = layout.projectDirectory.file("src/main/AndroidManifest.xml").asFile
        val manifestDeclaresActivity = manifestFile.takeIf { it.isFile }?.readText()
            ?.let { Regex("""<\s*(?:activity|activity-alias)\b""").containsMatchIn(it) } ?: false
        val forbiddenImports = Regex("""^\s*import\s+(android\.view\.|android\.widget\.|android\.graphics\.|android\.os\.(Handler|HandlerThread|Looper|Message)|androidx\.appcompat\.|androidx\.viewbinding\.|androidx\.compose\.ui\.viewinterop\.|androidx\.customview\.|androidx\.recyclerview\.|androidx\.viewpager2\.|com\.google\.android\.material\.|android\.app\.(Activity|Fragment)|androidx\.activity\.|androidx\.fragment\.|androidx\.lifecycle\.|androidx\.savedstate\.)""")
        val forbiddenReferences = Regex("""\b(AndroidView(?:Binding)?|Handler|HandlerThread|Looper|runOnUiThread|ComposeView|LayoutInflater|ViewBinding|ViewStub|ViewTreeLifecycleOwner|ViewTreeViewModelStoreOwner|ViewTreeSavedStateRegistryOwner|findViewById|setContentView|SymbolDrawerView|ViewGroup|MotionEvent|RecyclerView|ViewPager2|Activity|Fragment|Lifecycle|LifecycleOwner|LifecycleRegistry|LocalLifecycleOwner|ViewModel|ViewModelStoreOwner|ViewModelProvider|SavedStateHandle|SavedStateRegistryOwner|LifecycleEventObserver|rememberLauncherForActivityResult)\b""")
        val forbiddenQualifiedReferences = Regex("""\b(?:android\.(?:view|widget|graphics|os)|androidx\.(?:recyclerview|viewpager2|activity|fragment|lifecycle|savedstate|viewbinding|appcompat|customview|compose\.ui\.viewinterop)|com\.google\.android\.material)\.[A-Z][A-Za-z0-9_]*(?:\.[A-Z][A-Za-z0-9_]*)*\b""")
        val violations = files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val trimmed = line.trimStart()
                val comment = trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
                if (!comment && (forbiddenImports.containsMatchIn(line) || forbiddenReferences.containsMatchIn(line) ||
                        forbiddenQualifiedReferences.containsMatchIn(line))) {
                    "${file.relativeTo(projectDir)}:${index + 1}: $line"
                } else null
            }
        }
        val dependencyDeclarations = file("build.gradle.kts").readLines()
        val dependencies = dependencyDeclarations.mapNotNull { line ->
            val declaration = Regex(
                """^\s*(?:api|implementation|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|debugImplementation|releaseImplementation)\s*\(\s*["']([^"']+)["']"""
            ).find(line)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
            listOf(
                "androidx.recyclerview:recyclerview",
                "androidx.viewpager2:viewpager2",
                "com.google.android.material:material",
                "androidx.lifecycle:lifecycle-runtime-compose",
                "androidx.activity:activity-compose",
                "androidx.activity:activity",
                "androidx.fragment:fragment",
                "androidx.lifecycle:lifecycle-runtime",
                "androidx.lifecycle:lifecycle-viewmodel",
                "androidx.lifecycle:lifecycle-viewmodel-compose",
                "androidx.lifecycle:lifecycle-viewmodel-savedstate",
                "androidx.lifecycle:lifecycle-common",
                "androidx.savedstate:savedstate"
            ).firstOrNull { dependency ->
                declaration == dependency || declaration.startsWith("$dependency:")
            }?.let { dependency -> "$dependency <- ${line.trim()}" }
        }
        check(violations.isEmpty() && dependencies.isEmpty() && legacyLayoutFiles.isEmpty() && !manifestDeclaresActivity) {
            "Compose artifact boundary is not clean. Run auditComposeMigration for the complete inventory."
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.msmt2018.zero-Symbol-input-view"
                artifactId = "zero-symbol-input-view-v2-compose"
                version = providers.environmentVariable("VERSION")
                    .orElse(project.version.toString()).get()
                pom {
                    name.set("Zero Symbol Input View V2 Compose")
                    description.set("Jetpack Compose and Material 3 symbol drawer and input toolbar library.")
                    url.set("https://github.com/msmt2018/zero-Symbol-input-view")
                    licenses {
                        license {
                            name.set("AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0")
                            url.set("https://github.com/msmt2018/zero-Symbol-input-view")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set("msmt2017")
                            name.set("android_zero")
                            url.set("https://github.com/msmt2018")
                        }
                    }
                    scm {
                        url.set("https://github.com/msmt2018/zero-Symbol-input-view")
                        connection.set("scm:git:git://github.com/msmt2018/zero-Symbol-input-view.git")
                        developerConnection.set("scm:git:ssh://git@github.com/msmt2018/zero-Symbol-input-view.git")
                    }
                }
            }
        }
    }
}
