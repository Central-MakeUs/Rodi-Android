// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover) apply false
    id("dororong.rodi.kover")
}

dependencies {
    kover(project(":app"))
    kover(project(":core:common"))
    kover(project(":core:data"))
    kover(project(":core:domain"))
    kover(project(":core:ui"))
    kover(project(":feature:auth"))
    kover(project(":feature:course-registration"))
    kover(project(":feature:entry"))
    kover(project(":feature:home"))
    kover(project(":feature:mypage"))
    kover(project(":feature:settings"))
}
