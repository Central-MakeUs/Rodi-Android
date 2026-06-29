package com.dororong.rodi.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.data.SampleCourses
import com.dororong.rodi.directions.KakaoDirectionsClient
import com.dororong.rodi.directions.KakaoDirectionsClient.RouteResult
import com.dororong.rodi.model.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class HomeViewModel : ViewModel() {

    data class UiState(
        val courses: List<Course> = SampleCourses.ROUTI_COURSES,
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

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onDismissDetail() {
        _state.update { it.copy(selectedCourseId = null) }
    }

    fun onCourseClick(id: Int) {
        val current = _state.value
        if (current.selectedCourseId == id) return
        _state.update { it.copy(selectedCourseId = id) }

        if (current.routeByCourse.containsKey(id) || id in current.routingCourseIds) return
        val course = current.courses.firstOrNull { it.id == id } ?: return
        if (course.isParking) return
        _state.update { it.copy(routingCourseIds = it.routingCourseIds + id) }
        viewModelScope.launch {
            val result = KakaoDirectionsClient.getRoute(course)
            _state.update {
                it.copy(
                    routeByCourse = it.routeByCourse + (id to result),
                    routingCourseIds = it.routingCourseIds - id,
                )
            }
        }
    }

    fun onDistanceFilterChange(km: Int?) {
        _state.update { it.copy(distanceFilterKm = km) }
    }

    fun onLocationUpdate(lat: Double, lng: Double) {
        _state.update { it.copy(userLat = lat, userLng = lng) }
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
