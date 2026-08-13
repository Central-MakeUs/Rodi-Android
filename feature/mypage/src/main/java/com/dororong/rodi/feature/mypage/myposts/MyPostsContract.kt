package com.dororong.rodi.feature.mypage.myposts

import com.dororong.rodi.core.domain.model.review.Review

data class MyPost(
    val placeId: Long,
    val placeName: String,
    val review: Review,
)

data class MyPostsUiState(
    val posts: List<MyPost> = emptyList(),
    val hasPracticeRecords: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)
