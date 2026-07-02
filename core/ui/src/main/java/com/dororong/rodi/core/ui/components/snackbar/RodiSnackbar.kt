package com.dororong.rodi.core.ui.components.snackbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RodiSnackbar(
    data: RodiSnackbarData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(RodiRadius.md))
            .background(RodiTheme.colors.black)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        data.icon?.let {
            Image(
                painter = it,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = data.message,
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.white,
            modifier = Modifier.weight(1f),
        )
        if (data.actionLabel != null && data.onAction != null) {
            TextButton(onClick = data.onAction) {
                Text(
                    text = data.actionLabel,
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.primary300,
                )
            }
        }
    }
}
