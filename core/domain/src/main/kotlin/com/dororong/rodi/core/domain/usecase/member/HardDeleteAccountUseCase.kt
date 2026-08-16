package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.usecase.onboarding.ClearOnboardingDataUseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class HardDeleteAccountUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val clearOnboardingData: ClearOnboardingDataUseCase,
) {
    // 계정 삭제(서버 반영 + 토큰/캐시 정리)는 이미 되돌릴 수 없다. 그 뒤에 붙는 온보딩 로컬
    // 정리가 실패했다고 해서 "삭제 실패"로 보고하면 안 된다 — 실제로는 삭제됐는데 사용자는
    // 실패했다고 믿고, 이미 지워진 토큰으로 재시도하다 매번 인증 오류만 겪게 된다.
    suspend operator fun invoke(): Result<Unit> = runSuspendCatching {
        memberRepository.hardDelete()
        try {
            clearOnboardingData()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // 계정은 이미 삭제됐다. 로컬 정리 실패는 삼킨다.
        }
    }
}
