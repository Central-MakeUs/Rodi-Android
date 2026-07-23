package com.dororong.rodi.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.onboarding.recommendations
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageUiState(
    val profile: MyPageProfile = MyPageProfile(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val getMyPage: GetMyPageUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getMyPage()
                .onSuccess { page ->
                    _uiState.value = MyPageUiState(
                        profile = page.toUiProfile(),
                        isLoading = false,
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "마이페이지를 불러오지 못했어요.",
                        )
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
)
