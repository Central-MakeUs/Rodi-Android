package com.dororong.rodi.feature.settings.blocked

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.dororong.rodi.core.ui.R as CoreUiR
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.components.RodiTextEmptyState
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.feature.settings.SettingsTopBar

@Composable
fun BlockedMembersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlockedMembersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BlockedMembersContent(state, onBack, viewModel::unblock, viewModel::loadInitial, viewModel::loadNextPage, modifier)
}

@Composable
private fun BlockedMembersContent(
    state: BlockedMembersUiState,
    onBack: () -> Unit,
    onUnblock: (BlockedMember) -> Unit,
    onLoadInitial: () -> Unit,
    onLoadNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            SettingsTopBar(title = "차단목록", onBack = onBack)
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("불러오는 중…", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
                }
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(listState, state.members.size, state.hasNext) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .map { index -> index != null && index >= state.members.lastIndex - 2 }
                        .distinctUntilChanged()
                        .collect { shouldLoad -> if (shouldLoad) onLoadNext() }
                }
                if (state.members.isEmpty() && state.initialError != null) {
                    BlockedMembersError(state.initialError, onLoadInitial)
                } else if (state.members.isEmpty()) {
                    BlockedMembersEmpty()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(state.members, key = BlockedMember::memberId) { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(member.nickname.orEmpty(), style = RodiTheme.typography.body1Medium, color = RodiTheme.colors.black, modifier = Modifier.weight(1f))
                                UnblockButton(onClick = { onUnblock(member) })
                            }
                        }
                        if (state.nextPageError != null) {
                            item(key = "next-page-error") {
                                BlockedMembersNextPageError(state.nextPageError, onLoadNext)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnblockButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(RodiTheme.colors.infoBgPink, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(CoreUiR.drawable.ic_user_round_20),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "차단해제",
            style = RodiTheme.typography.caption2Medium,
            color = RodiTheme.colors.infoCancel,
        )
    }
}

@Composable
private fun BlockedMembersEmpty() {
    RodiTextEmptyState(
        modifier = Modifier.fillMaxSize(),
        title = "차단한 사용자가 없습니다.",
    )
}

@Composable
private fun BlockedMembersError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray700)
            RodiButton(
                text = "다시 시도",
                onClick = onRetry,
                fillMaxWidth = false,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun BlockedMembersNextPageError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray600)
        RodiButton(
            text = "다시 시도",
            onClick = onRetry,
            fillMaxWidth = false,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private val PreviewBlockedMembers = listOf(
    BlockedMember(1, "천천히 달리는 초보운전자", java.time.Instant.EPOCH),
    BlockedMember(2, "도로 위의 로디", java.time.Instant.EPOCH),
)

@Preview(name = "차단목록 항목 있음", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun BlockedMembersListPreview() = RodiTheme {
    BlockedMembersContent(BlockedMembersUiState(members = PreviewBlockedMembers), {}, {}, {}, {})
}

@Preview(name = "차단목록 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun BlockedMembersEmptyPreview() = RodiTheme {
    BlockedMembersContent(BlockedMembersUiState(), {}, {}, {}, {})
}
