package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R

@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = RodiTheme.colors.white,
        border = BorderStroke(1.dp, RodiTheme.colors.gray300),
        modifier = modifier.size(46.dp),
    ) {
        Icon(
            painter = painterResource(
                if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
            ),
            contentDescription = if (isBookmarked) "저장 취소" else "장소 저장",
            tint = if (isBookmarked) RodiTheme.colors.primary600 else RodiTheme.colors.gray800,
            modifier = Modifier
                .size(20.dp)
                .padding(13.dp),
        )
    }
}

@Preview(name = "Bookmark - inactive", showBackground = true)
@Composable
private fun BookmarkInactivePreview() {
    RodiTheme { BookmarkButton(false, {}) }
}

@Preview(name = "Bookmark - active", showBackground = true)
@Composable
private fun BookmarkActivePreview() {
    RodiTheme { BookmarkButton(true, {}) }
}
