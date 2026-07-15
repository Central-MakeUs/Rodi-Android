package com.dororong.rodi.feature.home

import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.feature.home.map.MapViewport
import com.dororong.rodi.core.domain.model.navi.NaviApp

sealed interface HomeIntent {
    data class OnCourseClick(val id: Int) : HomeIntent
    data object OnDismissDetail : HomeIntent
    data class OnDistanceFilterChange(val km: Int?) : HomeIntent
    data class OnLocationUpdate(val lat: Double, val lng: Double) : HomeIntent
    data class OnMapSearch(val viewport: MapViewport) : HomeIntent
    data object OnSettingsClick : HomeIntent

    data class OnNavigateClick(
        val course: Course,
        val kakaoMapInstalled: Boolean,
        val kakaoNaviInstalled: Boolean,
    ) : HomeIntent

    data class OnNaviAppSelected(val app: NaviApp, val course: Course, val always: Boolean) : HomeIntent

    data class OnInstallNaviAppSelected(val app: NaviApp) : HomeIntent
}

sealed interface HomeEffect {
    data object NavigateSettings : HomeEffect
    data class LaunchKakaoMap(val course: Course) : HomeEffect
    data class LaunchKakaoNavi(val course: Course) : HomeEffect
    data class ShowNaviPicker(val course: Course) : HomeEffect
    data class ShowInstallNaviPicker(val course: Course) : HomeEffect
    data class OpenNaviInstallPage(val app: NaviApp) : HomeEffect
}
