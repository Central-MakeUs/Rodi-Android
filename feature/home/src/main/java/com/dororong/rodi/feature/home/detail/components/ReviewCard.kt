package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.ui.components.RodiPopupMenu
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R
import java.time.Instant

private const val ReportIndex = 0
private const val EditIndex = 0

@Composable
fun ReviewCard(
    review: Review,
    onEditReviewClick: (Review) -> Unit,
    onDeleteReviewClick: (Review) -> Unit,
    onReportReviewClick: (Review) -> Unit,
    onBlockMemberClick: (Review) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollableState? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val menuItems = review.menuItems()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(RodiTheme.colors.white, CircleShape)
                        .border(1.dp, RodiTheme.colors.primary600, CircleShape),
                )
                Text(
                    text = review.nickname,
                    style = RodiTheme.typography.body1SemiBold,
                    color = RodiTheme.colors.black,
                )
            }
            Box {
                Icon(
                    painter = painterResource(R.drawable.ic_more_horizontal),
                    contentDescription = "후기 더보기",
                    tint = RodiTheme.colors.gray800,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { menuExpanded = true },
                )
                RodiPopupMenu(
                    expanded = menuExpanded,
                    items = menuItems,
                    onSelect = { index ->
                        menuExpanded = false
                        review.dispatchMenuSelection(
                            index = index,
                            onEditReviewClick = onEditReviewClick,
                            onDeleteReviewClick = onDeleteReviewClick,
                            onReportReviewClick = onReportReviewClick,
                            onBlockMemberClick = onBlockMemberClick,
                        )
                    },
                    onDismissRequest = { menuExpanded = false },
                    scrollState = scrollState,
                )
            }
        }

        review.practiceMethod?.let { method ->
            Text(
                text = method.label,
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
                modifier = Modifier
                    .background(RodiTheme.colors.gray50, RoundedCornerShape(2.dp))
                    .border(1.dp, RodiTheme.colors.gray200, RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            review.content?.takeIf { it.isNotBlank() }?.let { content ->
                Text(
                    text = content,
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray800,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = review.createdAt.toReviewDateText(),
                style = RodiTheme.typography.caption2Medium,
                color = RodiTheme.colors.gray500,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun Review.menuItems(): List<String> = when {
    !isMine -> listOf("신고하기", "차단")
    isEditable -> listOf("수정하기", "삭제하기")
    else -> listOf("삭제하기")
}

private fun Review.dispatchMenuSelection(
    index: Int,
    onEditReviewClick: (Review) -> Unit,
    onDeleteReviewClick: (Review) -> Unit,
    onReportReviewClick: (Review) -> Unit,
    onBlockMemberClick: (Review) -> Unit,
) {
    when {
        !isMine && index == ReportIndex -> onReportReviewClick(this)
        !isMine -> onBlockMemberClick(this)
        isEditable && index == EditIndex -> onEditReviewClick(this)
        else -> onDeleteReviewClick(this)
    }
}

private val previewReview = Review(
    reviewId = 1L,
    memberId = 2L,
    nickname = "초보초보",
    memberLevel = OnboardingLevel.SEED,
    isRecommended = true,
    difficulty = ReviewDifficulty.VERY_EASY,
    congestion = ReviewCongestion.QUIET,
    practiceMethod = PracticeMethod.SOLO,
    content = "자전거 타기 좋은 곳자전거 타기 좋은 곳자전거 타기 좋은 곳",
    caution = null,
    isMine = false,
    isEditable = false,
    isHidden = false,
    createdAt = Instant.parse("2026-05-10T00:00:00Z"),
)

@Composable
private fun PreviewCard(review: Review) {
    RodiTheme {
        ReviewCard(
            review = review,
            onEditReviewClick = {},
            onDeleteReviewClick = {},
            onReportReviewClick = {},
            onBlockMemberClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "후기 - 타인(신고·차단)", showBackground = true, widthDp = 375)
@Composable
private fun ReviewCardOtherPreview() = PreviewCard(previewReview)

@Preview(name = "후기 - 내 현재 레벨(수정·삭제)", showBackground = true, widthDp = 375)
@Composable
private fun ReviewCardMineEditablePreview() =
    PreviewCard(previewReview.copy(isMine = true, isEditable = true, nickname = "흐름타는 고슴도치"))

@Preview(name = "후기 - 내 이전 레벨(삭제만)", showBackground = true, widthDp = 375)
@Composable
private fun ReviewCardMineLockedPreview() =
    PreviewCard(previewReview.copy(isMine = true, isEditable = false, nickname = "흐름타는 고슴도치"))

@Preview(name = "후기 - 동승자 연습·최대 길이", showBackground = true, widthDp = 375)
@Composable
private fun ReviewCardLongPreview() = PreviewCard(
    previewReview.copy(
        practiceMethod = PracticeMethod.WITH_COMPANION,
        content = "자전거 타기 좋은 곳".repeat(30),
    ),
)

@Preview(name = "후기 - 내용 없음", showBackground = true, widthDp = 375)
@Composable
private fun ReviewCardNoContentPreview() =
    PreviewCard(previewReview.copy(content = null, practiceMethod = null))
