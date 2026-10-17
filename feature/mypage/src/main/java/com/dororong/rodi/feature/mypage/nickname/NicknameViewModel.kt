package com.dororong.rodi.feature.mypage.nickname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.member.NicknameRejectedException
import com.dororong.rodi.core.domain.usecase.member.CheckNicknameAvailabilityUseCase
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.member.UpdateNicknameUseCase
import com.dororong.rodi.core.ui.text.takeGraphemes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NicknameViewModel @Inject constructor(
    private val getMyPage: GetMyPageUseCase,
    private val checkAvailability: CheckNicknameAvailabilityUseCase,
    private val updateNickname: UpdateNicknameUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NicknameUiState())
    val uiState = _uiState.asStateFlow()
    private val _effect = Channel<NicknameEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var checkJob: Job? = null
    private var revision = 0L

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            getMyPage().onSuccess { page ->
                _uiState.value = NicknameUiState(
                    initialNickname = page.nickname,
                    nickname = page.nickname,
                    isLoading = false,
                )
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun changeNickname(nickname: String) {
        val current = _uiState.value
        if (current.isLoading || current.loadFailed || current.isSaving || current.saveSucceeded) return
        val limited = nickname.takeGraphemes(10)
        if (limited == current.nickname) return
        revision++
        checkJob?.cancel()
        _uiState.update {
            it.copy(nickname = limited, validatedNickname = null, normalizedNickname = null, rejection = null, isChecking = false)
        }
    }

    fun validate() {
        val current = _uiState.value
        if (current.isLoading || current.loadFailed || current.isSaving || current.saveSucceeded || current.isChecking ||
            current.validatedNickname == current.nickname || current.nickname == current.initialNickname
        ) return
        val requestRevision = revision
        _uiState.update { it.copy(isChecking = true) }
        checkJob = viewModelScope.launch {
            val result = checkAvailability(current.nickname)
            if (requestRevision != revision) return@launch
            result.onSuccess { availability ->
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        validatedNickname = current.nickname,
                        normalizedNickname = availability.nickname.takeIf { availability.isAvailable },
                        rejection = availability.rejection,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isChecking = false) }
                _effect.send(NicknameEffect.CheckFailed)
            }
        }
    }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        val nickname = current.normalizedNickname ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            updateNickname(nickname).onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSucceeded = true) }
            }.onFailure { error ->
                _uiState.update {
                    if (error is NicknameRejectedException) {
                        it.copy(isSaving = false, rejection = error.rejection, normalizedNickname = null)
                    } else {
                        it.copy(isSaving = false)
                    }
                }
                if (error !is NicknameRejectedException) _effect.send(NicknameEffect.SaveFailed)
            }
        }
    }
}
