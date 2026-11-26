/*
 * This file is part of Zero Symbol Input View.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only
 * License text: https://www.gnu.org/licenses/agpl-3.0.html
 * Project additional terms: ADDITIONAL-TERMS.md
 */
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.zerostudio.idea.widget.editor.symbolinput.v2"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation(libs.kotlin.stdlib)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation(libs.material)
    implementation(libs.gson)
    api(libs.androidx.annotation)
    testImplementation(libs.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
}

android {
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.msmt2018.zero-Symbol-input-view"
                artifactId = "zero-symbol-input-view-v2"
                version = providers.environmentVariable("VERSION")
                    .orElse(project.version.toString())
                    .get()

                pom {
                    name.set("Zero Symbol Input View V2")
                    description.set(
                        "An independently packaged Android symbol drawer and input toolbar library."
                    )
                    url.set("https://github.com/msmt2018/zero-Symbol-input-view")

                    licenses {
                        license {
                            name.set("GNU Affero General Public License v3.0 only (AGPL-3.0-only)")
                            url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("msmt2018")
                            name.set("android_zero")
                            url.set("https://github.com/msmt2018")
                        }
                    }

                    scm {
                        url.set("https://github.com/msmt2018/zero-Symbol-input-view")
                        connection.set(
                            "scm:git:git://github.com/msmt2018/zero-Symbol-input-view.git"
                        )
                        developerConnection.set(
                            "scm:git:ssh://git@github.com/msmt2018/zero-Symbol-input-view.git"
                        )
                    }
                }
            }
        }
    }
}
