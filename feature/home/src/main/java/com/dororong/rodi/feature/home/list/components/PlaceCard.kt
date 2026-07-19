package com.dororong.rodi.feature.home.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PlaceCard(
    place: PlaceSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        when (place.type) {
            PlaceType.COURSE -> CoursePlaceCard(place)
            PlaceType.PARKING -> ParkingPlaceCard(place)
        }
    }
}

@Composable
private fun CoursePlaceCard(place: PlaceSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = place.name,
                    style = RodiTheme.typography.body1SemiBold,
                    color = RodiTheme.colors.black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                place.distanceMeters?.let { distanceMeters ->
                    Text(
                        text = distanceMeters.toDistanceText(),
                        style = RodiTheme.typography.body3SemiBold,
                        color = RodiTheme.colors.primary600,
                    )
                    Text(
                        text = "주행거리",
                        style = RodiTheme.typography.body3Medium,
                        color = RodiTheme.colors.gray800,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            PracticeTagRow(place.practiceTypes)
        }
        place.description?.takeIf(String::isNotBlank)?.let { description ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(37.dp)
                    .background(RodiTheme.colors.gray50, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = description,
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ParkingPlaceCard(place: PlaceSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = place.name,
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = place.address.toDistrictAddress(),
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PracticeTagRow(types = place.practiceTypes, maxTags = 1)
            place.openTime?.let { openTime ->
                Text(
                    text = "･",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                )
                Text(
                    text = openTime.toOpeningTime(),
                    style = RodiTheme.typography.body3SemiBold,
                    color = RodiTheme.colors.primary600,
                )
                Text(
                    text = "에 영업 시작",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                )
            }
        }
        place.capacity?.let { capacity ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "총 주차 면수",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                )
                Text(
                    text = "･",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                )
                Text(
                    text = "${NumberFormat.getNumberInstance(Locale.KOREA).format(capacity)}대",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                )
            }
        }
    }
}

private fun Int.toDistanceText(): String = if (this >= 1_000) {
    val km = this / 1_000.0
    if (this % 1_000 == 0) "${this / 1_000}km" else String.format(Locale.KOREA, "%.1fkm", km)
} else {
    "${this}m"
}

private fun String.toOpeningTime(): String = substringBefore("-").trim()

private fun String.toDistrictAddress(): String {
    val tokens = trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val districtIndex = tokens.indexOfFirst { it.endsWith("군") || it.endsWith("구") }
    val cityIndex = tokens.indexOfFirst { it.endsWith("시") }
    val lastAdministrativeIndex = when {
        districtIndex >= 0 -> districtIndex
        cityIndex >= 0 -> cityIndex
        else -> return this
    }
    return tokens.take(lastAdministrativeIndex + 1).joinToString(" ") { token ->
        token
            .removeSuffix("특별자치시")
            .removeSuffix("특별시")
            .removeSuffix("광역시")
            .ifBlank { token }
    }
}

@Preview(name = "Place card - course", showBackground = true, widthDp = 375)
@Composable
private fun PlaceCardCoursePreview() {
    RodiTheme { PlaceCard(HomePreviewData.courseSummary, {}) }
}

@Preview(name = "Place card - parking", showBackground = true, widthDp = 375)
@Composable
private fun PlaceCardParkingPreview() {
    RodiTheme { PlaceCard(HomePreviewData.parkingSummary, {}) }
}

@Preview(name = "Place card - long", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable
private fun PlaceCardLongPreview() {
    RodiTheme { PlaceCard(HomePreviewData.longCourseSummary, {}) }
}
