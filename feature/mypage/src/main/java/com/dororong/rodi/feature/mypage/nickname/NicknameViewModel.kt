package com.dororong.rodi.feature.mypage.nickname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.member.NicknameRejectedException
import com.dororong.rodi.core.domain.model.member.NicknameRejection
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
        if (_uiState.value.isSaving || _uiState.value.saveSucceeded) return
        val requestRevision = ++revision
        checkJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false, isChecking = false) }
        viewModelScope.launch {
            val result = getMyPage()
            if (requestRevision != revision) return@launch
            result.onSuccess { page ->
                _uiState.value = NicknameUiState(
                    initialNickname = page.nickname,
                    nickname = page.nickname,
                    isLoading = false,
                    nicknameChangeableAt = page.nicknameChangeableAt,
                    isChangeRestricted = page.nicknameChangeableAt != null,
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
        if (current.isLoading || current.loadFailed ||
            current.isSaving || current.saveSucceeded || current.isChecking ||
            current.validatedNickname == current.nickname ||
            current.nickname == current.initialNickname
        ) return
        val requestRevision = revision
        _uiState.update { it.copy(isChecking = true) }
        checkJob = viewModelScope.launch {
            val result = checkAvailability(current.nickname)
            if (requestRevision != revision) return@launch
            result.onSuccess { availability ->
                _uiState.update { state ->
                    val restricted = availability.rejection == NicknameRejection.CHANGE_RESTRICTED
                    val restrictionCleared = !restricted &&
                        !availability.nickname.equals(state.initialNickname, ignoreCase = true)
                    state.copy(
                        isChecking = false,
                        validatedNickname = current.nickname.takeUnless { restricted },
                        normalizedNickname = availability.nickname.takeIf { availability.isAvailable },
                        rejection = availability.rejection,
                        nicknameChangeableAt = if (restrictionCleared) null else availability.nicknameChangeableAt ?: state.nicknameChangeableAt,
                        isChangeRestricted = !restrictionCleared &&
                            (state.isChangeRestricted || restricted || availability.nicknameChangeableAt != null),
                    )
                }
            }.onFailure { error ->
                if (error is NicknameRejectedException) {
                    applyRejection(error, current.nickname)
                } else {
                    _uiState.update { it.copy(isChecking = false) }
                    _effect.send(NicknameEffect.CheckFailed)
                }
            }
        }
    }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        val nickname = current.normalizedNickname ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            updateNickname(nickname).onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        initialNickname = saved.nickname,
                        nickname = saved.nickname,
                        validatedNickname = saved.nickname,
                        normalizedNickname = saved.nickname,
                        nicknameChangeableAt = saved.nicknameChangeableAt,
                        isChangeRestricted = saved.nicknameChangeableAt != null,
                        rejection = null,
                        isSaving = false,
                        saveSucceeded = true,
                    )
                }
            }.onFailure { error ->
                if (error is NicknameRejectedException) {
                    applyRejection(error, current.nickname)
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.send(NicknameEffect.SaveFailed)
                }
            }
        }
    }

    private fun applyRejection(error: NicknameRejectedException, nickname: String) {
        _uiState.update {
            val restricted = error.rejection == NicknameRejection.CHANGE_RESTRICTED
            val restrictionCleared = !restricted && !nickname.trim().equals(it.initialNickname, ignoreCase = true)
            it.copy(
                isChecking = false,
                isSaving = false,
                validatedNickname = nickname.takeUnless { restricted },
                normalizedNickname = null,
                rejection = error.rejection,
                nicknameChangeableAt = if (restrictionCleared) null else error.nicknameChangeableAt ?: it.nicknameChangeableAt,
                isChangeRestricted = !restrictionCleared &&
                    (it.isChangeRestricted || restricted || error.nicknameChangeableAt != null),
            )
        }
    }
}
