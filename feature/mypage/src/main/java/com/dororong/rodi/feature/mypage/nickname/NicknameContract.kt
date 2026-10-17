package com.dororong.rodi.feature.mypage.nickname

import com.dororong.rodi.core.domain.model.member.NicknameRejection

data class NicknameUiState(
    val initialNickname: String = "",
    val nickname: String = "",
    val validatedNickname: String? = null,
    val normalizedNickname: String? = null,
    val rejection: NicknameRejection? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isChecking: Boolean = false,
    val isSaving: Boolean = false,
    val saveSucceeded: Boolean = false,
) {
    val canSave: Boolean
        get() = validatedNickname == nickname && normalizedNickname != null && rejection == null &&
            normalizedNickname != initialNickname && !isLoading && !loadFailed && !isChecking && !isSaving && !saveSucceeded
}

sealed interface NicknameEffect {
    data object CheckFailed : NicknameEffect
    data object SaveFailed : NicknameEffect
}
