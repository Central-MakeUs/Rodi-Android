plugins {
    id("dororong.rodi.android.feature")
}

android {
    namespace = "com.dororong.rodi.feature.mypage"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(libs.clarity.compose)
    testImplementation(libs.bundles.unit.test)
    testImplementation(libs.bundles.flow.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
