plugins {
    id("dororong.rodi.android.library.compose")
}

android {
    namespace = "com.dororong.rodi.feature.home"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kakao.maps)
    implementation(libs.kakao.navi)
    implementation(libs.play.services.location)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
