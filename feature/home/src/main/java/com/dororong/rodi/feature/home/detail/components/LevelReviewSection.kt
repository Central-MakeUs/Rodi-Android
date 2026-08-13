package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun LevelReviewSection(
    totalCount: Long,
    recommendCount: Long,
    selectedLevel: OnboardingLevel,
    difficultyCounts: Map<ReviewDifficulty, Long>,
    review: Review?,
    onSelectLevel: (OnboardingLevel) -> Unit,
    onAllClick: () -> Unit,
    onWriteReviewClick: () -> Unit,
    onEditReviewClick: (Review) -> Unit,
    onDeleteReviewClick: (Review) -> Unit,
    onReportReviewClick: (Review) -> Unit,
    onBlockMemberClick: (Review) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollableState? = null,
) {
    val topDifficulty = difficultyCounts.topDifficulty()
    val hasReview = review != null

    Column(modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SectionHeader(showAllLink = totalCount > 0, onAllClick = onAllClick)

            // 코스에 후기가 하나라도 있으면(다른 레벨 포함) 요약 노출. difficultyCounts는 선택한
            // 레벨 기준이라 이걸로 노출 여부를 걸면 후기 없는 레벨로 바꿀 때마다 요약이 통째로
            // 사라진다 — totalCount(레벨 무관 전체 후기 수)로 판단해야 한다.
            if (totalCount > 0) {
                SummaryRow(
                    recommendCount = recommendCount,
                    selectedLevel = selectedLevel,
                    topDifficulty = topDifficulty,
                    topDifficultyCount = topDifficulty?.let { difficultyCounts[it] } ?: 0L,
                    onSelectLevel = onSelectLevel,
                    scrollState = scrollState,
                )
            }
        }

        if (!hasReview) {
            // Figma 두 빈 상태(후기 아예 없음 / 선택한 레벨만 없음) 모두 헤더와 프롬프트 사이에
            // 얇은 구분선이 있다. WriteReviewPrompt 내부의 top padding(32dp)이 구분선~문구 간격을
            // 이미 맞춰주므로 divider 뒤에 별도 Spacer는 두지 않는다.
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RodiTheme.colors.gray100)
            WriteReviewPrompt(onWriteReviewClick = onWriteReviewClick)
        } else {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RodiTheme.colors.gray100)

            Spacer(Modifier.height(12.dp))
            ReviewCard(
                review = review,
                onEditReviewClick = onEditReviewClick,
                onDeleteReviewClick = onDeleteReviewClick,
                onReportReviewClick = onReportReviewClick,
                onBlockMemberClick = onBlockMemberClick,
                modifier = Modifier.padding(horizontal = 16.dp),
                scrollState = scrollState,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RodiTheme.colors.gray100)
            Spacer(Modifier.height(12.dp))
            WriteReviewPrompt(onWriteReviewClick = onWriteReviewClick)
        }

        Spacer(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(RodiTheme.colors.gray100),
        )
    }
}

@Composable
private fun SectionHeader(
    showAllLink: Boolean,
    onAllClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "레벨별 후기",
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
        )
        if (showAllLink) {
            Row(
                modifier = Modifier.clickable(onClick = onAllClick),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "전체보기",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray400,
                )
                Icon(
                    painter = painterResource(com.dororong.rodi.core.ui.R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = RodiTheme.colors.gray400,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    recommendCount: Long,
    selectedLevel: OnboardingLevel,
    // 선택한 레벨에 후기가 없으면 null — 그 레벨만의 최다 난이도가 없다는 뜻이라 칩 없이 "0명"만 보여준다.
    topDifficulty: ReviewDifficulty?,
    topDifficultyCount: Long,
    onSelectLevel: (OnboardingLevel) -> Unit,
    scrollState: ScrollableState?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        // Figma는 이 통계 묶음 전체를 섹션 우측 끝에 붙인다("전체보기"·"자세히 보기" 등 다른
        // 우측 정렬 요소와 같은 라인). spacedBy(30dp)로 안쪽 간격은 유지하면서 묶음 자체를 끝으로 민다.
        horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(51.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "추천해요",
                style = RodiTheme.typography.body2SemiBold,
                color = RodiTheme.colors.gray800,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_thumbs_up),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "${recommendCount}명",
                    style = RodiTheme.typography.body1SemiBold,
                    color = RodiTheme.colors.black,
                )
            }
        }

        Box(
            Modifier
                .width(1.dp)
                .height(48.dp)
                .background(RodiTheme.colors.gray300),
        )

        Column(
            modifier = Modifier.width(202.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "난이도",
                    style = RodiTheme.typography.body2SemiBold,
                    color = RodiTheme.colors.gray800,
                )
                LevelDropdown(
                    selectedLevel = selectedLevel,
                    onSelectLevel = onSelectLevel,
                    scrollState = scrollState,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (topDifficulty != null) {
                    DifficultyChip(difficulty = topDifficulty, emphasized = false)
                }
                Text(
                    text = "${topDifficultyCount}명",
                    style = RodiTheme.typography.body1SemiBold,
                    color = RodiTheme.colors.black,
                )
            }
        }
    }
}

@Composable
internal fun LevelDropdown(
    selectedLevel: OnboardingLevel,
    onSelectLevel: (OnboardingLevel) -> Unit,
    scrollState: ScrollableState?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val levels = OnboardingLevel.entries

    Box(modifier) {
        Row(
            modifier = Modifier.clickable { expanded = true },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedLevel.displayName,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray700,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = "레벨 선택",
                tint = RodiTheme.colors.gray700,
                modifier = Modifier.size(16.dp),
            )
        }
        RodiPopupMenu(
            expanded = expanded,
            items = levels.map { it.displayName },
            onSelect = { index ->
                expanded = false
                onSelectLevel(levels[index])
            },
            onDismissRequest = { expanded = false },
            menuWidth = 103.dp,
            scrollState = scrollState,
        )
    }
}

@Composable
private fun WriteReviewPrompt(onWriteReviewClick: () -> Unit) {
    val buttonShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "여러분의 연습 경험을 공유해주세요!",
                modifier = Modifier.width(177.dp),
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .width(177.dp)
                    .clip(buttonShape)
                    .border(1.dp, RodiTheme.colors.primary600, buttonShape)
                    .clickable(onClick = onWriteReviewClick)
                    .padding(horizontal = 20.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "후기 쓰기",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.primary600,
                )
            }
        }
    }
}

private val previewSectionReview = Review(
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
private fun PreviewSection(
    totalCount: Long,
    recommendCount: Long,
    difficultyCounts: Map<ReviewDifficulty, Long>,
    review: Review?,
) {
    RodiTheme {
        LevelReviewSection(
            totalCount = totalCount,
            recommendCount = recommendCount,
            selectedLevel = OnboardingLevel.SEED,
            difficultyCounts = difficultyCounts,
            review = review,
            onSelectLevel = {},
            onAllClick = {},
            onWriteReviewClick = {},
            onEditReviewClick = {},
            onDeleteReviewClick = {},
            onReportReviewClick = {},
            onBlockMemberClick = {},
        )
    }
}

@Preview(name = "레벨별 후기 - 기본", showBackground = true, widthDp = 375)
@Composable
private fun LevelReviewSectionPreview() = PreviewSection(
    totalCount = 30,
    recommendCount = 15,
    difficultyCounts = mapOf(ReviewDifficulty.VERY_EASY to 30L),
    review = previewSectionReview,
)

@Preview(name = "레벨별 후기 - 후기 아예 없음", showBackground = true, widthDp = 375)
@Composable
private fun LevelReviewSectionEmptyPreview() = PreviewSection(
    totalCount = 0,
    recommendCount = 0,
    difficultyCounts = emptyMap(),
    review = null,
)

@Preview(name = "레벨별 후기 - 내 레벨 없음(요약 데이터 있음)", showBackground = true, widthDp = 375)
@Composable
private fun LevelReviewSectionNoReviewWithSummaryDataPreview() = PreviewSection(
    totalCount = 30,
    recommendCount = 15,
    difficultyCounts = mapOf(ReviewDifficulty.NORMAL to 8L, ReviewDifficulty.HARD to 3L),
    review = null,
)

@Preview(name = "레벨별 후기 - 선택한 레벨은 후기 0건(다른 레벨은 있음)", showBackground = true, widthDp = 375)
@Composable
private fun LevelReviewSectionSelectedLevelEmptyPreview() = PreviewSection(
    totalCount = 30,
    recommendCount = 15,
    difficultyCounts = emptyMap(),
    review = null,
)
