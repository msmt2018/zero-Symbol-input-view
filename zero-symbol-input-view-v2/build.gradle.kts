/*
 * This file is part of ZeroStudio.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
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
                            name.set("GNU Affero General Public License v3.0 only")
                            url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                            distribution.set("repo")
                        }
                        license {
                            name.set("GNU General Public License v3.0 only")
                            url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                            distribution.set("repo")
                        }
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
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
