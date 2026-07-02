plugins {
    id("dororong.rodi.jvm.library")
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.javax.inject)
}
