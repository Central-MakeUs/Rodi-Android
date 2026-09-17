package com.dororong.rodi.feature.mypage.savedcourses

import com.dororong.rodi.core.domain.model.place.PlaceSummary

data class SavedCoursesUiState(
    val places: List<PlaceSummary> = emptyList(),
    val totalCount: Long? = null,
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val isLoading: Boolean = true,
    val isNextPageLoading: Boolean = false,
    val initialError: String? = null,
    val nextPageError: String? = null,
)
