/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

import org.gradle.api.publish.maven.MavenPublication

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
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation(libs.material)
    implementation(libs.gson)
    api(libs.androidx.annotation)

    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    api("androidx.compose.runtime:runtime")
    api("androidx.compose.ui:ui")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
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
 * Keep the migrated UI packages strictly Compose-native while legacy View
 * implementation files are still being extracted from this migration baseline.
 *
 * This is intentionally a package-scoped guard: it does not pretend that the
 * entire artifact is already free of transitional View sources.
 */
val verifyComposeUiBoundary by tasks.registering {
    group = "verification"
    description = "Rejects Android View and View-based Material UI APIs in Compose UI packages."

    val composeUiRoots = listOf(
        layout.projectDirectory.dir(
            "src/main/kotlin/com/zerostudio/idea/widget/editor/symbolinput/v2/compose"
        ).asFile,
        layout.projectDirectory.dir(
            "src/main/kotlin/com/zerostudio/idea/widget/drawer/compose"
        ).asFile
    )

    doLast {
        val forbiddenImports = listOf(
            Regex("""^\s*import\s+android\.view\."""),
            Regex("""^\s*import\s+android\.widget\."""),
            Regex("""^\s*import\s+android\.graphics\."""),
            Regex("""^\s*import\s+androidx\.recyclerview\."""),
            Regex("""^\s*import\s+androidx\.viewpager2\."""),
            Regex("""^\s*import\s+com\.google\.android\.material\.""")
        )
        val forbiddenCalls = listOf(
            Regex("""\bAndroidView\s*\("""),
            Regex("""\bAndroidViewBinding\s*\(""")
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

tasks.named("check") {
    dependsOn(verifyComposeUiBoundary)
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
