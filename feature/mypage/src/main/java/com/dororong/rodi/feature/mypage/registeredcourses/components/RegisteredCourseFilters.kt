package com.dororong.rodi.feature.mypage.registeredcourses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R
import com.dororong.rodi.feature.mypage.registeredcourses.RegisteredCourseFilter
import kotlinx.coroutines.flow.filter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import com.dororong.rodi.feature.mypage.registeredcourses.registeredCourseFilterMenuItems
import com.dororong.rodi.feature.mypage.registeredcourses.resolveRegisteredCourseFilterSelection

@Composable
internal fun RegisteredCourseFilters(
    selectedFilter: RegisteredCourseFilter,
    onFilterSelected: (RegisteredCourseFilter) -> Unit,
    initiallyExpanded: Boolean,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    // Popup의 바깥 터치 닫기가 앵커 클릭보다 먼저 돌아서, 다시 누르면 닫혔다가 곧바로
    // 다시 열려 토글이 안 되는 것처럼 보였다. 닫힌 직후 짧은 시간은 다시 열지 않는다.
    var lastDismissedAtMillis by remember { mutableLongStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val popupPositionProvider = remember(density) {
        RegisteredCourseFilterPopupPositionProvider(density)
    }
    Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(48.dp)
                .padding(end = 16.dp)
                .clickable(interactionSource = interactionSource, indication = null) {
                    when {
                        expanded -> expanded = false
                        System.currentTimeMillis() - lastDismissedAtMillis > FilterReopenGuardMillis ->
                            expanded = true
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = if (expanded) "접기" else selectedFilter.label,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray700,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down_16),
                contentDescription = if (expanded) "필터 닫기" else "필터 열기",
                tint = RodiTheme.colors.gray700,
                modifier = Modifier.size(16.dp).then(if (expanded) Modifier.rotate(180f) else Modifier),
            )
        }
        if (expanded) {
            val onSelected: (RegisteredCourseFilter) -> Unit = { filter ->
                expanded = false
                onFilterSelected(resolveRegisteredCourseFilterSelection(selectedFilter, filter))
            }
            if (LocalInspectionMode.current) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp),
                ) {
                    RegisteredCourseFilterMenuSurface(
                        selectedFilter = selectedFilter,
                        onSelected = onSelected,
                    )
                }
            } else {
                Popup(
                    popupPositionProvider = popupPositionProvider,
                    onDismissRequest = {
                        expanded = false
                        lastDismissedAtMillis = System.currentTimeMillis()
                    },
                ) {
                    RegisteredCourseFilterMenuSurface(
                        selectedFilter = selectedFilter,
                        onSelected = onSelected,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RegisteredCourseFilterMenuSurface(
    selectedFilter: RegisteredCourseFilter,
    onSelected: (RegisteredCourseFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(75.dp)
            .background(RodiTheme.colors.white)
            .border(1.dp, RodiTheme.colors.gray300),
    ) {
        // 구분선을 drawBehind로 그리면 각 항목의 배경이 그 위를 덮어 보이지 않는다.
        // 항목 사이에 실제로 끼워 넣는다.
        val filters = registeredCourseFilterMenuItems(selectedFilter)
        filters.forEachIndexed { index, filter ->
            if (index > 0) {
                HorizontalDivider(color = RodiTheme.colors.gray300)
            }
            Text(
                text = filter.label,
                style = RodiTheme.typography.body2Medium,
                color = RodiTheme.colors.gray700,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .background(if (filter == selectedFilter) RodiTheme.colors.gray300 else RodiTheme.colors.white)
                    .clickable { onSelected(filter) }
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = 12.dp),
            )
        }
    }
}

private const val FilterReopenGuardMillis = 300L

private class RegisteredCourseFilterPopupPositionProvider(
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.right - with(density) { 16.dp.roundToPx() } - popupContentSize.width).coerceIn(
            0,
            (windowSize.width - popupContentSize.width).coerceAtLeast(0),
        )
        // 28dp를 쓰면 메뉴가 "전체" 라벨 아래쪽을 덮는다. 앵커 행 아래에서 시작한다.
        val y = anchorBounds.bottom.coerceIn(
            0,
            (windowSize.height - popupContentSize.height).coerceAtLeast(0),
        )
        return IntOffset(x, y)
    }
}
