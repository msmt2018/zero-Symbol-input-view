plugins {
    id("com.android.library")
}

android {
    namespace = "com.zerostudio.idea.widget.editor.symbolinput.v2"
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
}
