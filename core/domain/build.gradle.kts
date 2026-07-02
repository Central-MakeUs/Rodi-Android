plugins {
    id("dororong.rodi.jvm.library")
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.javax.inject)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
