package com.dororong.rodi.feature.course.registration.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.R

private data class TutorialPage(
    val title: String,
    val subtitle: String,
    val tooltip: String,
    val image: Int,
    val tooltipAlignment: Alignment,
    val dimTopFraction: Float,
    val dimBottomFraction: Float,
    val tooltipOffsetXFraction: Float = 0f,
    val tooltipOffsetYFraction: Float = 0f,
)

// Figma 목업 기준(3800:68104/68117/68129)은 270x533 박스다. 실기기 박스 높이가
// 화면 폭에 따라 달라지므로, 그 기준 박스 대비 비율로 저장해 어떤 폭에서도
// 이미지 크롭·Dim 경계·툴팁 위치가 같은 지점을 가리키게 한다.
private const val TUTORIAL_MOCKUP_REFERENCE_HEIGHT = 533f

private val tutorialPages = listOf(
    TutorialPage(
        title = "지도를 움직여 핀을 놓을 위치를 정하고",
        subtitle = "출발지 → 도착지 → 경유지 순서로 코스를 구성해요.",
        tooltip = "지도를 움직여 핀을 도로위에 맞춰요",
        image = R.drawable.illust_course_registration_tutorial_route,
        tooltipAlignment = Alignment.Center,
        dimTopFraction = 231.043f / TUTORIAL_MOCKUP_REFERENCE_HEIGHT,
        dimBottomFraction = 193f / TUTORIAL_MOCKUP_REFERENCE_HEIGHT,
    ),
    TutorialPage(
        title = "아래 ‘출발지 선택’을 눌러, 위치를 선택해요",
        subtitle = "건물이 아닌, 도로 위에 위치 시켜주세요.",
        tooltip = "버튼을 눌러 위치를 선택해요",
        image = R.drawable.illust_course_registration_tutorial_route,
        tooltipAlignment = Alignment.BottomCenter,
        dimTopFraction = 483.777f / TUTORIAL_MOCKUP_REFERENCE_HEIGHT,
        dimBottomFraction = 0f,
        tooltipOffsetXFraction = -61f / 270f,
        tooltipOffsetYFraction = -42.52f / TUTORIAL_MOCKUP_REFERENCE_HEIGHT,
    ),
    TutorialPage(
        title = "위치 수정 시 해당 핀을 눌러주세요",
        subtitle = "‘핀 수정하기’ 화면으로 이동할 수 있어요.",
        tooltip = "선택한 핀을 누르면 위치를 수정할 수 있어요",
        image = R.drawable.illust_course_registration_tutorial_pin,
        tooltipAlignment = Alignment.Center,
        dimTopFraction = 231.043f / TUTORIAL_MOCKUP_REFERENCE_HEIGHT,
        dimBottomFraction = 193f / TUTORIAL_MOCKUP_REFERENCE_HEIGHT,
    ),
)

// 목업 박스는 375dp 화면에서 270dp다(디자인 4165:11775). 좌우 여백은 그 나머지 절반.
private val TUTORIAL_BOX_HORIZONTAL_PADDING = 52.5.dp
private val TUTORIAL_BOX_TOP_PADDING = 28.dp
private const val TUTORIAL_BOX_ASPECT_RATIO = 270f / TUTORIAL_MOCKUP_REFERENCE_HEIGHT

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
        TutorialTopBar(
            onBack = onBack,
            onComplete = onComplete.takeIf {
                !isError && pagerState.currentPage == tutorialPages.lastIndex && !isCompleting
            },
        )
        if (isError) {
            TutorialError(onRetry = onRetry)
        } else {
            TutorialProgress(pagerState = pagerState)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) { index ->
                TutorialPageContent(page = tutorialPages[index])
            }
        }
    }
}

@Composable
private fun TutorialTopBar(onBack: () -> Unit, onComplete: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        RodiIconButton(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            onClick = onBack,
            contentDescription = "이전",
            tint = RodiTheme.colors.black,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = "코스 등록 방법",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
        )
        // 마지막 장의 완료는 하단 버튼이 아니라 앱바 오른쪽 체크다(디자인 4165:11817).
        onComplete?.let { complete ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 12.dp)
                    .requiredSize(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = complete)
                    .semantics {
                        contentDescription = "튜토리얼 완료"
                        role = Role.Button
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(RodiTheme.colors.primary600, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_registration_check_16),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialProgress(pagerState: PagerState) {
    // currentPage + currentPageOffsetFraction는 페이지 사이 드래그 중에도 프레임마다
    // 연속으로 갱신되므로, 별도 애니메이션 없이 그대로 스와이프 진행률과 일치한다.
    val progress = pagerState.currentPage + pagerState.currentPageOffsetFraction + 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tutorialPages.indices.forEach { index ->
            val fillFraction = (progress - index).coerceIn(0f, 1f)
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = page.subtitle,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.secondary300,
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = TUTORIAL_BOX_TOP_PADDING,
                    start = TUTORIAL_BOX_HORIZONTAL_PADDING,
                    end = TUTORIAL_BOX_HORIZONTAL_PADDING,
                )
                .aspectRatio(TUTORIAL_BOX_ASPECT_RATIO),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                Image(
                    painter = painterResource(page.image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                TutorialHighlightOverlay(page = page)
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(3.dp, RodiTheme.colors.gray300, RoundedCornerShape(10.dp)),
            )
            TutorialTooltip(
                text = page.tooltip,
                modifier = Modifier
                    .align(page.tooltipAlignment)
                    .offset(
                        x = maxWidth * page.tooltipOffsetXFraction,
                        y = maxHeight * page.tooltipOffsetYFraction,
                    ),
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
                .fillMaxHeight(page.dimTopFraction)
                .align(Alignment.TopCenter)
                .background(dimColor),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(page.dimBottomFraction)
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
