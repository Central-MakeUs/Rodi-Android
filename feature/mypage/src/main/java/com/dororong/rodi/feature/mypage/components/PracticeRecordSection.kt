package com.dororong.rodi.feature.mypage.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.feature.mypage.practicerecords.PracticeRecord
import com.dororong.rodi.feature.mypage.practicerecords.reviewAction
import com.dororong.rodi.feature.mypage.practicerecords.visitedDateLabel
import java.time.Instant

@Composable
internal fun PracticeRecordSection(
    records: List<PracticeRecord>,
    onAllClick: () -> Unit,
    onWriteReviewClick: (Long, String) -> Unit,
    errorMessage: String? = null,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visitedRecords = records.filter { it.status == PracticeStatus.VISITED }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "연습기록",
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
            )
            if (visitedRecords.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onAllClick),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "전체보기",
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.gray400,
                    )
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = RodiTheme.colors.gray400,
                        modifier = Modifier.width(16.dp),
                    )
                }
            }
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(141.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage, style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray600)
                    RodiButton(
                        text = "다시 시도",
                        onClick = onRetry,
                        fillMaxWidth = false,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else if (visitedRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(141.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "아직 연습기록이 없어요!",
                        style = RodiTheme.typography.body2SemiBold,
                        color = RodiTheme.colors.gray600,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "가까운 연습 장소부터 천천히 시작해볼까요?",
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.gray600,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visitedRecords.forEach { record ->
                    PracticeRecordCard(
                        record = record,
                        onWriteReviewClick = { onWriteReviewClick(record.placeId, record.placeName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeRecordCard(
    record: PracticeRecord,
    onWriteReviewClick: () -> Unit,
) {
    val reviewAction = record.reviewAction
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .width(179.dp)
            .height(141.dp)
            .border(1.dp, RodiTheme.colors.primary50, shape)
            .clip(shape)
            .padding(15.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = record.placeName,
                style = RodiTheme.typography.body2SemiBold,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = record.visitedDateLabel(),
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            record.practiceTypes.take(2).forEach { type ->
                Text(
                    text = type.label,
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray600,
                    modifier = Modifier
                        .background(RodiTheme.colors.gray200, RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Surface(
            onClick = { if (reviewAction.isEnabled) onWriteReviewClick() },
            enabled = reviewAction.isEnabled,
            shape = RoundedCornerShape(8.dp),
            color = if (reviewAction.isEnabled) RodiTheme.colors.white else RodiTheme.colors.gray300,
            border = if (reviewAction.isEnabled) BorderStroke(1.dp, RodiTheme.colors.primary600) else null,
            modifier = Modifier.fillMaxWidth().height(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = reviewAction.label,
                    style = RodiTheme.typography.body3Medium,
                    color = if (reviewAction.isEnabled) RodiTheme.colors.primary600 else RodiTheme.colors.gray500,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val PreviewRecords = listOf(
    PracticeRecord(
        practiceId = 1,
        placeId = 1,
        placeName = "망원한강공원",
        visitedAt = Instant.parse("2026-05-10T00:00:00Z"),
        visitCount = 1,
        practiceTypes = listOf(PracticeType.ROUNDABOUT, PracticeType.HIGHWAY_ENTRY),
        isVerified = true,
        hasReview = false,
        status = PracticeStatus.VISITED,
    ),
    PracticeRecord(
        practiceId = 2,
        placeId = 2,
        placeName = "용산구 교차로·회전",
        visitedAt = Instant.parse("2026-05-09T00:00:00Z"),
        visitCount = 2,
        practiceTypes = listOf(PracticeType.LANE_CHANGE, PracticeType.PARKING),
        isVerified = true,
        hasReview = true,
        status = PracticeStatus.VISITED,
    ),
    PracticeRecord(
        practiceId = 3,
        placeId = 3,
        placeName = "망원 공영주차장",
        visitedAt = Instant.parse("2026-05-08T00:00:00Z"),
        visitCount = 1,
        practiceTypes = listOf(PracticeType.PARKING),
        isVerified = true,
        hasReview = false,
        status = PracticeStatus.VISITED,
    ),
)

@Preview(name = "연습기록 - 빈 상태", showBackground = true, widthDp = 375)
@Composable
private fun PracticeRecordSectionEmptyPreview() = RodiTheme {
    PracticeRecordSection(emptyList(), {}, { _, _ -> }, onRetry = {})
}

@Preview(name = "연습기록 - 카드 1개", showBackground = true, widthDp = 375)
@Composable
private fun PracticeRecordSectionSinglePreview() = RodiTheme {
    PracticeRecordSection(PreviewRecords.take(1), {}, { _, _ -> }, onRetry = {})
}

@Preview(name = "연습기록 - 카드 여러 개", showBackground = true, widthDp = 375)
@Composable
private fun PracticeRecordSectionMultiplePreview() = RodiTheme {
    PracticeRecordSection(PreviewRecords, {}, { _, _ -> }, onRetry = {})
}
