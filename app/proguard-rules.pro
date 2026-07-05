# Rodi 릴리스 R8 규칙.
#
# 이 파일은 앱에서 실제로 사용하는 라이브러리 범위에 맞춰 관리한다. AndroidX,
# Compose, Google Play services, Kotlin은 보통 AAR에 consumer rule을 포함하므로,
# 앱 레벨에서는 축소 후 런타임 리플렉션, JNI, SDK 콜백이 깨질 수 있는 영역만 추가한다.

# SDK 리플렉션, Kotlin suspend 시그니처, Play/AndroidX annotation에 필요한
# 메타데이터를 유지하고, 난독화된 스택트레이스 분석에 필요한 파일/라인 정보를 보존한다.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Manifest에서 참조하는 Android 진입점.
-keep class com.dororong.rodi.RodiApplication { *; }
-keep class com.dororong.rodi.MainActivity { *; }

# native 메서드를 가진 클래스를 보존한다. Kakao Maps는 native 라이브러리와 JNI 호출을 포함한다.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kakao Maps SDK와 Kakao Navi SDK.
# Kakao Maps 2.11.9 AAR에는 consumer ProGuard rule이 포함되어 있지 않다.
# 지도 런타임은 SDK 콜백, JNI, SDK 클래스명으로 로드되는 asset에 의존한다.
-keep class com.kakao.vectormap.** { *; }
-keep class com.kakao.sdk.** { *; }
-dontwarn com.kakao.vectormap.**
-dontwarn com.kakao.sdk.**

# DataStore와 lifecycle-aware coroutine dispatch에서 사용하는 Kotlin coroutines의
# ServiceLoader 항목과 내부 volatile 필드를 보존한다.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.debug.**

# WebView는 Notion 약관 페이지 표시용으로만 사용한다. 앱에 JavaScript interface가 없으므로
# @JavascriptInterface 콜백을 위해 별도로 보존할 앱 클래스는 없다.
