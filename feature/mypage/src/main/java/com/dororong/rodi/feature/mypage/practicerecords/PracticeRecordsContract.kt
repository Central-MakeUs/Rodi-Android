package com.dororong.rodi.feature.mypage.practicerecords

data class PracticeRecordsUiState(
    val records: List<PracticeRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val initialError: String? = null,
    val nextPageError: String? = null,
    val hasNextPage: Boolean = false,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)
