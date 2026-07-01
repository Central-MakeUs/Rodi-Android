plugins {
    id("dororong.rodi.android.library")
}

android {
    namespace = "com.dororong.rodi.core.data"
}

dependencies {
    implementation(project(":core:domain"))
}
