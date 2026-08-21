package com.dororong.rodi.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R

@Composable
fun HomeSearchBar(
    onClick: () -> Unit,
    searchKeyword: String? = null,
    showBackButton: Boolean = searchKeyword != null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .semantics {
                contentDescription = if (showBackButton) "뒤로가기" else "지역 또는 코스 검색"
            },
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = RodiTheme.colors.white,
        shadowElevation = 2.5.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 11.dp, bottom = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(
                    if (showBackButton) CoreUiR.drawable.ic_chevron_left else R.drawable.ic_search,
                ),
                contentDescription = null,
                tint = if (showBackButton) RodiTheme.colors.black else Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = searchKeyword ?: "시/군/구/코스명으로 검색하기",
                style = RodiTheme.typography.body2Medium,
                color = if (searchKeyword == null) RodiTheme.colors.gray500 else RodiTheme.colors.black,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun HomeSearchBarPreview() {
    RodiTheme {
        HomeSearchBar(
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun HomeSearchBarResultPreview() {
    RodiTheme {
        HomeSearchBar(
            onClick = {},
            searchKeyword = "서울 중구",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
