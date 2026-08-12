plugins {
    id("dororong.rodi.jvm.library")
}

dependencies {
    api(project(":core:common"))
    implementation(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.unit.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
}
