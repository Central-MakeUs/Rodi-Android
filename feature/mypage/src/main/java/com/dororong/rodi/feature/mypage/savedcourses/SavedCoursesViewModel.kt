package com.dororong.rodi.feature.mypage.savedcourses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.usecase.course.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.course.ObserveSavedCourseIdsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SavedCoursesUiState(
    val courses: List<Course> = emptyList(),
)

@HiltViewModel
class SavedCoursesViewModel @Inject constructor(
    getCourses: GetCoursesUseCase,
    observeSavedCourseIds: ObserveSavedCourseIdsUseCase,
) : ViewModel() {
    val uiState: StateFlow<SavedCoursesUiState> = observeSavedCourseIds()
        .map { savedCourseIds ->
            SavedCoursesUiState(
                courses = getCourses().filter { it.id in savedCourseIds },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SavedCoursesUiState(),
        )
}
