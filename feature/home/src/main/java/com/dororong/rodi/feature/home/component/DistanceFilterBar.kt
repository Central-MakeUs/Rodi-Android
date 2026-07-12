package com.dororong.rodi.feature.home.component

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
import com.dororong.rodi.feature.home.R

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
