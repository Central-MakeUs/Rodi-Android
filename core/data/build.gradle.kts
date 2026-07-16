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

fun Properties.requireNotBlank(key: String): String =
    getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("local.properties에 '$key'가 설정되지 않았습니다.")

val kakaoRestApiKey: String = localProperties.requireNotBlank("KAKAO_REST_API_KEY")

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
    implementation(libs.bundles.room.runtime)
    implementation(libs.hilt.android)
    implementation(libs.kakao.maps)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.bundles.network.runtime)
    implementation(libs.timber)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.bundles.unit.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
}
