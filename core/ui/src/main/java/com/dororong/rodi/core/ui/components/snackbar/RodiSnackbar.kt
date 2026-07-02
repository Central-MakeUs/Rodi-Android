package com.dororong.rodi.core.ui.components.snackbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R
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
            .clip(RoundedCornerShape(RodiRadius.md))
            .background(RodiTheme.colors.gray800)
            .padding(16.dp),
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (data.actionLabel != null && data.onAction != null) {
            Text(
                text = data.actionLabel,
                style = RodiTheme.typography.caption2Medium,
                color = RodiTheme.colors.white,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RodiTheme.colors.primary600)
                    .clickable(onClick = data.onAction)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Preview(name = "RodiSnackbar - 텍스트만", showBackground = true, widthDp = 360)
@Composable
private fun RodiSnackbarTextOnlyPreview() {
    RodiTheme {
        RodiSnackbar(
            data = RodiSnackbarData(message = "저장되었습니다"),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "RodiSnackbar - 아이콘+액션", showBackground = true, widthDp = 360)
@Composable
private fun RodiSnackbarIconActionPreview() {
    RodiTheme {
        RodiSnackbar(
            data = RodiSnackbarData(
                message = "완료되었습니다",
                icon = painterResource(R.drawable.ic_alert_circle),
                actionLabel = "닫기",
                onAction = {},
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "RodiSnackbar - 긴 텍스트(2줄)", showBackground = true, widthDp = 360)
@Composable
private fun RodiSnackbarLongTextPreview() {
    RodiTheme {
        RodiSnackbar(
            data = RodiSnackbarData(
                message = "이것은 매우 긴 스낵바 메시지입니다 두 줄을 넘어가는 경우 " +
                    "말줄임표로 잘리는지 확인하기 위한 테스트 문구입니다",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "RodiSnackbar - 네트워크 에러(Figma 538:6700)", showBackground = true, widthDp = 360)
@Composable
private fun RodiSnackbarNetworkErrorPreview() {
    RodiTheme {
        RodiSnackbar(
            data = RodiSnackbarData(
                message = "네트워크 연결이 원활하지 않아요.\n다시 시도해볼까요?",
                icon = painterResource(R.drawable.ic_alert_circle),
                actionLabel = "새로고침",
                onAction = {},
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
