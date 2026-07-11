plugins {
    id("dororong.rodi.android.library.compose")
    id("dororong.rodi.android.hilt")
}

android {
    namespace = "com.dororong.rodi.feature.auth"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.hilt.compose)
    implementation(libs.bundles.lifecycle.compose)
    implementation(libs.kakao.user)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.bundles.unit.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.bundles.flow.test)
}
