package com.dororong.rodi.feature.auth

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@EntryPoint
@InstallIn(ActivityComponent::class)
interface KakaoLoginManagerEntryPoint {
    fun kakaoLoginManager(): KakaoLoginManager
}

@ActivityScoped
class KakaoLoginManager @Inject constructor(
    @ActivityContext private val context: Context,
) {
    fun login(onSuccess: (accessToken: String) -> Unit, onFailure: (message: String) -> Unit) {
        val resultHandler: (OAuthToken?, Throwable?) -> Unit = { token, _ ->
            when {
                token?.accessToken != null -> onSuccess(token.accessToken)
                else -> onFailure("로그인에 실패했습니다. 잠시 후 다시 시도해주세요.")
            }
        }

        if (!UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = resultHandler)
            return
        }

        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
            when {
                token?.accessToken != null -> onSuccess(token.accessToken)
                error is ClientError && error.reason == ClientErrorCause.Cancelled ->
                    onFailure("로그인이 취소되었습니다.")
                error != null ->
                    UserApiClient.instance.loginWithKakaoAccount(context, callback = resultHandler)
                else -> onFailure("로그인에 실패했습니다. 잠시 후 다시 시도해주세요.")
            }
        }
    }
}
