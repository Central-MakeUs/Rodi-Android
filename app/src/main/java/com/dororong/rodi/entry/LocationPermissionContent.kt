package com.dororong.rodi.entry

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.R
import com.dororong.rodi.ui.theme.RoutiTheme

/**
 * 1단계: 위치 권한 안내. "허용하기" → 시스템 권한 다이얼로그 → 결과와 무관하게 다음 단계로.
 */
@Composable
fun LocationPermissionContent(
    onPermissionResolved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { onPermissionResolved() }

    EntryScaffold(
        currentStep = 1,
        onBack = null,
        buttonText = "계속하기",
        buttonEnabled = true,
        onButtonClick = {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "현재 위치를 기반으로 주변 운전 연습 코스를\n추천하기 위해 위치정보를 사용합니다.",
                style = RoutiTheme.typography.headline1,
                color = RoutiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.illust_location_permission),
                contentDescription = null,
                modifier = Modifier.size(200.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "내 주변 운전 연습 코스를 찾으려면\n위치 정보 허용이 꼭 필요해요.",
                style = RoutiTheme.typography.body3Medium,
                color = RoutiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
        }
        Box(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun LocationPermissionPreview() {
    RoutiTheme {
        LocationPermissionContent(onPermissionResolved = {})
    }
}
