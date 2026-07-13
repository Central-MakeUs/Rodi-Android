package com.dororong.rodi.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.feature.home.map.MapMarkerMode
import java.util.Locale

@Composable
fun ClusterLabPanel(
    zoomLevel: Int,
    mode: MapMarkerMode,
    columns: Int?,
    rows: Int?,
    query: MapViewportQuery?,
    courseCount: Int,
    clusterCount: Int,
    isLoading: Boolean,
    hasError: Boolean,
    onZoomSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(276.dp),
        color = RodiTheme.colors.white.copy(alpha = 0.94f),
        shape = RoundedCornerShape(RodiRadius.md),
        border = BorderStroke(1.dp, RodiTheme.colors.primary200),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "CLUSTER LAB",
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.primary700,
            )
            Text(
                text = "zoom $zoomLevel · ${mode.label} · " + when (mode) {
                    MapMarkerMode.NATIONAL_CLUSTER -> "${columns ?: "-"}×${rows ?: "-"}"
                    MapMarkerMode.REGIONAL_CLUSTER -> "반경 56dp"
                    MapMarkerMode.INDIVIDUAL -> "개별"
                },
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray900,
            )
            Text(
                text = "items $courseCount · clusters $clusterCount" + when {
                    isLoading -> " · loading"
                    hasError -> " · error"
                    else -> ""
                },
                style = RodiTheme.typography.caption1Medium,
                color = if (hasError) RodiTheme.colors.pointRed else RodiTheme.colors.gray700,
            )
            Text(
                text = query?.let {
                    "NE ${it.northEast.short()}\nSW ${it.southWest.short()}"
                } ?: "NE -\nSW -",
                style = RodiTheme.typography.caption2Regular,
                color = RodiTheme.colors.gray600,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(7, 10, 11, 13, 14).forEach { zoom ->
                    Surface(
                        onClick = { onZoomSelected(zoom) },
                        color = if (zoom == zoomLevel) {
                            RodiTheme.colors.primary600
                        } else {
                            RodiTheme.colors.gray100
                        },
                        shape = RoundedCornerShape(RodiRadius.sm),
                    ) {
                        Text(
                            text = zoom.toString(),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            style = RodiTheme.typography.caption1Medium,
                            color = if (zoom == zoomLevel) {
                                RodiTheme.colors.white
                            } else {
                                RodiTheme.colors.gray800
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun GeoPoint.short(): String =
    String.format(Locale.US, "%.4f, %.4f", lat, lng)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ClusterLabPanelPreview() {
    RodiTheme {
        ClusterLabPanel(
            zoomLevel = 13,
            mode = MapMarkerMode.REGIONAL_CLUSTER,
            columns = 4,
            rows = 6,
            query = MapViewportQuery(
                northEast = GeoPoint(37.7, 127.2),
                southWest = GeoPoint(37.3, 126.7),
                zoomLevel = 13,
            ),
            courseCount = 42,
            clusterCount = 16,
            isLoading = false,
            hasError = false,
            onZoomSelected = {},
        )
    }
}
