plugins {
    id("com.android.library")
}

android {
    namespace = "android.zero.studio.widget.editor.symbolinput.v2"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation(libs.gson)
    api(libs.androidx.annotation)
    testImplementation(libs.junit)
}
