import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.aboutlibraries.android)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("dororong.rodi.android.hilt")
}

val localProperties = Properties().apply {
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) localProps.inputStream().use { load(it) }
}

val releaseSigningProperties = Properties().apply {
    val signingProps = rootProject.file("keystore.properties")
    if (signingProps.exists()) signingProps.inputStream().use { load(it) }
}

fun Properties.requireNotBlank(key: String, source: String): String =
    getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("${source}에 '$key'가 설정되지 않았습니다.")

val kakaoNativeAppKey: String = localProperties.requireNotBlank("KAKAO_NATIVE_APP_KEY", "local.properties")
val hasLocalReleaseSigning = releaseSigningProperties.isNotEmpty()

android {
    namespace = "com.dororong.rodi"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.dororong.rodi"
        minSdk = 30
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
    }

    signingConfigs {
        if (hasLocalReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(
                    releaseSigningProperties.requireNotBlank("storeFile", "keystore.properties"),
                )
                storePassword = releaseSigningProperties.requireNotBlank("storePassword", "keystore.properties")
                keyAlias = releaseSigningProperties.requireNotBlank("keyAlias", "keystore.properties")
                keyPassword = releaseSigningProperties.requireNotBlank("keyPassword", "keystore.properties")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:entry"))
    implementation(project(":feature:home"))
    implementation(project(":feature:mypage"))
    implementation(project(":feature:settings"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.bundles.navigation3)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.bundles.hilt.compose)
    implementation(libs.bundles.kakao.navigation)
    // AndroidManifest.xml이 이 라이브러리의 AuthCodeHandlerActivity를 직접 참조한다.
    // feature:auth를 통해 transitive로 포함돼 런타임엔 문제없지만, lint의 MissingClass
    // 검사는 app 모듈의 직접 의존성만 보므로 명시적으로 추가한다.
    implementation(libs.kakao.user)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    baselineProfile(project(":benchmark"))
}
