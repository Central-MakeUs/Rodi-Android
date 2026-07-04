package com.dororong.rodi.core.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 세션(액세스/리프레시 토큰)을 EncryptedSharedPreferences에 저장한다.
 * DataStore를 쓰지 않는 이유: 토큰은 평문 보관 금지 대상이라 Android Keystore 기반
 * 암호화가 필요하다(백엔드 문서의 "안전한 저장소" 요구사항).
 */
@Singleton
class AuthTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val accessToken: String? get() = prefs.getString(KEY_ACCESS_TOKEN, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH_TOKEN, null)
    val isLoggedIn: Boolean get() = refreshToken != null

    fun save(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
