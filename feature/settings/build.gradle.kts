plugins {
    id("dororong.rodi.android.feature")
}

android {
    namespace = "com.dororong.rodi.feature.settings"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.aboutlibraries.compose.m3)
    testImplementation(libs.bundles.unit.test)
    testImplementation(libs.bundles.flow.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
