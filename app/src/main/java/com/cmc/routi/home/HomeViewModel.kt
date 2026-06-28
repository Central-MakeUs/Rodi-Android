package com.cmc.routi.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmc.routi.data.SampleCourses
import com.cmc.routi.directions.KakaoDirectionsClient
import com.cmc.routi.directions.KakaoDirectionsClient.RouteResult
import com.cmc.routi.model.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    data class UiState(
        val courses: List<Course> = SampleCourses.ALL,
        val selectedCourseId: String? = null,
        val routeByCourse: Map<String, RouteResult> = emptyMap(),
        val isRouting: Boolean = false,
    ) {
        val selectedCourse: Course? get() = courses.firstOrNull { it.id == selectedCourseId }
        val selectedRoute: RouteResult? get() = selectedCourseId?.let { routeByCourse[it] }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** ✕ 탭: 상세 모드 → 리스트 모드 */
    fun onDismissDetail() {
        _state.update { it.copy(selectedCourseId = null) }
    }

    /** 코스 탭: 상세 모드 진입. 이미 선택된 코스라도 deselect 없이 유지(✕로만 닫음). */
    fun onCourseClick(id: String) {
        val current = _state.value
        if (current.selectedCourseId == id) return
        _state.update { it.copy(selectedCourseId = id) }

        if (current.routeByCourse.containsKey(id)) return // 캐시 있음
        val course = current.courses.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(isRouting = true) }
        viewModelScope.launch {
            val result = KakaoDirectionsClient.getRoute(course)
            _state.update {
                it.copy(
                    routeByCourse = it.routeByCourse + (id to result),
                    isRouting = false,
                )
            }
        }
    }
}
