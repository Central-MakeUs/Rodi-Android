package com.dororong.rodi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.usecase.course.ClearCourseDraftUseCase
import com.dororong.rodi.core.domain.usecase.course.ObserveCourseDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseRegistrationEntryViewModel @Inject constructor(
    private val observeCourseDraft: ObserveCourseDraftUseCase,
    private val clearCourseDraft: ClearCourseDraftUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CourseRegistrationEntryUiState>(CourseRegistrationEntryUiState.Loading)
    val uiState: StateFlow<CourseRegistrationEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCourseDraft()
                .catch { emit(null) }
                .collect { draft -> _uiState.value = CourseRegistrationEntryUiState.Ready(draft) }
        }
    }

    suspend fun clearDraft(): Result<Unit> = try {
        clearCourseDraft()
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
