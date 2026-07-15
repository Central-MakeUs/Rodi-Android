package com.dororong.rodi.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R

@Composable
fun MapListButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { contentDescription = "목록 열기" },
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = RodiTheme.colors.white,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_list),
                contentDescription = null,
                tint = RodiTheme.colors.gray900,
            )
            Text(
                text = "목록열기",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray900,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MapListButtonPreview() {
    RodiTheme { MapListButton(onClick = {}) }
}
