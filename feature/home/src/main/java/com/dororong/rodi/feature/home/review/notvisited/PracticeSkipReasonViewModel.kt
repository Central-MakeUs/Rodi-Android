package com.dororong.rodi.feature.home.review.notvisited

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.practice.PracticeException
import com.dororong.rodi.core.domain.model.practice.SkipReasonOption
import com.dororong.rodi.core.domain.usecase.practice.GetSkipReasonFormUseCase
import com.dororong.rodi.core.domain.usecase.practice.SubmitSkipReasonUseCase
import com.dororong.rodi.core.ui.text.takeGraphemes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PracticeSkipReasonViewModel @Inject constructor(
    private val getSkipReasonForm: GetSkipReasonFormUseCase,
    private val submitSkipReason: SubmitSkipReasonUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PracticeSkipReasonUiState())
    val state: StateFlow<PracticeSkipReasonUiState> = _state.asStateFlow()

    fun load(practiceId: Long) {
        val current = _state.value
        if (current.practiceId == practiceId && (current.form != null || current.isLoading)) return
        viewModelScope.launch {
            _state.value = PracticeSkipReasonUiState(practiceId = practiceId, isLoading = true)
            getSkipReasonForm()
                .onSuccess { form -> _state.update { it.copy(form = form, isLoading = false) } }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.skipReasonErrorMessage()) }
                }
        }
    }

    fun selectOption(option: SkipReasonOption) {
        _state.update { current ->
            current.copy(
                selectedOptionCode = option.code,
                detail = if (option.requiresTextInput) {
                    option.textInputMaxLength?.let(current.detail::takeGraphemes) ?: current.detail
                } else {
                    ""
                },
                errorMessage = null,
            )
        }
    }

    fun updateDetail(detail: String) {
        val maxLength = _state.value.selectedOption()?.textInputMaxLength
        _state.update { it.copy(detail = maxLength?.let(detail::takeGraphemes) ?: detail, errorMessage = null) }
    }

    fun submit() {
        val current = _state.value
        val practiceId = current.practiceId ?: return
        val option = current.selectedOption() ?: return
        if (current.isSubmitting || current.isLoading || !current.isSubmittable(option)) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            submitSkipReason(
                practiceId = practiceId,
                reason = option.code,
                detail = current.detail.takeIf { option.requiresTextInput },
            ).onSuccess { _state.update { it.copy(isSubmitting = false, isSubmitted = true) } }
                .onFailure { error ->
                    _state.update { it.copy(isSubmitting = false, errorMessage = error.skipReasonErrorMessage()) }
                }
        }
    }

    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    private fun PracticeSkipReasonUiState.selectedOption(): SkipReasonOption? =
        form?.options?.firstOrNull { it.code == selectedOptionCode }

    private fun PracticeSkipReasonUiState.isSubmittable(option: SkipReasonOption): Boolean =
        !option.requiresTextInput || detail.isNotBlank()
}

private fun Throwable.skipReasonErrorMessage(): String = when (this) {
    is PracticeException.Network -> "네트워크 연결을 확인해주세요."
    else -> "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요."
}
