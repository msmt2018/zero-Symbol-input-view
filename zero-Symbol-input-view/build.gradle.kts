/*
 * This file is part of Zero Symbol Input View.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only
 * License text: https://www.gnu.org/licenses/agpl-3.0.html
 * Project additional terms: ADDITIONAL-TERMS.md
 */
plugins {
    id("com.android.library")
}

android {
    namespace = "com.zerostudio.idea.widget.editor.symbolinput"
}

dependencies {
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    implementation(projects.editor)
    api(libs.androidx.annotation)
    implementation(projects.languageJava)
    implementation(projects.languageTextmate)

}
