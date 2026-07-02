package com.dororong.rodi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.domain.RouteResult
import com.dororong.rodi.core.domain.usecase.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.GetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.GetRouteUseCase
import com.dororong.rodi.core.domain.usecase.SetNaviAlwaysUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCoursesUseCase: GetCoursesUseCase,
    private val getRouteUseCase: GetRouteUseCase,
    private val getNaviAlwaysUseCase: GetNaviAlwaysUseCase,
    private val setNaviAlwaysUseCase: SetNaviAlwaysUseCase,
) : ViewModel() {

    data class UiState(
        val courses: List<Course> = emptyList(),
        val selectedCourseId: Int? = null,
        val routeByCourse: Map<Int, RouteResult> = emptyMap(),
        val routingCourseIds: Set<Int> = emptySet(),
        val distanceFilterKm: Int? = null,    // null=전체, 3, 5, 10
        val userLat: Double? = null,
        val userLng: Double? = null,
    ) {
        val selectedCourse: Course? get() = courses.firstOrNull { it.id == selectedCourseId }
        val selectedRoute: RouteResult? get() = selectedCourseId?.let { routeByCourse[it] }
        val isRouting: Boolean get() = selectedCourseId in routingCourseIds

        val filteredCourses: List<Course>
            get() {
                val km = distanceFilterKm ?: return courses
                val lat = userLat ?: return courses
                val lng = userLng ?: return courses
                val limitM = km * 1000.0
                return courses.filter { course ->
                    haversineMeters(lat, lng, course.startWaypoint.lat, course.startWaypoint.lng) <= limitM
                }
            }
    }

    private val _state = MutableStateFlow(UiState(courses = getCoursesUseCase()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnCourseClick -> onCourseClick(intent.id)
            HomeIntent.OnDismissDetail -> onDismissDetail()
            is HomeIntent.OnDistanceFilterChange -> onDistanceFilterChange(intent.km)
            is HomeIntent.OnLocationUpdate -> onLocationUpdate(intent.lat, intent.lng)
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

    private fun onDistanceFilterChange(km: Int?) {
        _state.update { it.copy(distanceFilterKm = km) }
    }

    private fun onLocationUpdate(lat: Double, lng: Double) {
        _state.update { it.copy(userLat = lat, userLng = lng) }
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

    companion object {
        private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6_371_000.0
            val φ1 = Math.toRadians(lat1)
            val φ2 = Math.toRadians(lat2)
            val dφ = Math.toRadians(lat2 - lat1)
            val dλ = Math.toRadians(lng2 - lng1)
            val a = sin(dφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(dλ / 2).pow(2)
            return 2 * r * asin(sqrt(a))
        }
    }
}
