package com.dororong.rodi.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun DistanceFilterBar(
    selectedKm: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options: List<Pair<String, Int?>> = listOf(
        "전체" to null,
        "3km" to 3,
        "5km" to 5,
        "10km" to 10,
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = RodiTheme.colors.white,
        border = BorderStroke(1.dp, RodiTheme.colors.primary100),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (label, km) ->
                val selected = selectedKm == km
                val shape = RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(if (selected) RodiTheme.colors.primary600 else Color.Transparent)
                        .clickable { onSelect(km) }
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = RodiTheme.typography.body1Medium,
                        color = if (selected) RodiTheme.colors.white else RodiTheme.colors.gray600,
                    )
                }
            }
        }
    }
}


@Composable
fun MyLocationButton(isActive: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = RodiTheme.colors.white,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_crosshair),
                contentDescription = "현재 위치",
                tint = if (isActive) RodiTheme.colors.primary600 else RodiTheme.colors.gray900,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun SettingsButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = "설정" },
        shape = CircleShape,
        color = RodiTheme.colors.white,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = null,
                tint = RodiTheme.colors.gray900,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview(name = "MyLocationButton - Inactive/Active", showBackground = true, widthDp = 360)
@Composable
private fun MyLocationButtonPreview() {
    RodiTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            MyLocationButton(isActive = false, onClick = {})
            MyLocationButton(isActive = true, onClick = {})
        }
    }
}

@Preview(name = "SettingsButton - Default", showBackground = true, widthDp = 360)
@Composable
private fun SettingsButtonPreview() {
    RodiTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SettingsButton(onClick = {})
        }
    }
}

@Preview(name = "DistanceFilterBar - Selections", showBackground = true, widthDp = 360)
@Composable
private fun DistanceFilterBarPreview() {
    RodiTheme {
        Column(
            modifier = Modifier
                .background(Color.LightGray)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DistanceFilterBar(selectedKm = null, onSelect = {})
            DistanceFilterBar(selectedKm = 3, onSelect = {})
            DistanceFilterBar(selectedKm = 5, onSelect = {})
        }
    }
}

@Preview(name = "MapControls - Combined", showBackground = true, widthDp = 360, heightDp = 220)
@Composable
private fun MapControlsCombinedPreview() {
    RodiTheme {
        Column(
            modifier = Modifier
                .background(RodiTheme.colors.gray200)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DistanceFilterBar(selectedKm = null, onSelect = {})
            DistanceFilterBar(selectedKm = 10, onSelect = {})
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MyLocationButton(isActive = false, onClick = {})
                MyLocationButton(isActive = true, onClick = {})
                SettingsButton(onClick = {})
            }
        }
    }
}
