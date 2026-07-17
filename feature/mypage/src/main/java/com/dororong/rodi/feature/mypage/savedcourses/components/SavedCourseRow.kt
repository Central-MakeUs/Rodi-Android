package com.dororong.rodi.feature.mypage.savedcourses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CourseFeatures
import com.dororong.rodi.core.domain.model.course.Waypoint
import com.dororong.rodi.core.domain.model.course.WaypointType
import com.dororong.rodi.core.ui.theme.RodiTheme
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun SavedCourseRow(course: Course) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = course.title,
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = course.formattedDrivingDistance(),
                style = RodiTheme.typography.caption1SemiBold,
                color = RodiTheme.colors.primary600,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "주행거리",
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            course.tags.take(2).forEach { tag ->
                Text(
                    text = tag.label,
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.black,
                    modifier = Modifier
                        .background(RodiTheme.colors.gray200, RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = course.summary,
            style = RodiTheme.typography.caption1Medium,
            color = RodiTheme.colors.gray600,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(RodiTheme.colors.gray50, RoundedCornerShape(8.dp))
                .padding(10.dp),
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = RodiTheme.colors.primary100)
        Spacer(Modifier.height(16.dp))
    }
}

private fun Course.formattedDrivingDistance(): String {
    val distanceKm = allPoints.zipWithNext().sumOf { (from, to) ->
        val latitudeDelta = Math.toRadians(to.lat - from.lat)
        val longitudeDelta = Math.toRadians(to.lng - from.lng)
        val startLatitude = Math.toRadians(from.lat)
        val endLatitude = Math.toRadians(to.lat)
        val haversine = sin(latitudeDelta / 2).let { it * it } +
            cos(startLatitude) * cos(endLatitude) * sin(longitudeDelta / 2).let { it * it }
        6_371.0 * 2 * asin(sqrt(haversine))
    }
    val rounded = (distanceKm * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()}km" else "${rounded}km"
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun SavedCourseRowTaggedPreview() {
    RodiTheme {
        SavedCourseRow(
            course = savedCourseRowPreview(
                courseName = "한강 야경 드라이브",
                features = CourseFeatures(intersection = true, highway = true),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun SavedCourseRowLongTextPreview() {
    RodiTheme {
        SavedCourseRow(
            course = savedCourseRowPreview(
                courseName = "이름이 긴 저장 코스도 한 줄 안에서 자연스럽게 잘려야 합니다",
                features = CourseFeatures(),
                summary = "태그가 없는 코스의 긴 요약 문구도 두 줄까지만 표시되는지 확인합니다. 세 번째 줄부터는 보이지 않아야 합니다.",
            ),
        )
    }
}

private fun savedCourseRowPreview(
    courseName: String,
    features: CourseFeatures,
    summary: String = "교차로와 차선 변경을 연습하기 좋은 추천 코스입니다.",
): Course = Course(
    id = 1,
    courseName = courseName,
    courseNickname = courseName,
    areaName = "마포구",
    region = "seoul",
    difficulty = 2,
    trafficDensity = null,
    source = "preview",
    sourceUrl = "",
    crawledAt = "",
    waypoints = listOf(
        Waypoint(
            order = 0,
            type = WaypointType.START,
            name = "망원한강공원",
            lat = 37.551,
            lng = 126.902,
            address = "서울특별시 마포구 망원동",
            category = "도로",
        ),
        Waypoint(
            order = 1,
            type = WaypointType.END,
            name = "반포한강공원",
            lat = 37.511,
            lng = 126.995,
            address = "서울특별시 서초구 반포동",
            category = "도로",
        ),
    ),
    features = features,
    recommendation = 1,
    caution = "",
    bestTime = "",
    enrichedDescription = summary,
)
