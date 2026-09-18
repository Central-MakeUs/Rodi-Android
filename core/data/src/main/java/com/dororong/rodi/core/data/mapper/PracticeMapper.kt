package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.practice.FormOptionResponse
import com.dororong.rodi.core.data.source.remote.model.practice.FormResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitResponse
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.practice.Practice
import com.dororong.rodi.core.domain.model.practice.PracticeException
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.domain.model.practice.PracticeVisitResult
import com.dororong.rodi.core.domain.model.practice.SkipReasonForm
import com.dororong.rodi.core.domain.model.practice.SkipReasonOption
import timber.log.Timber

fun PracticeRegisterResponse.toDomain() = Practice(
    practiceId = practiceId,
    status = status.toPracticeStatus(),
    visitCount = visitCount,
    requiredDistanceMeters = requiredDistanceMeters,
)

fun PracticeVisitResponse.toDomain() = PracticeVisitResult(
    visitCount = visitCount,
    addedCertifiedDistanceMeters = addedCertifiedDistanceMeters,
    requiredDistanceMeters = requiredDistanceMeters,
    isCertifiedNow = isCertifiedNow,
    isVerified = isVerified,
    totalDistanceKm = totalDistanceKm,
    levelUp = levelUp,
    newLevel = newLevel.toOnboardingLevelOrNull("newLevel"),
)

fun FormResponse.toDomain() = SkipReasonForm(
    questionId = questionId,
    type = type,
    title = title,
    description = description,
    required = required,
    options = options.sortedBy(FormOptionResponse::order).map(FormOptionResponse::toDomain),
)

/**
 * 연습 상태는 화면 분기를 결정하는 제어 값이라 모르는 값을 임의의 상태로 대체하지 않는다.
 * PLANNED로 덮으면 서버가 새 상태를 추가했을 때 앱이 "아직 방문 안 함"을 자신 있게 잘못 보여준다.
 */
fun String.toPracticeStatus(): PracticeStatus = PracticeStatus.entries.firstOrNull { it.name == this }
    ?: run {
        Timber.w("Unknown practice status value: %s", this)
        throw PracticeException.Unexpected("연습 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.")
    }

private fun FormOptionResponse.toDomain() = SkipReasonOption(
    code = code,
    label = label,
    order = order,
    requiresTextInput = requiresTextInput,
    textInputPlaceholder = textInputPlaceholder,
    textInputMaxLength = textInputMaxLength,
)

private fun String?.toOnboardingLevelOrNull(field: String): OnboardingLevel? = when (this) {
    null -> null
    else -> OnboardingLevel.entries.firstOrNull { it.name == this }.also { mapped ->
        if (mapped == null) Timber.w("Unknown practice %s value: %s", field, this)
    }
}
