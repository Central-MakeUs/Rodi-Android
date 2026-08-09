package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.dororong.rodi.core.ui.theme.RodiTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 앵커 바로 아래에 뜨는 목록 메뉴. 레벨 드롭다운과 후기 더보기 메뉴가 공유한다.
 *
 * [scrollState]를 넘기면 스크롤이 시작될 때 스스로 닫힌다. 팝업은 별도 윈도우라 부모가 스크롤돼도
 * 따라 움직이지 않고 허공에 남기 때문에, 목록/시트 안에서 쓸 때는 반드시 넘겨야 한다.
 */
@Composable
fun RodiPopupMenu(
    expanded: Boolean,
    items: List<String>,
    onSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollableState? = null,
) {
    LaunchedEffect(expanded, scrollState) {
        if (!expanded || scrollState == null) return@LaunchedEffect
        snapshotFlow { scrollState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it }
            .collect { onDismissRequest() }
    }

    if (!expanded) return

    val positionProvider = remember { BelowAnchorEndAlignedPositionProvider() }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = modifier
                .widthIn(min = MenuMinWidth)
                .background(RodiTheme.colors.white, MenuShape)
                .border(1.dp, RodiTheme.colors.gray300, MenuShape),
        ) {
            items.forEachIndexed { index, item ->
                Text(
                    text = item,
                    style = RodiTheme.typography.body2Medium,
                    color = RodiTheme.colors.gray700,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                if (index != items.lastIndex) {
                    HorizontalDivider(color = RodiTheme.colors.gray300)
                }
            }
        }
    }
}

private val MenuMinWidth = 75.dp
private val MenuShape = RoundedCornerShape(2.dp)

/** 앵커 바로 아래, 오른쪽 끝을 맞춰 띄운다. 화면 밖으로 나가면 안쪽으로 당긴다. */
private class BelowAnchorEndAlignedPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right - popupContentSize.width
        } else {
            anchorBounds.left
        }
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x.coerceIn(0, maxX), anchorBounds.bottom.coerceIn(0, maxY))
    }
}

@Preview(name = "레벨 선택 툴팁 - 열림", showBackground = true, widthDp = 360, heightDp = 300)
@Composable
private fun RodiPopupMenuLevelPreview() {
    RodiTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(Modifier.align(Alignment.TopEnd)) {
                Text(
                    text = "Rookie  ▾",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray700,
                )
                RodiPopupMenu(
                    expanded = true,
                    items = listOf("Seed", "Rookie", "Owner", "Explorer", "Navigator"),
                    onSelect = {},
                    onDismissRequest = {},
                )
            }
        }
    }
}

@Preview(name = "후기 더보기 툴팁 - 열림", showBackground = true, widthDp = 360, heightDp = 150)
@Composable
private fun RodiPopupMenuReviewPreview() {
    RodiTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(Modifier.align(Alignment.TopEnd)) {
                Text(
                    text = "···",
                    style = RodiTheme.typography.body1SemiBold,
                    color = RodiTheme.colors.gray800,
                )
                RodiPopupMenu(
                    expanded = true,
                    items = listOf("신고하기", "차단"),
                    onSelect = {},
                    onDismissRequest = {},
                )
            }
        }
    }
}
