package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.place.PracticeTagRow
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.R
import java.util.Locale

@Composable
fun CourseDetailContent(
    place: PlaceDetail,
    onDismiss: () -> Unit,
    reviewContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    showCloseButton: Boolean = true,
    closeButtonAlpha: () -> Float = { if (showCloseButton) 1f else 0f },
    onSummaryHeightChanged: (Int) -> Unit = {},
) {
    val course = place.course ?: return
    val cautionText = course.cautions.filter(String::isNotBlank).joinToString(" ･ ")
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .onSizeChanged { onSummaryHeightChanged(it.height) }
                // 접힌 높이는 이 블록 높이로 정해진다. 아래 여백이 없으면 설명 칸이
                // 하단 버튼 바에 붙어 보인다.
                .padding(
                    top = 0.dp,
                    bottom = RodiSpacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(RodiSpacing.sm),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RodiSpacing.sm)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RodiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = place.name,
                            style = RodiTheme.typography.headline1,
                            color = RodiTheme.colors.black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_bookmark_detail),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = place.bookmarkCount.toString(),
                            style = RodiTheme.typography.body3Medium,
                            color = RodiTheme.colors.gray700,
                        )
                    }
                    RodiIconButton(
                        painter = painterResource(R.drawable.ic_x),
                        onClick = onDismiss,
                        iconSize = 20.dp,
                        contentDescription = "닫기",
                        tint = RodiTheme.colors.black,
                        enabled = showCloseButton,
                        modifier = Modifier.graphicsLayer { alpha = closeButtonAlpha() },
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = RodiSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(RodiSpacing.sm),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = course.distanceMeters.toDistanceText(),
                            style = RodiTheme.typography.body1SemiBold,
                            color = RodiTheme.colors.primary600,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "주행거리",
                            style = RodiTheme.typography.caption1Medium,
                            color = RodiTheme.colors.gray800,
                        )
                    }
                    PracticeTagRow(place.practiceTypes)
                    if (cautionText.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_alert_triangle),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = cautionText,
                                style = RodiTheme.typography.caption1Medium,
                                color = RodiTheme.colors.secondary400,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (course.description.isNotBlank()) {
                Text(
                    text = course.description,
                    style = RodiTheme.typography.caption1Regular,
                    color = RodiTheme.colors.gray700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RodiSpacing.md)
                        .background(RodiTheme.colors.gray100, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                )
            }
        }

        Spacer(Modifier.height(RodiSpacing.lg))
        RouteInfoSection(course.waypoints)
        Spacer(Modifier.height(RodiSpacing.lg))
        HorizontalDivider(
            thickness = 2.dp,
            color = RodiTheme.colors.gray100,
        )
        Spacer(Modifier.height(RodiSpacing.md))
        reviewContent()
    }
}

private fun Int.toDistanceText(): String = if (this >= 1_000) {
    if (this % 1_000 == 0) "${this / 1_000}km" else String.format(Locale.KOREA, "%.1fkm", this / 1_000.0)
} else "${this}m"

@Preview(name = "코스 상세 내용 - 기본", showBackground = true, widthDp = 375)
@Composable
private fun CourseDetailContentPreview() {
    RodiTheme {
        CourseDetailContent(place = HomePreviewData.courseDetail, onDismiss = {}, reviewContent = {})
    }
}

@Preview(name = "코스 상세 내용 - 저장됨", showBackground = true, widthDp = 375)
@Composable
private fun CourseDetailContentBookmarkedPreview() {
    RodiTheme {
        CourseDetailContent(
            place = HomePreviewData.courseDetail.copy(isBookmarked = true),
            onDismiss = {},
            reviewContent = {},
        )
    }
}

@Preview(name = "코스 상세 내용 - 확장(닫기 숨김)", showBackground = true, widthDp = 375)
@Composable
private fun CourseDetailContentExpandedPreview() {
    RodiTheme {
        CourseDetailContent(
            place = HomePreviewData.courseDetail,
            onDismiss = {},
            reviewContent = {},
            showCloseButton = false,
        )
    }
}
