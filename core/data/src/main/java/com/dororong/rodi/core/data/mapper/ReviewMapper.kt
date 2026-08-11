package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.review.CursorPageReviewResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormOptionResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReviewRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReviewResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewSummaryResponse
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportFormOption
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.model.review.ReviewSummary
import java.time.Instant
import timber.log.Timber

fun ReviewLevelFilter.toQueryValue(): String? = when (this) {
    ReviewLevelFilter.Mine -> null
    ReviewLevelFilter.All -> "ALL"
    is ReviewLevelFilter.Of -> level.name
}

fun ReviewDraft.toRequest() = ReviewRequest(
    isRecommended = isRecommended,
    difficulty = difficulty.name,
    congestion = congestion.name,
    practiceMethod = when (practiceMethod) {
        PracticeMethod.SOLO -> "SOLO"
        PracticeMethod.WITH_COMPANION -> "ACCOMPANIED"
    },
    content = content,
    caution = caution,
)

fun ReportSubmission.toRequest() = ReportRequest(reason, detail, detailConsistent)

fun CursorPageReviewResponse.toDomain() = CursorPage(
    items = items.mapNotNull(ReviewResponse::toDomain),
    hasNext = hasNext,
    nextCursor = nextCursor,
    totalCount = totalCount,
)

fun ReviewResponse.toDomain(): Review? {
    val mappedLevel = memberLevel.toEnumOrNull<OnboardingLevel>("memberLevel")
    if (mappedLevel == null) return null
    return Review(
        reviewId = reviewId,
        memberId = memberId,
        nickname = nickname,
        memberLevel = mappedLevel,
        isRecommended = isRecommended,
        difficulty = difficulty.toNullableEnum<ReviewDifficulty>("difficulty"),
        congestion = congestion.toNullableEnum<ReviewCongestion>("congestion"),
        practiceMethod = practiceMethod.toPracticeMethodOrNull(),
        content = content,
        caution = caution,
        isMine = isMine,
        isEditable = isEditable,
        isHidden = isHidden,
        createdAt = Instant.parse(createdAt),
    )
}

fun ReviewSummaryResponse.toDomain() = ReviewSummary(
    level = when (level) {
        null, "ALL" -> null
        else -> level.toEnumOrNull<OnboardingLevel>("level")
    },
    totalCount = totalCount,
    recommendCount = recommendCount,
    notRecommendCount = notRecommendCount,
    difficultyCounts = difficultyCounts.toEnumMap<ReviewDifficulty>("difficultyCounts"),
    congestionCounts = congestionCounts.toEnumMap<ReviewCongestion>("congestionCounts"),
    levelCounts = levelCounts.toEnumMap<OnboardingLevel>("levelCounts"),
)

fun ReportFormResponse.toDomain() = ReportForm(
    questionId = questionId,
    title = title,
    description = description,
    required = required,
    options = options.sortedBy(ReportFormOptionResponse::order).map(ReportFormOptionResponse::toDomain),
)

private fun ReportFormOptionResponse.toDomain() = ReportFormOption(
    code = code,
    label = label,
    order = order,
    requiresTextInput = requiresTextInput,
    textInputPlaceholder = textInputPlaceholder,
    textInputMaxLength = textInputMaxLength,
)

private fun String?.toPracticeMethodOrNull(): PracticeMethod? = when (this) {
    null -> null
    "SOLO" -> PracticeMethod.SOLO
    "ACCOMPANIED" -> PracticeMethod.WITH_COMPANION
    else -> null.also { Timber.w("Unknown review practiceMethod value: %s", this) }
}

private inline fun <reified T : Enum<T>> String?.toNullableEnum(field: String): T? =
    this?.toEnumOrNull<T>(field)

private inline fun <reified T : Enum<T>> String.toEnumOrNull(field: String): T? =
    enumValues<T>().firstOrNull { it.name == this }.also { mapped ->
        if (mapped == null) Timber.w("Unknown review %s value: %s", field, this)
    }

private inline fun <reified T : Enum<T>> Map<String, Long>.toEnumMap(field: String): Map<T, Long> =
    mapNotNull { (key, count) -> key.toEnumOrNull<T>(field)?.let { it to count } }.toMap()
