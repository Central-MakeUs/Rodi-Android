plugins {
    id("dororong.rodi.android.library.compose")
}

android {
    namespace = "com.dororong.rodi.core.ui"
}

dependencies {
    implementation(project(":core:common"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.bundles.compose)
    implementation(libs.compose.shimmer)
    implementation(libs.timber)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
