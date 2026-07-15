package com.dororong.rodi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.usecase.course.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetLocationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.entry.MarkLocationPermissionRequestedUseCase
import com.dororong.rodi.feature.home.map.MapViewport
import com.dororong.rodi.core.domain.usecase.navi.GetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.course.GetRouteUseCase
import com.dororong.rodi.core.domain.usecase.navi.SetNaviAlwaysUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCoursesUseCase: GetCoursesUseCase,
    private val getRouteUseCase: GetRouteUseCase,
    private val getNaviAlwaysUseCase: GetNaviAlwaysUseCase,
    private val setNaviAlwaysUseCase: SetNaviAlwaysUseCase,
    getLocationPermissionRequested: GetLocationPermissionRequestedUseCase,
    private val markLocationPermissionRequestedUseCase: MarkLocationPermissionRequestedUseCase,
) : ViewModel() {

    private val allCourses = getCoursesUseCase()

    data class UiState(
        val courses: List<Course> = emptyList(),
        val selectedCourseId: Int? = null,
        val routeByCourse: Map<Int, RouteResult> = emptyMap(),
        val routingCourseIds: Set<Int> = emptySet(),
    ) {
        val selectedCourse: Course? get() = courses.firstOrNull { it.id == selectedCourseId }
        val selectedRoute: RouteResult? get() = selectedCourseId?.let { routeByCourse[it] }
        val isRouting: Boolean get() = selectedCourseId in routingCourseIds
    }

    private val _state = MutableStateFlow(UiState(courses = allCourses))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()
    val hasRequestedLocationPermission: Flow<Boolean> = getLocationPermissionRequested()

    fun markLocationPermissionRequested() {
        viewModelScope.launch { markLocationPermissionRequestedUseCase() }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnCourseClick -> onCourseClick(intent.id)
            HomeIntent.OnDismissDetail -> onDismissDetail()
            is HomeIntent.OnMapSearch -> onMapSearch(intent.viewport)
            is HomeIntent.OnNavigateClick -> onNavigateClick(intent)
            is HomeIntent.OnNaviAppSelected -> onNaviAppSelected(intent)
            is HomeIntent.OnInstallNaviAppSelected -> onInstallNaviAppSelected(intent)
        }
    }

    private fun onDismissDetail() {
        _state.update { it.copy(selectedCourseId = null) }
    }

    private fun onCourseClick(id: Int) {
        val current = _state.value
        if (current.selectedCourseId == id) return
        _state.update { it.copy(selectedCourseId = id) }

        if (current.routeByCourse.containsKey(id) || id in current.routingCourseIds) return
        val course = current.courses.firstOrNull { it.id == id } ?: return
        if (course.isParking) return
        _state.update { it.copy(routingCourseIds = it.routingCourseIds + id) }
        viewModelScope.launch {
            getRouteUseCase(course)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            routeByCourse = it.routeByCourse + (id to result),
                            routingCourseIds = it.routingCourseIds - id,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(routingCourseIds = it.routingCourseIds - id) }
                }
        }
    }

    private fun onMapSearch(viewport: MapViewport) {
        _state.update {
            it.copy(
                courses = allCourses.filter { course ->
                    course.startWaypoint.lat in viewport.southWest.lat..viewport.northEast.lat &&
                        course.startWaypoint.lng in viewport.southWest.lng..viewport.northEast.lng
                },
            )
        }
    }

    private fun onNavigateClick(intent: HomeIntent.OnNavigateClick) {
        viewModelScope.launch {
            val savedApp = getNaviAlwaysUseCase()
            when {
                savedApp == NaviApp.KAKAOMAP && intent.kakaoMapInstalled ->
                    _effect.send(HomeEffect.LaunchKakaoMap(intent.course))

                savedApp == NaviApp.KAKAONAVI && intent.kakaoNaviInstalled ->
                    _effect.send(HomeEffect.LaunchKakaoNavi(intent.course))

                intent.kakaoMapInstalled && intent.kakaoNaviInstalled ->
                    _effect.send(HomeEffect.ShowNaviPicker(intent.course))

                intent.kakaoMapInstalled -> _effect.send(HomeEffect.LaunchKakaoMap(intent.course))
                intent.kakaoNaviInstalled -> _effect.send(HomeEffect.LaunchKakaoNavi(intent.course))
                else -> _effect.send(HomeEffect.ShowInstallNaviPicker(intent.course))
            }
        }
    }

    private fun onNaviAppSelected(intent: HomeIntent.OnNaviAppSelected) {
        viewModelScope.launch {
            if (intent.always) setNaviAlwaysUseCase(intent.app)
            when (intent.app) {
                NaviApp.KAKAOMAP -> _effect.send(HomeEffect.LaunchKakaoMap(intent.course))
                NaviApp.KAKAONAVI -> _effect.send(HomeEffect.LaunchKakaoNavi(intent.course))
            }
        }
    }

    private fun onInstallNaviAppSelected(intent: HomeIntent.OnInstallNaviAppSelected) {
        viewModelScope.launch {
            _effect.send(HomeEffect.OpenNaviInstallPage(intent.app))
        }
    }
}
