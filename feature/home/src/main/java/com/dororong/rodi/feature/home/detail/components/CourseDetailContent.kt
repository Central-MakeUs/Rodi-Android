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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.place.PracticeTagRow
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
    onSummaryHeightChanged: (Int) -> Unit = {},
) {
    val course = place.course ?: return
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(Modifier.onSizeChanged { onSummaryHeightChanged(it.height) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                if (showCloseButton) {
                    RodiIconButton(
                        painter = painterResource(R.drawable.ic_x),
                        onClick = onDismiss,
                        iconSize = 20.dp,
                        contentDescription = "닫기",
                        tint = RodiTheme.colors.black,
                    )
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                val cautionText = course.cautions.filter(String::isNotBlank).joinToString(" ･ ")
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
                if (course.description.isNotBlank()) {
                    Text(
                        text = course.description,
                        style = RodiTheme.typography.caption1Regular,
                        color = RodiTheme.colors.gray700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RodiTheme.colors.gray100, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        RouteInfoSection(course.waypoints)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(
            thickness = 2.dp,
            color = RodiTheme.colors.gray100,
        )
        Spacer(Modifier.height(16.dp))
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
