package com.dororong.rodi.feature.home.detail.reviewactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.ui.text.takeGraphemes
import com.dororong.rodi.core.domain.model.review.ReportFormOption
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.usecase.member.BlockMemberUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReportFormUseCase
import com.dororong.rodi.core.domain.usecase.review.ReportReviewUseCase
import com.dororong.rodi.core.domain.usecase.review.DeleteReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewActionsViewModel @Inject constructor(
    private val getReportForm: GetReportFormUseCase,
    private val reportReview: ReportReviewUseCase,
    private val blockMemberUseCase: BlockMemberUseCase,
    private val deleteReviewUseCase: DeleteReviewUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewActionsUiState())
    val state: StateFlow<ReviewActionsUiState> = _state.asStateFlow()
    private val _effect = Channel<ReviewActionsEffect>(Channel.BUFFERED)
    val effect: Flow<ReviewActionsEffect> = _effect.receiveAsFlow()

    fun loadReportForm(reviewId: Long) {
        val current = _state.value
        if (current.reportReviewId == reviewId && current.reportForm != null) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    reportReviewId = reviewId,
                    reportForm = null,
                    selectedOptionCode = null,
                    reportDetail = "",
                    isReportFormLoading = true,
                    isReportSubmitting = false,
                    isReportSubmitted = false,
                    reportErrorMessage = null,
                )
            }
            getReportForm()
                .onSuccess { form ->
                    _state.update { it.copy(reportForm = form, isReportFormLoading = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isReportFormLoading = false,
                            reportErrorMessage = error.userMessage("신고 사유를 불러오지 못했어요."),
                        )
                    }
                }
        }
    }

    fun selectReportOption(option: ReportFormOption) {
        _state.update { current ->
            current.copy(
                selectedOptionCode = option.code,
                reportDetail = if (option.requiresTextInput) {
                    option.textInputMaxLength?.let(current.reportDetail::takeGraphemes) ?: current.reportDetail
                } else {
                    ""
                },
                reportErrorMessage = null,
            )
        }
    }

    fun updateReportDetail(detail: String) {
        val maxLength = _state.value.selectedOption()?.textInputMaxLength
        _state.update {
            it.copy(
                reportDetail = maxLength?.let(detail::takeGraphemes) ?: detail,
                reportErrorMessage = null,
            )
        }
    }

    fun submitReport() {
        val current = _state.value
        val reviewId = current.reportReviewId ?: return
        val option = current.selectedOption() ?: return
        if (current.isReportSubmitting || !current.isSubmittable(option)) return

        viewModelScope.launch {
            _state.update { it.copy(isReportSubmitting = true, reportErrorMessage = null) }
            reportReview(
                reviewId = reviewId,
                submission = ReportSubmission(
                    reason = option.code,
                    detail = current.reportDetail.takeIf { option.requiresTextInput },
                    detailConsistent = true,
                ),
            ).onSuccess {
                _state.update { it.copy(isReportSubmitting = false, isReportSubmitted = true) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isReportSubmitting = false,
                        reportErrorMessage = error.userMessage("신고하지 못했어요."),
                    )
                }
            }
        }
    }

    fun blockMember(memberId: Long) {
        if (_state.value.isBlocking) return

        viewModelScope.launch {
            _state.update { it.copy(isBlocking = true) }
            blockMemberUseCase(memberId)
                .onSuccess {
                    _state.update { it.copy(isBlocking = false) }
                    _effect.send(ReviewActionsEffect.Blocked(memberId))
                }
                .onFailure { error ->
                    _state.update { it.copy(isBlocking = false) }
                    _effect.send(ReviewActionsEffect.BlockFailed(error.userMessage("차단하지 못했어요.")))
                }
        }
    }

    fun closeReport() {
        _state.update {
            it.copy(
                reportReviewId = null,
                reportForm = null,
                selectedOptionCode = null,
                reportDetail = "",
                isReportFormLoading = false,
                isReportSubmitting = false,
                isReportSubmitted = false,
                reportErrorMessage = null,
            )
        }
    }

    fun consumeReportError() {
        _state.update { it.copy(reportErrorMessage = null) }
    }

    fun deleteReview(reviewId: Long) {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            deleteReviewUseCase(reviewId).onSuccess {
                _state.update { it.copy(isDeleting = false) }
                _effect.send(ReviewActionsEffect.Deleted(reviewId))
            }.onFailure { error ->
                _state.update { it.copy(isDeleting = false) }
                _effect.send(ReviewActionsEffect.DeleteFailed(error.userMessage("후기를 삭제하지 못했어요.")))
            }
        }
    }

    private fun ReviewActionsUiState.selectedOption(): ReportFormOption? =
        reportForm?.options?.firstOrNull { it.code == selectedOptionCode }

    private fun ReviewActionsUiState.isSubmittable(option: ReportFormOption): Boolean =
        !option.requiresTextInput || reportDetail.isNotBlank()
}
