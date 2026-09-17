package com.dororong.rodi.feature.settings.blocked

import com.dororong.rodi.core.domain.model.member.BlockedMember as DomainBlockedMember

typealias BlockedMember = DomainBlockedMember

data class BlockedMembersUiState(
    val members: List<BlockedMember> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val initialError: String? = null,
    val nextPageError: String? = null,
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)
