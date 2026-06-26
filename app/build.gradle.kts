import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// local.properties 에서 카카오/T맵 키를 읽는다. (커밋되지 않음)
val localProperties = Properties().apply {
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) localProps.inputStream().use { load(it) }
}
val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY", "")
val kakaoRestApiKey: String = localProperties.getProperty("KAKAO_REST_API_KEY", "")
val tmapAppKey: String = localProperties.getProperty("TMAP_APP_KEY", "")

android {
    namespace = "com.cmc.routi"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.cmc.routi"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 카카오 SDK 초기화용 네이티브 앱 키 (BuildConfig 로 노출)
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        // 카카오모빌리티 길찾기 REST API 키
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
        // T맵 앱연동 SDK 인증 키 (TMapTapi.setSKTMapAuthentication)
        buildConfigField("String", "TMAP_APP_KEY", "\"$tmapAppKey\"")
        // 카카오맵 SDK 가 매니페스트에서 참조하는 키 (선택)
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // 첫 실행 게이트 완료 플래그 저장
    implementation(libs.androidx.datastore.preferences)
    // 카카오맵 SDK (지도 표시)
    implementation(libs.kakao.maps)
    // 현재 위치 (FusedLocationProvider)
    implementation(libs.play.services.location)
    // T맵 앱연동 SDK: app/libs/tmap.jar 파일이 있을 때만 추가 (없어도 빌드 green)
    if (file("libs/tmap.jar").exists()) {
        implementation(files("libs/tmap.jar"))
    }
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
