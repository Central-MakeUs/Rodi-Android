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
}
