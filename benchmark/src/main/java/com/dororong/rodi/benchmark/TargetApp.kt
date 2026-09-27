package com.dororong.rodi.benchmark

// self-instrumenting 모듈이라 instrumentation의 targetContext가 이 벤치마크 APK 자신을 가리킨다.
// 측정·프로파일 대상은 앱이므로 앱의 applicationId를 직접 쓴다.
internal const val TARGET_PACKAGE_NAME = "com.dororong.rodi"
