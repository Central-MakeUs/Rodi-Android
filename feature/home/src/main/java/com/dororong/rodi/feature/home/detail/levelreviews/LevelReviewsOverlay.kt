package com.dororong.rodi.feature.home.detail.levelreviews

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.feature.home.detail.components.BookmarkButton
import com.dororong.rodi.feature.home.detail.components.DifficultyBarChart
import com.dororong.rodi.feature.home.detail.components.LevelDropdown
import com.dororong.rodi.feature.home.detail.components.ReviewCard
import kotlinx.coroutines.flow.filter
import java.time.Instant

private const val LoadMoreThreshold = 3

@Composable
fun LevelReviewsOverlay(
    recommendCount: Long,
    selectedLevel: OnboardingLevel,
    difficultyCounts: Map<ReviewDifficulty, Long>,
    reviews: List<Review>,
    isBookmarked: Boolean,
    isBookmarkUpdating: Boolean,
    onClose: () -> Unit,
    onSelectLevel: (OnboardingLevel) -> Unit,
    onLoadInitial: () -> Unit,
    onLoadNext: () -> Unit,
    onBookmarkClick: () -> Unit,
    onNavigate: () -> Unit,
    onEditReviewClick: (Review) -> Unit,
    onDeleteReviewClick: (Review) -> Unit,
    onReportReviewClick: (Review) -> Unit,
    onBlockMemberClick: (Review) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { onLoadInitial() }

    // 목록 끝에 다가가면 다음 페이지. 커서가 없으면 ViewModel이 무시하므로 여기서 따로 막지 않는다.
    val currentOnLoadNext by rememberUpdatedState(onLoadNext)
    val shouldLoadMore by remember(reviews.size) {
        derivedStateOf {
            if (reviews.isEmpty()) return@derivedStateOf false
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= info.totalItemsCount - LoadMoreThreshold
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shouldLoadMore }.filter { it }.collect { currentOnLoadNext() }
    }

    Surface(modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(Modifier.statusBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
            ) {
                RodiIconButton(
                    painter = painterResource(com.dororong.rodi.core.ui.R.drawable.ic_chevron_left),
                    onClick = onClose,
                    iconSize = 20.dp,
                    contentDescription = "뒤로가기",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(
                    text = "레벨별 후기",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                item {
                    RecommendBlock(
                        recommendCount = recommendCount,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                    HorizontalDivider(
                        thickness = 2.dp,
                        color = RodiTheme.colors.gray100,
                    )
                }
                item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(26.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "난이도",
                                    style = RodiTheme.typography.body1SemiBold,
                                    color = RodiTheme.colors.black,
                                )
                                LevelDropdown(
                                    selectedLevel = selectedLevel,
                                    onSelectLevel = onSelectLevel,
                                    scrollState = listState,
                                )
                            }
                            if (difficultyCounts.isNotEmpty()) {
                                DifficultyBarChart(difficultyCounts)
                            }
                        }
                        HorizontalDivider(color = RodiTheme.colors.gray100)
                }
                items(reviews, key = { it.reviewId }) { review ->
                    ReviewCard(
                        review = review,
                        onEditReviewClick = onEditReviewClick,
                        onDeleteReviewClick = onDeleteReviewClick,
                        onReportReviewClick = onReportReviewClick,
                        onBlockMemberClick = onBlockMemberClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        scrollState = listState,
                    )
                }
            }

            HorizontalDivider(color = RodiTheme.colors.gray200)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BookmarkButton(
                    isBookmarked = isBookmarked,
                    onClick = onBookmarkClick,
                    enabled = !isBookmarkUpdating,
                )
                RodiButton(
                    text = "연습하러 가기",
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RecommendBlock(
    recommendCount: Long,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "추천해요",
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
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
}

@Composable
private fun PreviewOverlay(
    recommendCount: Long,
    difficultyCounts: Map<ReviewDifficulty, Long>,
    reviews: List<Review>,
) {
    RodiTheme {
        LevelReviewsOverlay(
            recommendCount = recommendCount,
            selectedLevel = OnboardingLevel.ROOKIE,
            difficultyCounts = difficultyCounts,
            reviews = reviews,
            isBookmarked = false,
            isBookmarkUpdating = false,
            onClose = {},
            onSelectLevel = {},
            onLoadInitial = {},
            onLoadNext = {},
            onBookmarkClick = {},
            onNavigate = {},
            onEditReviewClick = {},
            onDeleteReviewClick = {},
            onReportReviewClick = {},
            onBlockMemberClick = {},
        )
    }
}

@Preview(name = "전체보기 - 후기 있음", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun LevelReviewsOverlayPreview() = PreviewOverlay(
    recommendCount = 15,
    difficultyCounts = mapOf(
        ReviewDifficulty.VERY_EASY to 30L,
        ReviewDifficulty.EASY to 26L,
        ReviewDifficulty.NORMAL to 5L,
        ReviewDifficulty.HARD to 5L,
        ReviewDifficulty.VERY_HARD to 5L,
    ),
    reviews = listOf(previewOverlayReview, previewOverlayReview.copy(reviewId = 2L, nickname = "달리는 토끼")),
)

@Preview(name = "전체보기 - 목록 없음(차단 등)", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun LevelReviewsOverlayNoListPreview() = PreviewOverlay(
    recommendCount = 15,
    difficultyCounts = mapOf(ReviewDifficulty.NORMAL to 4L),
    reviews = emptyList(),
)

@Preview(name = "전체보기 - 추천 수만", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun LevelReviewsOverlayRecommendOnlyPreview() = PreviewOverlay(
    recommendCount = 15,
    difficultyCounts = emptyMap(),
    reviews = emptyList(),
)

private val previewOverlayReview = Review(
    reviewId = 1L,
    memberId = 2L,
    nickname = "초보초보",
    memberLevel = OnboardingLevel.ROOKIE,
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
