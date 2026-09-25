package com.dororong.rodi.feature.mypage

import com.dororong.rodi.feature.mypage.practicerecords.PracticeRecord

data class MyPageUiState(
    val profile: MyPageProfile = MyPageProfile(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val practiceRecords: List<PracticeRecord> = emptyList(),
    val practiceRecordsErrorMessage: String? = null,
    val isHardDeleteSubmitting: Boolean = false,
)

sealed interface MyPageEffect {
    data class ShowError(val message: String) : MyPageEffect
}
