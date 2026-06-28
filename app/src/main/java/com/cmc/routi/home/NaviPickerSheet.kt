package com.cmc.routi.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cmc.routi.R
import com.cmc.routi.navi.NaviApp
import com.cmc.routi.ui.theme.RoutiTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * 내비게이션 앱 선택 시트.
 * 카카오맵 / 카카오내비 카드 중 선택 후 "이번만" 또는 "항상"으로 실행.
 * "항상" 선택 시 [NaviPreference]에 저장해 다음 실행부터 시트 없이 바로 진행.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaviPickerSheet(
    onDismiss: () -> Unit,
    onSelect: (app: NaviApp, always: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedApp by remember { mutableStateOf<NaviApp?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RoutiTheme.colors.white,
        dragHandle = null,
    ) {
        NaviPickerContent(
            selectedApp = selectedApp,
            onAppSelect = { selectedApp = it },
            onConfirm = { app, always ->
                onSelect(app, always)
            },
        )
    }
}

@Composable
private fun NaviPickerContent(
    selectedApp: NaviApp?,
    onAppSelect: (NaviApp) -> Unit,
    onConfirm: (app: NaviApp, always: Boolean) -> Unit,
) {
    val canConfirm = selectedApp != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 40.dp, bottom = 8.dp),
    ) {
        Text(
            "내비게이션 앱을 선택하세요",
            style = RoutiTheme.typography.headline1,
            color = RoutiTheme.colors.black,
        )
        Spacer(Modifier.height(20.dp))

        // 앱 카드 가로 배치
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NaviApp.entries.forEach { app ->
                NaviAppCard(
                    app = app,
                    isSelected = selectedApp == app,
                    onClick = { onAppSelect(app) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 이번만 | 항상 버튼
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = canConfirm) { selectedApp?.let { onConfirm(it, false) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "이번만",
                        style = RoutiTheme.typography.body1SemiBold,
                        color = if (canConfirm) RoutiTheme.colors.black else RoutiTheme.colors.gray400,
                    )
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(RoutiTheme.colors.gray200),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = canConfirm) { selectedApp?.let { onConfirm(it, true) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "항상",
                        style = RoutiTheme.typography.body1SemiBold,
                        color = if (canConfirm) RoutiTheme.colors.black else RoutiTheme.colors.gray400,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NaviAppCard(
    app: NaviApp,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val iconRes = when (app) {
        NaviApp.KAKAOMAP -> R.drawable.img_navi_kakaomap
        NaviApp.KAKAONAVI -> R.drawable.img_navi_kakaonavi
    }
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) RoutiTheme.colors.gray100 else RoutiTheme.colors.white)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = app.label,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            app.label,
            style = RoutiTheme.typography.body3Medium,
            color = RoutiTheme.colors.black,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NaviPickerContentSelectedPreview() {
    RoutiTheme {
        NaviPickerContent(
            selectedApp = NaviApp.KAKAOMAP,
            onAppSelect = {},
            onConfirm = { _, _ -> },
        )
    }
}
