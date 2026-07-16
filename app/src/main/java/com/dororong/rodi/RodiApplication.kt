package com.dororong.rodi

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RodiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        runCatching {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }.onFailure { error ->
            Timber.e(error, "Kakao SDK initialization failed.")
        }
    }
}
