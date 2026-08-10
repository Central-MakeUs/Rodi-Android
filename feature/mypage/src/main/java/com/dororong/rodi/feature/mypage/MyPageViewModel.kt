package com.dororong.rodi.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.onboarding.recommendations
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.member.GetPracticeRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dororong.rodi.feature.mypage.practicerecords.PracticeRecord
import kotlin.math.roundToInt

data class MyPageUiState(
    val profile: MyPageProfile = MyPageProfile(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val practiceRecords: List<PracticeRecord> = emptyList(),
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val getMyPage: GetMyPageUseCase,
    private val getPracticeRecords: GetPracticeRecordsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val profileDeferred = async { getMyPage() }
                val recordsDeferred = async { getPracticeRecords(size = 4) }
                val records = recordsDeferred.await().getOrNull()?.items.orEmpty().map { it.toFeatureModel() }
                profileDeferred.await()
                    .onSuccess { page ->
                        _uiState.value = MyPageUiState(
                            profile = page.toUiProfile(),
                            practiceRecords = records,
                            isLoading = false,
                        )
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                practiceRecords = records,
                                errorMessage = error.message ?: "마이페이지를 불러오지 못했어요.",
                            )
                        }
                    }
            }
        }
    }
}

private fun MyPage.toUiProfile() = MyPageProfile(
    nickname = nickname,
    level = level,
    practiceTypes = level.recommendations,
    drivingGoal = drivingGoal.orEmpty(),
    savedPlaceCount = savedPlaceCount,
    progress = levelProgress.nextLevelKm?.let { nextLevelKm ->
        ((levelProgress.totalDistanceKm - levelProgress.currentLevelStartKm) /
            (nextLevelKm - levelProgress.currentLevelStartKm)).toFloat().coerceIn(0f, 1f)
    } ?: 1f,
    distanceLabel = "${levelProgress.totalDistanceKm.roundToInt()}km",
)

private fun com.dororong.rodi.core.domain.model.member.PracticeRecordItem.toFeatureModel() = PracticeRecord(
    practiceId = practiceId,
    placeId = placeId,
    placeName = placeName,
    practiceTypes = practiceTypes,
    visitCount = visitCount,
    visitedAt = visitedAt,
    isVerified = isVerified,
    hasReview = hasReview,
)
