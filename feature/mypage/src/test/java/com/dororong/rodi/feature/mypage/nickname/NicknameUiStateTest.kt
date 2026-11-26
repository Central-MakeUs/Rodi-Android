package com.dororong.rodi.feature.mypage.nickname

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NicknameUiStateTest {
    @Test
    fun `검증을 통과한 닉네임도 변경 제한 중이면 저장하지 못하고 해제 후 저장할 수 있다`() {
        val state = NicknameUiState(
            initialNickname = "운전하는_고양이",
            nickname = "산뜻한_기린",
            validatedNickname = "산뜻한_기린",
            normalizedNickname = "산뜻한_기린",
            isLoading = false,
        )
        assertTrue(state.canSave)

        val restricted = state.copy(isChangeRestricted = true)

        assertFalse(restricted.canSave)
        assertTrue(restricted.copy(isChangeRestricted = false).canSave)
    }
    @Test
    fun `제한 중에는 서버가 검증한 대소문자 변경만 저장한다`() {
        val state = NicknameUiState(initialNickname = "Rodi_user", nickname = "rodi_user",
            validatedNickname = "rodi_user", normalizedNickname = "rodi_user", isLoading = false, isChangeRestricted = true)
        assertTrue(state.canSave)
        assertFalse(state.copy(validatedNickname = null).canSave)
        assertFalse(state.copy(nickname = "Different", validatedNickname = "Different", normalizedNickname = "Different").canSave)
        assertFalse(state.copy(nickname = "Rodi_user", validatedNickname = "Rodi_user", normalizedNickname = "Rodi_user").canSave)
    }

}
