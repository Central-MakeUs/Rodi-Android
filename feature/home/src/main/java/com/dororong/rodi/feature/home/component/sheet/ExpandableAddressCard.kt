package com.dororong.rodi.feature.home.component.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun ExpandableAddressCard(roadAddress: String, jibunAddress: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, RodiTheme.colors.primary200),
        color = RodiTheme.colors.primary50,
    ) {
        Column(
            modifier = Modifier.padding(top = 10.dp, bottom = 11.dp, start = 10.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("도로명", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
                Text(roadAddress, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("지번", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
                Text(jibunAddress, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
            }
        }
    }
}
