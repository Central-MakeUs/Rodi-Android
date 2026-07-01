package com.dororong.rodi.feature.home

import com.dororong.rodi.core.data.navi.NaviApp
import com.dororong.rodi.core.domain.Course

sealed interface HomeIntent {
    data class OnCourseClick(val id: Int) : HomeIntent
    data object OnDismissDetail : HomeIntent
    data class OnDistanceFilterChange(val km: Int?) : HomeIntent
    data class OnLocationUpdate(val lat: Double, val lng: Double) : HomeIntent

    /**
     * 경로 안내 버튼 클릭. 설치 여부/저장된 선호 앱 조회는 Context가 필요해 Composable이
     * 미리 계산해서 함께 넘긴다 — ViewModel은 순수 분기 로직만 담당한다.
     */
    data class OnNavigateClick(
        val course: Course,
        val savedApp: NaviApp?,
        val kakaoMapInstalled: Boolean,
        val kakaoNaviInstalled: Boolean,
    ) : HomeIntent

    /** NaviPickerSheet(SELECT 모드)에서 앱을 골랐을 때. */
    data class OnNaviAppSelected(val app: NaviApp, val course: Course, val always: Boolean) : HomeIntent

    /** NaviPickerSheet(INSTALL 모드)에서 앱을 골랐을 때(설치 페이지로 이동). */
    data class OnInstallNaviAppSelected(val app: NaviApp) : HomeIntent
}

sealed interface HomeEffect {
    data class LaunchKakaoMap(val course: Course) : HomeEffect
    data class LaunchKakaoNavi(val course: Course) : HomeEffect
    data class ShowNaviPicker(val course: Course) : HomeEffect
    data class ShowInstallNaviPicker(val course: Course) : HomeEffect
    data class OpenNaviInstallPage(val app: NaviApp) : HomeEffect
    data class SaveNaviPreference(val app: NaviApp) : HomeEffect
}
