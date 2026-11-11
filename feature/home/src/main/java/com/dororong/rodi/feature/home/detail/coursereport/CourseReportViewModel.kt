package com.dororong.rodi.feature.home.detail.coursereport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.domain.model.course.CourseReportSubmission
import com.dororong.rodi.core.domain.usecase.course.GetCourseReportFormUseCase
import com.dororong.rodi.core.domain.usecase.course.ReportCourseUseCase
import com.dororong.rodi.core.ui.text.takeGraphemes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CourseReportViewModel @Inject constructor(
    private val getReportForm: GetCourseReportFormUseCase,
    private val reportCourse: ReportCourseUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CourseReportUiState())
    val uiState: StateFlow<CourseReportUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun load(courseId: Long) {
        val current = _uiState.value
        if (current.courseId == courseId && (current.form != null || current.isLoading)) return
        loadJob?.cancel()
        _uiState.value = CourseReportUiState(courseId = courseId, isLoading = true)
        loadJob = viewModelScope.launch {
            getReportForm()
                .onSuccess { form -> _uiState.update { it.copy(form = form, isLoading = false) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, loadErrorMessage = error.userMessage("신고 사유를 불러오지 못했어요."))
                    }
                }
        }
    }

    fun retry() {
        val courseId = _uiState.value.courseId ?: return
        _uiState.update { it.copy(courseId = null) }
        load(courseId)
    }

    fun toggle(code: String) = _uiState.update { current ->
        val options = current.form?.options ?: return@update current
        if (options.none { it.code == code }) return@update current
        val selected = if (code in current.selected) current.selected - code else current.selected + code
        current.copy(
            selected = options.map { it.code }.filter { it in selected },
            errorMessage = null,
        )
    }

    fun updateDetail(value: String) = _uiState.update { current ->
        val max = current.form?.options?.firstOrNull { it.requiresTextInput && it.code in current.selected }
            ?.textInputMaxLength ?: return@update current
        current.copy(detail = value.takeGraphemes(max), errorMessage = null)
    }

    fun submit() {
        val current = _uiState.value
        val courseId = current.courseId ?: return
        if (!current.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            reportCourse(courseId, CourseReportSubmission(current.selected, current.submittedDetail))
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = error.userMessage("신고하지 못했어요."))
                    }
                }
        }
    }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    fun close() {
        loadJob?.cancel()
        _uiState.value = CourseReportUiState()
    }
}
