plugins {
    id("dororong.rodi.android.feature")
}

android {
    namespace = "com.dororong.rodi.feature.entry"
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:common"))
    api(project(":core:domain"))
    implementation(libs.bundles.coil)
    implementation(libs.timber)
    testImplementation(libs.bundles.unit.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.bundles.flow.test)
    testImplementation(libs.bundles.robolectric.test)
    testRuntimeOnly(libs.junit.vintage.engine)
    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
