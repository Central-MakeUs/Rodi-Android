package com.dororong.rodi.feature.mypage

import com.dororong.rodi.feature.mypage.practicerecords.PracticeRecord
import java.time.LocalDate

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
    data object OpenNicknameEditor : MyPageEffect
    data class NicknameChangeRestricted(val changeableOn: LocalDate) : MyPageEffect
}
