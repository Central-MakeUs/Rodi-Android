plugins {
    id("dororong.rodi.android.feature")
}

android {
    namespace = "com.dororong.rodi.feature.entry"
}

dependencies {
    implementation(project(":core:common"))
    api(project(":core:domain"))
    implementation(libs.bundles.coil)
    implementation(libs.timber)
    testImplementation(libs.bundles.unit.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.bundles.flow.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
