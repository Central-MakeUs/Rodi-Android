package com.dororong.rodi.core.data

import com.dororong.rodi.core.domain.AuthRepository
import javax.inject.Inject

// 로그인 API 확정 전 placeholder(NetworkModule.BASE_URL과 동일 사유).
// 현재는 카카오 액세스 토큰 획득 성공을 곧 로그인 성공으로 간주한다.
// 서버 연동 시 이 안에서 Retrofit 호출로 토큰을 전달하고 응답(세션/회원 상태 등)을 반영하도록 교체.
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    override suspend fun loginWithKakao(kakaoAccessToken: String) {
    }
}
