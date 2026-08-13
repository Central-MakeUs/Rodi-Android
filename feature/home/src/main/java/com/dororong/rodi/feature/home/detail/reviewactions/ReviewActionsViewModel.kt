package com.dororong.rodi.feature.home.detail.reviewactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.takeGraphemes
import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportFormOption
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.usecase.member.BlockMemberUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReportFormUseCase
import com.dororong.rodi.core.domain.usecase.review.ReportReviewUseCase
import com.dororong.rodi.core.domain.usecase.review.DeleteReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewActionsUiState(
    val reportReviewId: Long? = null,
    val reportForm: ReportForm? = null,
    val selectedOptionCode: String? = null,
    val reportDetail: String = "",
    val isReportFormLoading: Boolean = false,
    val isReportSubmitting: Boolean = false,
    val isReportSubmitted: Boolean = false,
    val reportErrorMessage: String? = null,
    val isBlocking: Boolean = false,
    val blockedMemberId: Long? = null,
    val blockErrorMessage: String? = null,
    val isDeleting: Boolean = false,
    val deletedReviewId: Long? = null,
    val deleteErrorMessage: String? = null,
)

@HiltViewModel
class ReviewActionsViewModel @Inject constructor(
    private val getReportForm: GetReportFormUseCase,
    private val reportReview: ReportReviewUseCase,
    private val blockMemberUseCase: BlockMemberUseCase,
    private val deleteReviewUseCase: DeleteReviewUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewActionsUiState())
    val state: StateFlow<ReviewActionsUiState> = _state.asStateFlow()

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
                            reportErrorMessage = error.message,
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
                        reportErrorMessage = error.message,
                    )
                }
            }
        }
    }

    fun blockMember(memberId: Long) {
        if (_state.value.isBlocking) return

        viewModelScope.launch {
            _state.update { it.copy(isBlocking = true, blockErrorMessage = null) }
            blockMemberUseCase(memberId)
                .onSuccess {
                    _state.update { it.copy(isBlocking = false, blockedMemberId = memberId) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isBlocking = false,
                            blockErrorMessage = error.message,
                        )
                    }
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

    fun consumeBlockResult() {
        _state.update { it.copy(blockedMemberId = null, blockErrorMessage = null) }
    }

    fun consumeReportError() {
        _state.update { it.copy(reportErrorMessage = null) }
    }

    fun deleteReview(reviewId: Long) {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, deleteErrorMessage = null) }
            deleteReviewUseCase(reviewId).onSuccess {
                _state.update { it.copy(isDeleting = false, deletedReviewId = reviewId) }
            }.onFailure { error ->
                _state.update { it.copy(isDeleting = false, deleteErrorMessage = error.message) }
            }
        }
    }

    fun consumeDeleteResult() {
        _state.update { it.copy(deletedReviewId = null, deleteErrorMessage = null) }
    }

    private fun ReviewActionsUiState.selectedOption(): ReportFormOption? =
        reportForm?.options?.firstOrNull { it.code == selectedOptionCode }

    private fun ReviewActionsUiState.isSubmittable(option: ReportFormOption): Boolean =
        !option.requiresTextInput || reportDetail.isNotBlank()
}
