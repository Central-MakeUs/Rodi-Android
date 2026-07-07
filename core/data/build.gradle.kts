import java.util.Properties
import org.gradle.api.tasks.testing.Test

plugins {
    id("dororong.rodi.android.library")
    id("dororong.rodi.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) localProps.inputStream().use { load(it) }
}
val kakaoRestApiKey: String = localProperties.getProperty("KAKAO_REST_API_KEY", "")

android {
    namespace = "com.dororong.rodi.core.data"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":core:common"))
    api(project(":core:domain"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.security.crypto)
    implementation(libs.hilt.android)
    implementation(libs.kakao.maps)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.timber)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
