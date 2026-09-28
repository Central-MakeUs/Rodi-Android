import java.util.Properties

plugins {
    id("dororong.rodi.android.application")
    alias(libs.plugins.aboutlibraries.android)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.kotlin.serialization)
    id("dororong.rodi.android.hilt")
    id("dororong.rodi.kover")
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

    defaultConfig {
        applicationId = "com.dororong.rodi"
        versionCode = 25
        versionName = "1.5.0-alpha02"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "CLARITY_PROJECT_ID", "\"\"")
        // 내부 테스트 트랙은 release 빌드라 BuildConfig.DEBUG로는 가를 수 없다. 릴리스 워크플로의
        // prerelease 판정과 같은 기준(-alpha/-beta/-rc)으로, 정식 버전에서는 빌드 시점에 빠진다.
        buildConfigField(
            "boolean",
            "SHOW_TEST_MENU",
            Regex("-(alpha|beta|rc)").containsMatchIn(checkNotNull(versionName)).toString(),
        )
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
            buildConfigField("boolean", "SHOW_TEST_MENU", "true")
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
        // Baseline Profile 플러그인이 release를 바탕으로 만드는 빌드 타입이다. benchmarkRelease는
        // Macrobenchmark 측정, nonMinifiedRelease는 Baseline Profile 생성에 쓰인다. 여기서 먼저 만들면
        // 플러그인이 release를 복사하지 않고 자기 강제 속성(debuggable·profileable 등)만 덮으므로,
        // release를 직접 복사한 뒤 release에서 물려받으면 안 되는 두 가지만 바꾼다.
        // - prod Clarity ID를 물려받으면 자동화된 실행까지 prod 세션으로 잡힌다.
        // - release 키 없이도 로컬·에뮬레이터에서 설치할 수 있도록 debug 키로 서명한다.
        listOf("benchmarkRelease", "nonMinifiedRelease").forEach { name ->
            create(name) {
                initWith(getByName("release"))
                signingConfig = signingConfigs.getByName("debug")
                buildConfigField("String", "CLARITY_PROJECT_ID", "\"\"")
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:entry"))
    implementation(project(":feature:home"))
    implementation(project(":feature:mypage"))
    implementation(project(":feature:course-registration"))
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
    testImplementation(libs.bundles.unit.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    baselineProfile(project(":benchmark"))
}
