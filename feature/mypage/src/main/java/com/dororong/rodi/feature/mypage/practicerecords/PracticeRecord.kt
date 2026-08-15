package com.dororong.rodi.feature.mypage.practicerecords

import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem

typealias PracticeRecord = PracticeRecordItem

internal enum class PracticeRecordReviewAction(
    val label: String,
    val isEnabled: Boolean,
) {
    WRITE_REVIEW("후기 작성", true),
    REVIEW_COMPLETED("작성 완료", false),
    PARKING_UNAVAILABLE("작성 불가", false),
}

internal val PracticeRecord.reviewAction: PracticeRecordReviewAction
    get() = when {
        practiceTypes.singleOrNull() == PracticeType.PARKING -> PracticeRecordReviewAction.PARKING_UNAVAILABLE
        hasReview -> PracticeRecordReviewAction.REVIEW_COMPLETED
        else -> PracticeRecordReviewAction.WRITE_REVIEW
    }
