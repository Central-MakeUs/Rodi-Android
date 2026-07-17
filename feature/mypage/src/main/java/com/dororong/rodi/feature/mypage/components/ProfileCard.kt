package com.dororong.rodi.feature.mypage.components

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.MyPageProfile
import com.dororong.rodi.feature.mypage.R

private object ProfileCardLayout {
    val height = 227.dp
    val horizontalPadding = 11.dp
    val topPadding = 15.dp
    val profileRowHeight = 90.dp
    val practiceTypeSlotHeight = 21.dp
    val drivingGoalSlotHeight = 19.dp
    val stampSize = 200.dp
    val stampTransparentRightInset = stampSize * 0.17f
    val stampVisibleRightCrop = stampSize * 0.1f
    val stampRightCrop = stampTransparentRightInset + stampVisibleRightCrop
    val stampTopOffset = (-16).dp
}

@Composable
internal fun ProfileCard(profile: MyPageProfile, onGoalClick: () -> Unit) {
    val colors = RodiTheme.colors
    val profileImageWidth = 95.dp
    val profileImageHeight = 120.dp
    val profileImageOffsetX = (-5).dp
    val profileImageOffsetY = 2.dp

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
            .border(BorderStroke(1.dp, RodiTheme.colors.primary50), RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        ) {
            Image(
                painter = painterResource(R.drawable.img_rodi_stamp),
                contentDescription = null,
                alpha = 0.2f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = ProfileCardLayout.stampRightCrop,
                        y = ProfileCardLayout.stampTopOffset,
                    )
                    .size(ProfileCardLayout.stampSize),
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
                Row(modifier = Modifier.height(ProfileCardLayout.profileRowHeight)) {
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
                    Column(modifier = Modifier.padding(start = 15.dp, top = 12.dp)) {
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
                Box(modifier = Modifier.height(ProfileCardLayout.practiceTypeSlotHeight)) {
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
}

@Preview(showBackground = true, widthDp = 375, heightDp = 248)
@Composable
private fun ProfileCardFilledPreview() {
    RodiTheme {
        ProfileCard(
            profile = MyPageProfile(
                nickname = "흐름타는 고슴도치",
                level = "Rookie",
                practiceTypes = listOf("차선변경", "교차로", "주차"),
                drivingGoal = "복잡한 강남 자신있게 운전하기",
            ),
            onGoalClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 248)
@Composable
private fun ProfileCardIncompletePreview() {
    RodiTheme {
        ProfileCard(
            profile = MyPageProfile(
                nickname = "아주 긴 닉네임도 카드 안에서 한 줄로 표시돼야 해요",
                level = "Seed",
            ),
            onGoalClick = {},
        )
    }
}
