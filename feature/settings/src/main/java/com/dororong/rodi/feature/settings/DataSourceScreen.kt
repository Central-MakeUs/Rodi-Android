package com.dororong.rodi.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
@Composable
fun DataSourceScreen(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RodiTheme.colors.white,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            SettingsTopBar(title = "데이터 출처", onBack = onBack)
            Text(
                text = ParkingDataSourceNotice,
                style = RodiTheme.typography.body2Regular,
                color = RodiTheme.colors.black,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            )
        }
    }
}

internal val ParkingDataSourceNotice = """
    주차장 정보는 공공데이터포털에서 제공하는
    「전국주차장정보표준데이터」를 기반으로 가공되었습니다.

    - 데이터명: 전국주차장정보표준데이터
    - 출처: 공공데이터포털
    - 제공기관: 데이터 상세페이지에 기재된 제공기관
    - 데이터 기준일: 2026년 7월 23일

    본 서비스에서 제공하는 정보는 원본 데이터를 가공한 것으로,
    실제 주차장 운영시간, 요금 및 운영 상태와 차이가 있을 수 있습니다.
""".trimIndent()

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun DataSourceScreenPreview() {
    RodiTheme {
        DataSourceScreen(onBack = {})
    }
}
