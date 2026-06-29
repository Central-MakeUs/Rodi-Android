package com.cmc.routi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmc.routi.data.EntryPreferences
import com.cmc.routi.entry.EntryFlow
import com.cmc.routi.home.HomeScreen
import com.cmc.routi.ui.theme.RoutiTheme

/**
 * 앱 진입점 분기: 첫 실행 게이트(EntryFlow) 완료 여부에 따라 게이트 또는 홈을 보여준다.
 * 완료 플래그는 DataStore 로 영속되며, 완료 시 Flow 가 true 로 전환되어 자동으로 홈으로 바뀐다.
 */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { EntryPreferences(context) }
    val completed by prefs.isCompleted.collectAsStateWithLifecycle(initialValue = null)

    when (completed) {
        // 로딩 중: 흰 화면(깜빡임 방지)
        null -> Box(Modifier.fillMaxSize().background(RoutiTheme.colors.white))
        false -> EntryFlow(onComplete = {})
        true -> HomeScreen()
    }
}
