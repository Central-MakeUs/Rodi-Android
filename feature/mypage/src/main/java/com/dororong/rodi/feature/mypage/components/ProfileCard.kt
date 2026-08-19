package com.dororong.rodi.feature.mypage.components

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.microsoft.clarity.modifiers.clarityMask
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.profileImageRes
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.feature.mypage.MyPageProfile
import com.dororong.rodi.feature.mypage.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object ProfileCardLayout {
    val height = 227.dp
    val horizontalPadding = 11.dp
    val topPadding = 15.dp
    val profileRowHeight = 90.dp
    val practiceTypeSlotHeight = 21.dp
    val drivingGoalSlotHeight = 19.dp
    val stampSize = 170.dp
    val stampTransparentRightInset = stampSize * 0.19f
    val stampVisibleRightCrop = stampSize * 0.1f
    val stampRightCrop = stampTransparentRightInset + stampVisibleRightCrop
    val stampTopOffset = (-16).dp
}

private const val CardEntranceDurationMillis = 340
private const val CardFlickDurationMillis = 900
private const val StampDelayMillis = 200
private const val StampImpactDurationMillis = 700
private val CubicEaseOut = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val QuadraticEaseOut = Easing { fraction -> 1f - (1f - fraction) * (1f - fraction) }

@Composable
internal fun ProfileCard(profile: MyPageProfile, onGoalClick: () -> Unit) {
    val colors = RodiTheme.colors
    val practiceTitle = if (profile.level == OnboardingLevel.NAVIGATOR) "추천 활동" else "추천 연습 유형"
    val drivingGoal = profile.drivingGoal.ifBlank { "나만의 운전 목표를 입력해보세요 !" }
    val drivingGoalColor = if (profile.drivingGoal.isBlank()) RodiTheme.colors.gray500 else RodiTheme.colors.black
    val density = LocalDensity.current
    val isInPreview = LocalInspectionMode.current
    val initialCardTranslationY = with(density) { if (isInPreview) 0f else 16.dp.toPx() }
    val cardAlpha = remember(isInPreview) { Animatable(if (isInPreview) 1f else 0f) }
    val cardTranslationY = remember(initialCardTranslationY) { Animatable(initialCardTranslationY) }
    val cardScale = remember { Animatable(1f) }
    val stampAlpha = remember(isInPreview) { Animatable(if (isInPreview) 0.2f else 0f) }
    val stampScale = remember(isInPreview) { Animatable(if (isInPreview) 1f else 1.8f) }
    val stampRotation = remember(isInPreview) { Animatable(if (isInPreview) 0f else -18f) }

    LaunchedEffect(isInPreview) {
        if (isInPreview) return@LaunchedEffect
        launch {
            cardAlpha.animateTo(1f, tween(CardEntranceDurationMillis, easing = CubicEaseOut))
        }
        launch {
            cardTranslationY.animateTo(0f, tween(CardEntranceDurationMillis, easing = CubicEaseOut))
        }
        launch {
            cardScale.animateTo(1f, tween(CardFlickDurationMillis * 60 / 100))
            cardScale.animateTo(1.012f, tween(CardFlickDurationMillis * 8 / 100, easing = QuadraticEaseOut))
            cardScale.animateTo(1f, tween(CardFlickDurationMillis * 32 / 100, easing = QuadraticEaseOut))
        }
        launch {
            delay(StampDelayMillis.toLong())
            stampAlpha.animateTo(0.28f, tween(StampImpactDurationMillis * 55 / 100, easing = CubicEaseOut))
            stampAlpha.animateTo(0.16f, tween(StampImpactDurationMillis * 20 / 100))
            stampAlpha.animateTo(0.2f, tween(StampImpactDurationMillis * 25 / 100))
        }
        launch {
            delay(StampDelayMillis.toLong())
            stampScale.animateTo(0.97f, tween(StampImpactDurationMillis * 55 / 100, easing = CubicEaseOut))
            stampScale.animateTo(1.02f, tween(StampImpactDurationMillis * 20 / 100))
            stampScale.animateTo(1f, tween(StampImpactDurationMillis * 25 / 100))
        }
        launch {
            delay(StampDelayMillis.toLong())
            stampRotation.animateTo(3f, tween(StampImpactDurationMillis * 55 / 100, easing = CubicEaseOut))
            stampRotation.animateTo(-2f, tween(StampImpactDurationMillis * 20 / 100))
            stampRotation.animateTo(0f, tween(StampImpactDurationMillis * 25 / 100))
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(ProfileCardLayout.height)
            .drawWithCache {
                val center = Offset.Zero
                val horizontalRadius = size.width * 5f
                val verticalRadius = size.height * 0.5f
                val shader = RadialGradient(
                    center.x,
                    center.y,
                    horizontalRadius,
                    intArrayOf(colors.primary20.copy(alpha = 0.5f).toArgb(), colors.white.toArgb()),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                ).apply {
                    setLocalMatrix(
                        Matrix().apply {
                            setScale(
                                1f,
                                verticalRadius / horizontalRadius,
                                center.x,
                                center.y,
                            )
                        },
                    )
                }
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
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
            .border(BorderStroke(1.dp, RodiTheme.colors.primary50), RoundedCornerShape(8.dp))
            .graphicsLayer {
                alpha = cardAlpha.value
                translationY = cardTranslationY.value
                scaleX = cardScale.value
                scaleY = cardScale.value
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        ) {
            Image(
                painter = painterResource(R.drawable.img_rodi_stamp),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = ProfileCardLayout.stampRightCrop,
                        y = ProfileCardLayout.stampTopOffset,
                    )
                    .size(ProfileCardLayout.stampSize)
                    .graphicsLayer {
                        alpha = stampAlpha.value
                        scaleX = stampScale.value
                        scaleY = stampScale.value
                        rotationZ = stampRotation.value
                    },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = ProfileCardLayout.horizontalPadding,
                    top = ProfileCardLayout.topPadding,
                    end = ProfileCardLayout.horizontalPadding,
                ),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ProfileCardLayout.profileRowHeight),
                ) {
                    Image(
                        painter = painterResource(profile.level.profileImageRes),
                        contentDescription = null,
                        modifier = Modifier.size(90.dp),
                        contentScale = ContentScale.FillBounds,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 15.dp, top = 12.dp)
                    ) {
                        Text(
                            text = profile.nickname,
                            modifier = Modifier.clarityMask(),
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = profile.level.displayName,
                                    style = RodiTheme.typography.body3Medium,
                                    color = RodiTheme.colors.black,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = profile.distanceLabel,
                                    style = RodiTheme.typography.caption2Medium,
                                    color = RodiTheme.colors.gray600,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(RodiTheme.colors.gray200, RoundedCornerShape(100)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(profile.progress.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .background(RodiTheme.colors.primary600, RoundedCornerShape(100)),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = practiceTitle,
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray700,
                )
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.height(ProfileCardLayout.practiceTypeSlotHeight)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        profile.practiceTypes.forEach { practiceType ->
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
                }
            }
            Spacer(Modifier.height(12.dp))
            Column {
                Text(
                    text = "운전 목표",
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray700,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ProfileCardLayout.drivingGoalSlotHeight)
                        .clickable(onClick = onGoalClick),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = drivingGoal,
                        style = RodiTheme.typography.body3Medium,
                        color = drivingGoalColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
}

private val OnboardingLevel.displayName: String
    get() = name.lowercase().replaceFirstChar { it.titlecase() }

@Preview(name = "프로필 진행률 0%", showBackground = true, widthDp = 375, heightDp = 248)
@Composable
private fun ProfileCardProgressZeroPreview() {
    RodiTheme {
        ProfileCard(
            profile = MyPageProfile(
                nickname = "아주 긴 닉네임도 카드 안에서 한 줄로 표시돼야 해요",
                level = OnboardingLevel.SEED,
            ),
            onGoalClick = {},
        )
    }
}

@Preview(name = "프로필 진행률 40%", showBackground = true, widthDp = 375, heightDp = 248)
@Composable
private fun ProfileCardProgressFortyPreview() {
    RodiTheme {
        ProfileCard(
            profile = MyPageProfile(
                nickname = "흐름타는 고슴도치",
                level = OnboardingLevel.ROOKIE,
                practiceTypes = listOf("유턴", "차선변경", "주차", "교차로"),
                drivingGoal = "복잡한 강남 자신있게 운전하기",
                progress = 0.4f,
            ),
            onGoalClick = {},
        )
    }
}

@Preview(name = "프로필 진행률 100%", showBackground = true, widthDp = 375, heightDp = 248)
@Composable
private fun ProfileCardProgressFullPreview() {
    RodiTheme {
        ProfileCard(
            profile = MyPageProfile(
                nickname = "운전에 익숙한 로디",
                level = OnboardingLevel.OWNER,
                practiceTypes = listOf("고속도로", "합류", "다차로주행"),
                drivingGoal = "고속도로를 편안하게 주행하기",
                progress = 1f,
            ),
            onGoalClick = {},
        )
    }
}

@Preview(name = "프로필 긴 닉네임", showBackground = true, widthDp = 375, heightDp = 248)
@Composable
private fun ProfileCardLongNicknamePreview() {
    RodiTheme {
        ProfileCard(
            profile = MyPageProfile(
                nickname = "새 도전을 즐기는 로디",
                level = OnboardingLevel.EXPLORER,
                practiceTypes = listOf("비보호좌회전", "회전교차로", "코너링"),
                drivingGoal = "낯선 길도 자신 있게 주행하기",
            ),
            onGoalClick = {},
        )
    }
}

@Preview(name = "프로필 긴 운전 목표", showBackground = true, widthDp = 375, heightDp = 248)
@Composable
private fun ProfileCardLongGoalPreview() {
    RodiTheme {
        ProfileCard(
            profile = MyPageProfile(
                nickname = "길잡이 로디",
                level = OnboardingLevel.NAVIGATOR,
                practiceTypes = listOf("코스등록", "리뷰 작성", "추천 코스"),
                drivingGoal = "다른 운전자에게 도움이 되는 코스 남기기",
            ),
            onGoalClick = {},
        )
    }
}

@Preview(name = "프로필 목표 없음", showBackground = true, widthDp = 375, heightDp = 248)
@Composable
private fun ProfileCardNoGoalPreview() {
    RodiTheme {
        ProfileCard(
            profile = MyPageProfile(nickname = "운전 초보", level = OnboardingLevel.SEED),
            onGoalClick = {},
        )
    }
}
