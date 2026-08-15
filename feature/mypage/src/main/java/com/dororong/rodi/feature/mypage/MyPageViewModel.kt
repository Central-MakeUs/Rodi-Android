package com.dororong.rodi.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.onboarding.recommendations
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.member.GetPracticeRecordsUseCase
import com.dororong.rodi.core.domain.usecase.member.HardDeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dororong.rodi.feature.mypage.practicerecords.PracticeRecord
import kotlin.math.roundToInt

data class MyPageUiState(
    val profile: MyPageProfile = MyPageProfile(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val practiceRecords: List<PracticeRecord> = emptyList(),
    val practiceRecordsErrorMessage: String? = null,
    val isHardDeleteSubmitting: Boolean = false,
)

sealed interface MyPageEffect {
    data object HardDeleteCompleted : MyPageEffect
    data class ShowError(val message: String) : MyPageEffect
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val getMyPage: GetMyPageUseCase,
    private val getPracticeRecords: GetPracticeRecordsUseCase,
    private val hardDeleteAccount: HardDeleteAccountUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()
    private val _effect = Channel<MyPageEffect>(Channel.BUFFERED)
    val effect: Flow<MyPageEffect> = _effect.receiveAsFlow()
    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val profileDeferred = async { getMyPage() }
                val recordsDeferred = async { getPracticeRecords(size = 4) }
                val recordsResult = recordsDeferred.await()
                val records = recordsResult.getOrNull()?.items.orEmpty()
                    .filter { it.status == PracticeStatus.VISITED }
                    .map { it.toFeatureModel() }
                val recordsErrorMessage = recordsResult.exceptionOrNull()
                    ?.userMessage("연습기록을 불러오지 못했어요.")
                profileDeferred.await()
                    .onSuccess { page ->
                        _uiState.value = MyPageUiState(
                            profile = page.toUiProfile(),
                            practiceRecords = records,
                            isLoading = false,
                            practiceRecordsErrorMessage = recordsErrorMessage,
                        )
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                practiceRecords = records,
                                errorMessage = error.userMessage("마이페이지를 불러오지 못했어요."),
                                practiceRecordsErrorMessage = recordsErrorMessage,
                            )
                        }
                    }
            }
        }
    }

    fun hardDelete() {
        if (_uiState.value.isHardDeleteSubmitting) return
        _uiState.update { it.copy(isHardDeleteSubmitting = true) }
        viewModelScope.launch {
            hardDeleteAccount()
                .onSuccess {
                    _uiState.update { it.copy(isHardDeleteSubmitting = false) }
                    _effect.send(MyPageEffect.HardDeleteCompleted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isHardDeleteSubmitting = false) }
                    _effect.send(
                        MyPageEffect.ShowError(error.userMessage("계정을 삭제하지 못했어요.")),
                    )
                }
        }
    }
}

/**
 * `AuthException`만 사용자에게 보여줄 만한 문구를 갖는다. 나머지(직렬화·파싱 실패 등)의
 * `message`는 JSON 필드명이 그대로 박힌 개발자용 텍스트라 화면에 노출하면 안 된다.
 */
private fun Throwable.userMessage(fallback: String): String =
    (this as? AuthException)?.message?.ifBlank { null } ?: fallback

private fun MyPage.toUiProfile() = MyPageProfile(
    nickname = nickname,
    level = level,
    practiceTypes = level.recommendations,
    drivingGoal = drivingGoal.orEmpty(),
    savedPlaceCount = savedPlaceCount,
    progress = (levelProgress.progressPercent / 100f).coerceIn(0f, 1f),
    distanceLabel = "${levelProgress.totalDistanceKm.roundToInt()}km",
)

// PracticeRecord는 PracticeRecordItem의 typealias라 이 매핑은 항등 복사다. 필드별로 나열해두면
// 도메인 모델에 필드가 추가될 때(status가 그랬다) 여기 반영을 잊어도 컴파일이 통과해 조용히
// 기본값으로 떨어진다 — 실제로 status 누락으로 마이페이지 카드가 계속 "방문 예정"을 보여줬다.
private fun com.dororong.rodi.core.domain.model.member.PracticeRecordItem.toFeatureModel() = PracticeRecord(
    practiceId = practiceId,
    placeId = placeId,
    placeName = placeName,
    practiceTypes = practiceTypes,
    visitCount = visitCount,
    visitedAt = visitedAt,
    isVerified = isVerified,
    hasReview = hasReview,
    status = status,
)
