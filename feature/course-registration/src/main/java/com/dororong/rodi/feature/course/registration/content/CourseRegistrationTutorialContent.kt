package com.dororong.rodi.feature.course.registration.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.R
import kotlin.math.abs
import kotlin.math.roundToInt

private data class TutorialPage(
    val title: String,
    val subtitle: String,
    val tooltip: String,
    val image: Int,
    val tooltipAlignment: Alignment,
    val dimTop: Dp,
    val dimBottom: Dp,
    val tooltipOffsetX: Dp = 0.dp,
    val tooltipOffsetY: Dp = 0.dp,
)

private val tutorialPages = listOf(
    TutorialPage(
        title = "지도를 움직여 핀을 놓을 위치를 정하고",
        subtitle = "출발지 → 도착지 → 경유지 순서로 코스를 구성해요.",
        tooltip = "지도를 움직여 핀을 도로위에 맞춰요",
        image = R.drawable.illust_course_registration_tutorial_route,
        tooltipAlignment = Alignment.Center,
        dimTop = 231.dp,
        dimBottom = 193.dp,
    ),
    TutorialPage(
        title = "아래 ‘출발지 선택’을 눌러, 위치를 선택해요",
        subtitle = "건물이 아닌, 도로 위에 위치 시켜주세요.",
        tooltip = "버튼을 눌러 위치를 선택해요",
        image = R.drawable.illust_course_registration_tutorial_route,
        tooltipAlignment = Alignment.BottomCenter,
        dimTop = 484.dp,
        dimBottom = 0.dp,
        tooltipOffsetX = (-61).dp,
        tooltipOffsetY = (-43).dp,
    ),
    TutorialPage(
        title = "위치 수정 시 해당 핀을 눌러주세요",
        subtitle = "‘핀 수정하기’ 화면으로 이동할 수 있어요.",
        tooltip = "선택한 핀을 누르면 위치를 수정할 수 있어요.",
        image = R.drawable.illust_course_registration_tutorial_pin,
        tooltipAlignment = Alignment.Center,
        dimTop = 231.dp,
        dimBottom = 193.dp,
    ),
)

private const val TUTORIAL_PROGRESS_ANIMATION_DURATION_PER_PAGE_MILLIS = 220
private val TUTORIAL_BOX_HORIZONTAL_PADDING = 36.5.dp
private val TUTORIAL_BOX_TOP_PADDING = 28.dp

@Composable
fun CourseRegistrationTutorialContent(
    page: Int,
    isCompleting: Boolean,
    onPageChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onRetry: () -> Unit = {},
) {
    val pagerState = rememberPagerState(
        initialPage = page.coerceIn(0, tutorialPages.lastIndex),
        pageCount = { tutorialPages.size },
    )
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }
    LaunchedEffect(page) {
        val target = page.coerceIn(0, tutorialPages.lastIndex)
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TutorialTopBar(onBack = onBack)
        if (isError) {
            TutorialError(onRetry = onRetry)
        } else {
            TutorialProgress(currentPage = pagerState.currentPage)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) { index ->
                TutorialPageContent(page = tutorialPages[index])
            }
            if (pagerState.currentPage == tutorialPages.lastIndex) {
                RodiButton(
                    text = "완료",
                    onClick = onComplete,
                    enabled = !isCompleting,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun TutorialTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .clickable(onClick = onBack)
                .semantics {
                    contentDescription = "이전"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(CoreUiR.drawable.ic_chevron_left),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = "코스 등록 방법",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
        )
    }
}

@Composable
private fun TutorialProgress(currentPage: Int) {
    val progress = remember { Animatable(0f) }
    val targetProgress = (currentPage + 1).coerceIn(0, tutorialPages.size).toFloat()
    LaunchedEffect(targetProgress) {
        progress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(
                durationMillis = (abs(targetProgress - progress.value) * TUTORIAL_PROGRESS_ANIMATION_DURATION_PER_PAGE_MILLIS)
                    .roundToInt()
                    .coerceAtLeast(1),
            ),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tutorialPages.indices.forEach { index ->
            val fillFraction = (progress.value - index).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(RodiTheme.colors.gray300),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillFraction)
                        .background(RodiTheme.colors.primary600),
                )
            }
        }
    }
}

@Composable
private fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = page.title,
                style = RodiTheme.typography.heading2,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = page.subtitle,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.secondary300,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    top = TUTORIAL_BOX_TOP_PADDING,
                    start = TUTORIAL_BOX_HORIZONTAL_PADDING,
                    end = TUTORIAL_BOX_HORIZONTAL_PADDING,
                )
                .clip(RoundedCornerShape(10.dp))
                .border(3.dp, RodiTheme.colors.gray300, RoundedCornerShape(10.dp)),
        ) {
            Image(
                painter = painterResource(page.image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            TutorialHighlightOverlay(page = page)
            TutorialTooltip(
                text = page.tooltip,
                modifier = Modifier
                    .align(page.tooltipAlignment)
                    .offset(x = page.tooltipOffsetX, y = page.tooltipOffsetY),
            )
        }
    }
}

@Composable
private fun TutorialHighlightOverlay(page: TutorialPage) {
    val dimColor = RodiTheme.colors.black.copy(alpha = 0.5f)
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(page.dimTop)
                .align(Alignment.TopCenter)
                .background(dimColor),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(page.dimBottom)
                .align(Alignment.BottomCenter)
                .background(dimColor),
        )
    }
}

@Composable
private fun TutorialTooltip(text: String, modifier: Modifier = Modifier) {
    val tooltipColor = RodiTheme.colors.primary600
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(tooltipColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Text(
                text = text,
                style = RodiTheme.typography.caption2SemiBold,
                color = RodiTheme.colors.white,
            )
        }
        Canvas(Modifier.width(14.dp).height(8.dp)) {
            val pointer = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(pointer, color = tooltipColor)
        }
    }
}

@Composable
private fun TutorialError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "튜토리얼을 불러오지 못했어요",
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
        )
        Spacer(Modifier.height(12.dp))
        RodiButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = RodiButtonVariant.Secondary,
            fillMaxWidth = false,
            modifier = Modifier.width(120.dp),
        )
    }
}

@Preview(name = "Tutorial P1", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationTutorialP1Preview() {
    RodiTheme {
        CourseRegistrationTutorialContent(0, false, {}, {}, {})
    }
}

@Preview(name = "Tutorial P2", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationTutorialP2Preview() {
    RodiTheme {
        CourseRegistrationTutorialContent(1, false, {}, {}, {})
    }
}

@Preview(name = "Tutorial P3", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationTutorialP3Preview() {
    RodiTheme {
        CourseRegistrationTutorialContent(2, false, {}, {}, {})
    }
}

@Preview(name = "Tutorial Error", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationTutorialErrorPreview() {
    RodiTheme {
        CourseRegistrationTutorialContent(0, false, {}, {}, {}, isError = true)
    }
}
