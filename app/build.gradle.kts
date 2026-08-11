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
// 카카오 디벨로퍼스 콘솔에 debug(.dev) 패키지·키해시를 별도 네이티브 앱키로 등록해뒀다면 여기 지정한다.
// 없으면 기본 키를 그대로 쓴다(기존 local.properties와 하위 호환).
val kakaoNativeAppKeyDebug: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY_DEV")
    ?.trim()?.takeIf(String::isNotEmpty) ?: kakaoNativeAppKey
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
        versionCode = 11
        versionName = "1.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "CLARITY_PROJECT_ID", "\"\"")
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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
        debug {
            applicationIdSuffix = ".dev"
            // 카카오톡 앱 로그인은 로컬에서 패키지명+키해시를 네이티브 앱키의 등록된 플랫폼과
            // 대조한다. .dev 패키지는 기본 키가 아니라 별도 Dev Native AppKey에 등록돼 있으므로
            // BuildConfig와 매니페스트 리다이렉트 스킴(kakao${KAKAO_NATIVE_APP_KEY}) 둘 다 맞춰줘야
            // 웹 로그인으로 조용히 폴백되지 않는다.
            buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKeyDebug\"")
            manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKeyDebug
            buildConfigField("String", "CLARITY_PROJECT_ID", "\"xuel7v1h92\"")
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "CLARITY_PROJECT_ID", "\"xuepsqfoyk\"")
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
            // release의 prod Clarity ID를 그대로 물려받으면 자동화된 벤치마크 실행까지 prod 세션으로 잡힌다.
            buildConfigField("String", "CLARITY_PROJECT_ID", "\"\"")
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
    implementation(libs.clarity.compose)
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
