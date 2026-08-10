package com.dororong.rodi.feature.settings.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.member.BlockedMember as DomainBlockedMember
import com.dororong.rodi.core.domain.usecase.member.GetBlockedMembersUseCase
import com.dororong.rodi.core.domain.usecase.member.UnblockMemberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

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

@HiltViewModel
class BlockedMembersViewModel @Inject constructor(
    private val unblockMember: UnblockMemberUseCase,
    private val getBlockedMembers: GetBlockedMembersUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BlockedMembersUiState())
    val uiState: StateFlow<BlockedMembersUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private val excludedMemberIds = mutableSetOf<Long>()

    init { loadInitial() }

    fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = BlockedMembersUiState(isLoading = true)
            getBlockedMembers(cursor = null, size = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.value = BlockedMembersUiState(
                        members = page.items,
                        isLoading = false,
                        nextCursor = page.nextCursor,
                        hasNext = page.hasNext,
                    )
                }
                .onFailure { error ->
                    _uiState.value = BlockedMembersUiState(
                        isLoading = false,
                        initialError = error.message ?: "차단목록을 불러오지 못했어요.",
                    )
                }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        val cursor = current.nextCursor ?: return
        if (!current.hasNext || current.isLoadingMore || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, nextPageError = null) }
            getBlockedMembers(cursor = cursor, size = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.update { latest ->
                        val visibleItems = page.items.filterNot { it.memberId in excludedMemberIds }
                        latest.copy(
                            members = (latest.members + visibleItems).distinctBy(BlockedMember::memberId),
                            nextCursor = page.nextCursor,
                            hasNext = page.hasNext,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(isLoadingMore = false, nextPageError = error.message ?: "다음 차단목록을 불러오지 못했어요.") } }
        }
    }

    fun replaceMembers(members: List<BlockedMember>) {
        loadJob?.cancel()
        _uiState.value = BlockedMembersUiState(members = members.distinctBy { it.memberId })
    }

    fun unblock(member: BlockedMember) {
        excludedMemberIds += member.memberId
        viewModelScope.launch {
            unblockMember(member.memberId)
                .onSuccess { _uiState.update { state -> state.copy(members = state.members.filterNot { it.memberId == member.memberId }) } }
                .onFailure { error ->
                    excludedMemberIds -= member.memberId
                    _uiState.update { it.copy(errorMessage = error.message ?: "차단을 해제하지 못했어요.") }
                }
        }
    }
}

private const val PAGE_SIZE = 20
