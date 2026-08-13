package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceWaypoint
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R

private data class RouteRowItem(
    val label: String,
    val name: String,
    val color: Color,
    val dotColor: Color,
)

@Composable
fun RouteInfoSection(
    waypoints: List<PlaceWaypoint>,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    val startColor = RodiTheme.semantic.pinStart
    val arrivalColor = RodiTheme.semantic.pinArrival
    val viaColor = RodiTheme.colors.gray800
    val viaDotColor = RodiTheme.colors.gray400

    val allRows = remember(waypoints, startColor, viaColor, viaDotColor, arrivalColor) {
        buildRouteRows(waypoints, startColor, viaColor, viaDotColor, arrivalColor)
    }
    if (allRows.isEmpty()) return
    val rows = if (expanded) allRows else allRows.take(1)

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "경로 정보",
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
        )

        Column {
            rows.forEachIndexed { index, row ->
                RouteRow(row)
                if (index != rows.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(16.dp)
                            .background(RodiTheme.colors.gray400, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }

        if (allRows.size > 1) {
            val buttonShape = RoundedCornerShape(8.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(buttonShape)
                    .border(1.dp, RodiTheme.colors.gray300, buttonShape)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 20.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "접기" else "자세히 보기",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = RodiTheme.colors.gray800,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f),
                )
            }
        }
    }
}

@Composable
private fun RouteRow(row: RouteRowItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.width(85.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(9.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(7.dp).background(row.dotColor, CircleShape))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = row.label,
                style = RodiTheme.typography.body1Medium,
                color = row.color,
            )
        }
        Text(
            text = row.name,
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.gray800,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun buildRouteRows(
    waypoints: List<PlaceWaypoint>,
    startColor: Color,
    viaColor: Color,
    viaDotColor: Color,
    arrivalColor: Color,
): List<RouteRowItem> {
    val sortedWaypoints = waypoints
        .filter { !it.name.isNullOrBlank() }
        .sortedBy { it.sequence }
    return sortedWaypoints.mapIndexed { index, waypoint ->
            val (label, color, dotColor) = when (waypoint.type) {
                PlaceWaypointType.START -> Triple("출발지", startColor, startColor)
                PlaceWaypointType.DESTINATION -> Triple("도착지", arrivalColor, arrivalColor)
                PlaceWaypointType.VIA -> {
                    val viaNumber = sortedWaypoints.take(index + 1).count { it.type == PlaceWaypointType.VIA }
                    Triple("경유지 $viaNumber", viaColor, viaDotColor)
                }
            }
            RouteRowItem(label = label, name = waypoint.name.orEmpty(), color = color, dotColor = dotColor)
        }
}

@Preview(name = "Route info - 접힘", showBackground = true, widthDp = 375)
@Composable
private fun RouteInfoCollapsedPreview() {
    RodiTheme { RouteInfoSection(previewWaypoints) }
}

@Preview(name = "Route info - 펼침", showBackground = true, widthDp = 375)
@Composable
private fun RouteInfoExpandedPreview() {
    RodiTheme { RouteInfoSection(previewWaypoints, initiallyExpanded = true) }
}

@Preview(name = "Route info - 경유지 없음", showBackground = true, widthDp = 375)
@Composable
private fun RouteInfoNoViaPreview() {
    RodiTheme {
        RouteInfoSection(
            waypoints = previewWaypoints.filter { it.type != PlaceWaypointType.VIA },
            initiallyExpanded = true,
        )
    }
}

@Preview(name = "Route info - 출발지만", showBackground = true, widthDp = 375)
@Composable
private fun RouteInfoStartOnlyPreview() {
    RodiTheme { RouteInfoSection(previewWaypoints.take(1)) }
}

private val previewWaypoints = listOf(
    PlaceWaypoint(PlaceWaypointType.START, 0, GeoPoint(37.5, 127.0), "가톨릭대학교 서울성모병원"),
    PlaceWaypoint(PlaceWaypointType.VIA, 1, GeoPoint(37.5, 127.0), "반포역"),
    PlaceWaypoint(PlaceWaypointType.VIA, 2, GeoPoint(37.5, 127.0), "고속터미널역"),
    PlaceWaypoint(PlaceWaypointType.DESTINATION, 3, GeoPoint(37.5, 127.0), "망원한강공원"),
)
