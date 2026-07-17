package com.dororong.rodi.feature.mypage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiTheme

data class MyPageProfile(
    val nickname: String = "",
    val level: String = "Seed",
    val practiceTypes: List<String> = emptyList(),
    val drivingGoal: String = "",
    val savedCourseCount: Int = 0,
)

@Composable
fun MyPageScreen(
    onSettingsClick: () -> Unit,
    onGoalClick: () -> Unit,
    onSavedCoursesClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyPageContent(
        profile = uiState.profile,
        onSettingsClick = onSettingsClick,
        onGoalClick = onGoalClick,
        onSavedCoursesClick = onSavedCoursesClick,
        modifier = modifier,
    )
}

@Composable
private fun MyPageContent(
    profile: MyPageProfile,
    onSettingsClick: () -> Unit,
    onGoalClick: () -> Unit,
    onSavedCoursesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        MyPageTopBar(onSettingsClick = onSettingsClick)
        ProfileCard(profile = profile, onGoalClick = onGoalClick)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = RodiTheme.colors.gray100)
        SavedCoursesRow(
            count = profile.savedCourseCount,
            onClick = onSavedCoursesClick,
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun MyPageTopBar(onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "프로필",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
        )
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_settings),
            contentDescription = "설정",
            tint = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(24.dp)
                .clickable(onClick = onSettingsClick),
        )
    }
}

@Composable
private fun ProfileCard(profile: MyPageProfile, onGoalClick: () -> Unit) {
    val colors = RodiTheme.colors
    val profileImageWidth = 95.dp
    val profileImageHeight = 120.dp
    val profileImageOffsetX = (-2.5).dp
    val profileImageOffsetY = 2.dp

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(227.dp)
            .drawWithCache {
                val gradientCenterXRatio = 0f
                val gradientCenterYRatio = 0f
                val gradientHorizontalRadiusRatio = 5f
                val gradientVerticalRadiusRatio = 0.5f
                val center = Offset(
                    x = size.width * gradientCenterXRatio,
                    y = size.height * gradientCenterYRatio,
                )
                val horizontalRadius = size.width * gradientHorizontalRadiusRatio
                val verticalRadius = size.height * gradientVerticalRadiusRatio
                val shader = android.graphics.RadialGradient(
                    center.x,
                    center.y,
                    horizontalRadius,
                    intArrayOf(colors.primary20.copy(alpha = 0.5f).toArgb(), colors.white.toArgb()),
                    floatArrayOf(0f, 1f),
                    android.graphics.Shader.TileMode.CLAMP,
                ).apply {
                    setLocalMatrix(
                        android.graphics.Matrix().apply {
                            setScale(
                                1f,
                                verticalRadius / horizontalRadius,
                                center.x,
                                center.y,
                            )
                        },
                    )
                }
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                }
                val cornerRadius = 8.dp.toPx()
                onDrawBehind {
                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadius,
                        cornerRadius,
                        paint,
                    )
                }
            }
            .border(BorderStroke(1.dp, RodiTheme.colors.primary50), RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(x = 550, y = -40) }
                    .size(200.dp)
                    .clipToBounds(),
            ) {
                Image(
                    painter = painterResource(R.drawable.img_rodi_stamp),
                    contentDescription = null,
                    alpha = 0.2f,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 11.dp, top = 15.dp, end = 11.dp),
        ) {
            Row(
                modifier = Modifier.height(90.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary100),
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_rodi_profile),
                        contentDescription = null,
                        modifier = Modifier
                            .size(90.dp)
                            .graphicsLayer {
                                scaleX = profileImageWidth.value / 90f
                                scaleY = profileImageHeight.value / 90f
                                transformOrigin = TransformOrigin(0f, 0f)
                                translationX = profileImageOffsetX.toPx()
                                translationY = profileImageOffsetY.toPx()
                            },
                        contentScale = ContentScale.FillBounds,
                    )
                }
                Column(
                    modifier = Modifier.padding(start = 15.dp, top = 12.dp),
                ) {
                    Text(
                        text = profile.nickname,
                        style = RodiTheme.typography.body1SemiBold,
                        color = RodiTheme.colors.black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "레벨",
                            style = RodiTheme.typography.caption1Medium,
                            color = RodiTheme.colors.gray700,
                        )
                        Text(
                            text = profile.level,
                            style = RodiTheme.typography.body3Medium,
                            color = RodiTheme.colors.black,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "추천 연습 유형",
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray700,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                profile.practiceTypes.take(3).forEach { practiceType ->
                    Text(
                        text = practiceType,
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.black,
                        modifier = Modifier
                            .background(RodiTheme.colors.primary50, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "운전 목표",
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray700,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onGoalClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = profile.drivingGoal,
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.black,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                    contentDescription = "운전 목표 수정",
                    tint = RodiTheme.colors.gray600,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun SavedCoursesRow(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "저장한 코스 ($count)",
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.black,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_right),
            contentDescription = "저장한 코스 보기",
            tint = RodiTheme.colors.gray600,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPageContentPreview() {
    RodiTheme {
        MyPageContent(
            profile = MyPageProfile(
                nickname = "흐름타는 고슴도치",
                level = "Rookie",
                practiceTypes = listOf("차선변경", "교차로", "주차"),
                drivingGoal = "복잡한 강남 자신있게 운전하기",
                savedCourseCount = 5,
            ),
            onSettingsClick = {},
            onGoalClick = {},
            onSavedCoursesClick = {},
        )
    }
}
