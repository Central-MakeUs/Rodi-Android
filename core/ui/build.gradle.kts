plugins {
    id("dororong.rodi.android.library.compose")
}

android {
    namespace = "com.dororong.rodi.core.ui"
}

dependencies {
    implementation(project(":core:common"))
}
