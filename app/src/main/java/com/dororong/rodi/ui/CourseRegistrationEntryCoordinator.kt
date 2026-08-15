package com.dororong.rodi.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.domain.usecase.course.ClearCourseDraftUseCase
import com.dororong.rodi.core.domain.usecase.course.ObserveCourseDraftUseCase
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.feature.course.registration.CourseRegistrationDialog
import com.dororong.rodi.feature.course.registration.CourseRegistrationIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CourseRegistrationEntryMode {
    Normal,
    ContinueDraft,
    StartFresh,
}

sealed interface CourseRegistrationEntryState {
    data object Loading : CourseRegistrationEntryState

    data class Ready(val draft: CourseDraft?) : CourseRegistrationEntryState
}

internal enum class CourseRegistrationPreflightDecision {
    OpenImmediately,
    ShowResumeDialog,
}

@HiltViewModel
class CourseRegistrationEntryViewModel @Inject constructor(
    private val observeCourseDraft: ObserveCourseDraftUseCase,
    private val clearCourseDraft: ClearCourseDraftUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<CourseRegistrationEntryState>(CourseRegistrationEntryState.Loading)
    val state: StateFlow<CourseRegistrationEntryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeCourseDraft()
                .catch { emit(null) }
                .collect { draft -> _state.value = CourseRegistrationEntryState.Ready(draft) }
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

internal fun CourseDraft?.courseRegistrationPreflightDecision(): CourseRegistrationPreflightDecision =
    if (this?.isMeaningful == true) {
        CourseRegistrationPreflightDecision.ShowResumeDialog
    } else {
        CourseRegistrationPreflightDecision.OpenImmediately
    }

internal fun courseRegistrationClearDraftFailureMessage(error: Throwable): String =
    error.message?.takeIf { it.isNotBlank() }
        ?: "작성 중인 코스를 삭제하지 못했어요. 다시 시도해주세요."

internal fun courseRegistrationIntentForEntry(
    entryMode: CourseRegistrationEntryMode,
    dialog: CourseRegistrationDialog?,
): CourseRegistrationIntent? {
    if (dialog != CourseRegistrationDialog.ResumeDraft) return null
    return when (entryMode) {
        CourseRegistrationEntryMode.StartFresh -> CourseRegistrationIntent.DiscardDraft
        CourseRegistrationEntryMode.ContinueDraft,
        CourseRegistrationEntryMode.Normal,
        -> CourseRegistrationIntent.ContinueDraft
    }
}

@Composable
internal fun CourseRegistrationResumeDialog(
    onStartFresh: () -> Unit,
    onContinue: () -> Unit,
) {
    RodiAlertDialog(
        confirmText = "이어서 하기",
        onConfirm = onContinue,
        onDismissRequest = {},
        title = "작성 중인 코스가 있어요",
        description = "이전에 입력한 내용부터 등록할 수 있어요. 코스 등록을 이어서 할까요?",
        dismissText = "새로 하기",
        onDismiss = onStartFresh,
        dismissible = false,
    )
}
