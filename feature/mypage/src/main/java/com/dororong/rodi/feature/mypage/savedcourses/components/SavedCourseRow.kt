package com.dororong.rodi.feature.mypage.savedcourses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
internal fun SavedCourseRow(place: PlaceSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = place.name,
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (place.type == PlaceType.COURSE) "코스" else "주차장",
                style = RodiTheme.typography.caption1SemiBold,
                color = RodiTheme.colors.primary600,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = place.address,
            style = RodiTheme.typography.caption1Medium,
            color = RodiTheme.colors.gray600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (place.practiceTypes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                place.practiceTypes.take(3).forEach { type ->
                    Text(
                        text = type.label,
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.black,
                        modifier = Modifier
                            .background(RodiTheme.colors.gray200, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        place.description?.takeIf(String::isNotBlank)?.let { description ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = description,
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RodiTheme.colors.gray50, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = RodiTheme.colors.primary100)
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun SavedPlaceRowPreview() {
    RodiTheme {
        SavedCourseRow(
            place = PlaceSummary(
                id = 1,
                type = PlaceType.COURSE,
                name = "한강 야경 드라이브",
                address = "서울특별시 마포구",
                point = GeoPoint(37.55, 126.9),
                distanceFromMeMeters = null,
                practiceTypes = listOf(PracticeType.INTERSECTION, PracticeType.LANE_CHANGE),
                description = "교차로와 차선 변경을 연습하기 좋은 추천 코스입니다.",
                distanceMeters = 4500,
                capacity = null,
                openTime = null,
            ),
            onClick = {},
        )
    }
}
