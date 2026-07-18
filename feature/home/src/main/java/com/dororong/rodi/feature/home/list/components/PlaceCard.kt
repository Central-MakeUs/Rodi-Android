package com.dororong.rodi.feature.home.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = place.name,
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val distanceMeters = place.distanceMeters
            if (place.type == PlaceType.COURSE && distanceMeters != null) {
                Text(
                    text = distanceMeters.toDistanceText(),
                    style = RodiTheme.typography.body3SemiBold,
                    color = RodiTheme.colors.primary600,
                )
                Text(
                    text = " 주행거리",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                )
            }
        }
        Text(
            text = place.address,
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PracticeTagRow(place.practiceTypes)
        if (place.type == PlaceType.COURSE) {
            place.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    style = RodiTheme.typography.caption1Regular,
                    color = RodiTheme.colors.gray700,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                place.openTime?.let { openTime ->
                    Text(
                        text = "$openTime 영업 시작",
                        style = RodiTheme.typography.body3SemiBold,
                        color = RodiTheme.colors.primary600,
                    )
                }
                if (place.openTime != null && place.capacity != null) {
                    Text("·", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
                }
                place.capacity?.let { capacity ->
                    Text(
                        text = "총 ${NumberFormat.getNumberInstance(Locale.KOREA).format(capacity)}면",
                        style = RodiTheme.typography.body3Medium,
                        color = RodiTheme.colors.gray800,
                    )
                }
                Spacer(Modifier.weight(1f))
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
