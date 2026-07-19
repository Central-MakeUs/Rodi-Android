package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.feature.home.list.components.PracticeTagRow
import java.util.Locale

@Composable
fun CourseDetailContent(
    place: PlaceDetail,
    isBookmarkUpdating: Boolean,
    onDismiss: () -> Unit,
    onBookmarkClick: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val course = place.course ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = place.name,
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = place.bookmarkCount.toString(),
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray700,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = "닫기",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = course.distanceMeters.toDistanceText(),
                    style = RodiTheme.typography.headline2,
                    color = RodiTheme.colors.primary600,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "주행거리",
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray600,
                )
            }
            PracticeTagRow(place.practiceTypes)
            course.cautions.forEach { caution ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_alert_circle),
                        contentDescription = null,
                        tint = RodiTheme.colors.primary600,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = caution,
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.primary600,
                    )
                }
            }
            Text(
                text = course.description,
                style = RodiTheme.typography.caption1Regular,
                color = RodiTheme.colors.gray700,
            )
            course.waypoints.sortedBy { it.sequence }.forEach { waypoint ->
                Text(
                    text = "${waypoint.type.label()}  ${waypoint.name.orEmpty()}",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookmarkButton(
                isBookmarked = place.isBookmarked,
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

private fun Int.toDistanceText(): String = if (this >= 1_000) {
    if (this % 1_000 == 0) "${this / 1_000}km" else String.format(Locale.KOREA, "%.1fkm", this / 1_000.0)
} else "$this m"

private fun PlaceWaypointType.label(): String = when (this) {
    PlaceWaypointType.START -> "출발"
    PlaceWaypointType.VIA -> "경유"
    PlaceWaypointType.DESTINATION -> "도착"
}

@Preview(name = "Course detail - unsaved", showBackground = true, widthDp = 375, heightDp = 460)
@Composable
private fun CourseDetailUnsavedPreview() {
    RodiTheme { CourseDetailContent(HomePreviewData.courseDetail, false, {}, {}, {}) }
}

@Preview(name = "Course detail - saved many via", showBackground = true, widthDp = 375, heightDp = 600)
@Composable
private fun CourseDetailSavedPreview() {
    RodiTheme {
        CourseDetailContent(HomePreviewData.courseDetail.copy(isBookmarked = true), false, {}, {}, {})
    }
}

@Preview(name = "Course detail - no via long text", showBackground = true, widthDp = 320, heightDp = 520, fontScale = 1.3f)
@Composable
private fun CourseDetailNoViaPreview() {
    val detail = HomePreviewData.courseDetail
    val course = requireNotNull(detail.course)
    RodiTheme {
        CourseDetailContent(
            place = detail.copy(
                name = "아주 긴 이름의 초보 운전자 도심 적응 연습 코스",
                course = course.copy(
                    description = course.description.repeat(3),
                    waypoints = course.waypoints.filter { it.type != PlaceWaypointType.VIA },
                ),
            ),
            isBookmarkUpdating = true,
            onDismiss = {},
            onBookmarkClick = {},
            onNavigate = {},
        )
    }
}
