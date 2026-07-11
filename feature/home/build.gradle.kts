plugins {
    id("dororong.rodi.android.library.compose")
    id("dororong.rodi.android.hilt")
}

android {
    namespace = "com.dororong.rodi.feature.home"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.lifecycle.compose)
    implementation(libs.bundles.hilt.compose)
    implementation(libs.bundles.kakao.navigation)
    implementation(libs.play.services.location)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.bundles.unit.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.bundles.flow.test)
}
