package com.dororong.rodi.feature.mypage.practicerecords

import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

private val PracticeRecordDateFormatter = DateTimeFormatter.ofPattern("yy.MM.dd").withZone(ZoneId.systemDefault())

// visitedAt이 없는 VISITED 기록(서버가 시각 없이 방문만 확정한 경우)도 있을 수 있어
// 그때는 빈 문자열 대신 "방문 완료"로 최소한의 상태는 보여준다.
internal fun PracticeRecord.visitedDateLabel(): String =
    visitedAt?.let(PracticeRecordDateFormatter::format)
        ?: if (status == PracticeStatus.VISITED) "방문 완료" else ""
