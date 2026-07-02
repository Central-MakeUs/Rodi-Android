package com.dororong.rodi.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.ui.theme.RodiTheme

enum class NaviPickerMode {
    SELECT,
    INSTALL
}

/**
 * 내비게이션 앱 선택 다이얼로그.
 * 하단에 띄워지는 부유형 다이얼로그 형태로 구현.
 */
@Composable
fun NaviPickerSheet(
    mode: NaviPickerMode = NaviPickerMode.SELECT,
    onDismiss: () -> Unit,
    onSelect: (app: NaviApp, always: Boolean) -> Unit,
) {
    var selectedApp by remember { mutableStateOf<NaviApp?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = RodiTheme.colors.white
            ) {
                NaviPickerContent(
                    mode = mode,
                    onClose = onDismiss,
                    selectedApp = selectedApp,
                    onAppSelect = { selectedApp = it },
                    onConfirm = { app, always ->
                        onSelect(app, always)
                    },
                )
            }
        }
    }
}

@Composable
private fun NaviPickerContent(
    mode: NaviPickerMode,
    onClose: () -> Unit,
    selectedApp: NaviApp?,
    onAppSelect: (NaviApp) -> Unit,
    onConfirm: (app: NaviApp, always: Boolean) -> Unit,
) {
    val canConfirm = selectedApp != null
    val title = when (mode) {
        NaviPickerMode.SELECT -> "내비게이션 앱을 선택하세요"
        NaviPickerMode.INSTALL -> "길 안내를 위해 내비게이션 앱\n설치가 필요합니다."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = "닫기",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier
                        .padding(top = 16.dp, end = 16.dp)
                        .size(24.dp),
                )
            }
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = title,
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 앱 카드 가로 배치
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            NaviApp.entries.forEachIndexed { index, app ->
                if (index > 0) Spacer(Modifier.width(32.dp))
                NaviAppCard(
                    app = app,
                    isSelected = selectedApp == app,
                    onClick = { onAppSelect(app) },
                )
            }
        }

        if (mode == NaviPickerMode.SELECT) {
            Spacer(Modifier.height(16.dp))

            // 이번만 | 항상 버튼 (가로 배치된 두 개의 독립된 버튼)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 이번만 버튼
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, RodiTheme.colors.gray200, RoundedCornerShape(8.dp))
                        .clickable(enabled = canConfirm) { selectedApp?.let { onConfirm(it, false) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "이번만",
                        style = RodiTheme.typography.button1,
                        color = if (canConfirm) RodiTheme.colors.black else RodiTheme.colors.gray400,
                    )
                }

                // 항상 버튼
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canConfirm) RodiTheme.colors.primary600 else RodiTheme.colors.gray200)
                        .clickable(enabled = canConfirm) { selectedApp?.let { onConfirm(it, true) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "항상",
                        style = RodiTheme.typography.button1,
                        color = if (canConfirm) RodiTheme.colors.white else RodiTheme.colors.gray400,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (canConfirm) RodiTheme.colors.primary600 else RodiTheme.colors.gray200)
                    .clickable(enabled = canConfirm) { selectedApp?.let { onConfirm(it, false) } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "설치하기",
                    style = RodiTheme.typography.button1,
                    color = if (canConfirm) RodiTheme.colors.white else RodiTheme.colors.gray400,
                )
            }
        }
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
    Box(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = app.label,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Text(
                app.label,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.black,
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(RodiTheme.colors.gray400.copy(alpha = 0.24f)),
            )
        }
    }
}

@Preview(name = "NaviPicker - Select", showBackground = true, widthDp = 360)
@Composable
private fun NaviPickerSelectPreview() {
    RodiTheme {
        NaviPickerContent(
            mode = NaviPickerMode.SELECT,
            onClose = {},
            selectedApp = NaviApp.KAKAOMAP,
            onAppSelect = {},
            onConfirm = { _, _ -> },
        )
    }
}

@Preview(name = "NaviPicker - Install", showBackground = true, widthDp = 360)
@Composable
private fun NaviPickerInstallPreview() {
    RodiTheme {
        NaviPickerContent(
            mode = NaviPickerMode.INSTALL,
            onClose = {},
            selectedApp = null,
            onAppSelect = {},
            onConfirm = { _, _ -> },
        )
    }
}
