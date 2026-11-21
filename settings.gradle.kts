@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    // Resolve the Compose compiler plugin as a regular Kotlin Gradle artifact.
    // The Plugin Portal has intermittently served its marker/plugin POM while
    // failing to serve the imported kotlin-gradle-plugins-bom POM from the same
    // repository. Direct module resolution lets Maven Central provide the
    // plugin and its complete Kotlin BOM dependency graph consistently.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.plugin.compose") {
                useModule(
                    "org.jetbrains.kotlin:compose-compiler-gradle-plugin:${requested.version}"
                )
            }
        }
    }

    repositories {
        // Resolve Kotlin Gradle plugin transitive BOMs from canonical Maven
        // repositories first. Plugin Portal can serve the plugin POM while a
        // transitive Kotlin BOM is temporarily absent from its mirror.
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven.aliyun.com/nexus/content/groups/public/")
        mavenCentral()
        maven("https://repo.eclipse.org/content/groups/releases/") {
            content { includeGroupByRegex("org\\.eclipse(\\..*)?") }
        }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven("https://repo.itextsupport.com/android")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide/")
        maven("https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-plugin/")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies/")
        maven(url = "https://www.jetbrains.com/intellij-repository/releases/")
        maven(url = "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies/")
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

rootProject.name = "sora-editor"
include(
    ":editor-bom",
    ":editor",
    ":app",
    ":language-monarch",
    ":language-java",
    ":language-textmate",
    ":editor-lsp",
    ":language-treesitter",
    ":zero-Symbol-input-view",
    ":zero-symbol-input-view-v2",
    ":zero-symbol-input-view-v2-compose",
    ":oniguruma-native"
)
