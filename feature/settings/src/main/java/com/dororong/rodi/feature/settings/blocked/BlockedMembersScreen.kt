package com.dororong.rodi.feature.settings.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.settings.SettingsTopBar

@Composable
fun BlockedMembersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlockedMembersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BlockedMembersContent(state, onBack, viewModel::unblock, viewModel::loadNextPage, modifier)
}

@Composable
private fun BlockedMembersContent(
    state: BlockedMembersUiState,
    onBack: () -> Unit,
    onUnblock: (BlockedMember) -> Unit,
    onLoadNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
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
                            Surface(
                                onClick = { onUnblock(member) },
                                color = RodiTheme.colors.pointRed.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(100),
                                modifier = Modifier.clip(RoundedCornerShape(100)),
                            ) {
                                Text("차단해제", style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.pointRed, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private val PreviewBlockedMembers = listOf(
    BlockedMember(1, "천천히 달리는 초보운전자", java.time.Instant.EPOCH),
    BlockedMember(2, "도로 위의 로디", java.time.Instant.EPOCH),
)

@Preview(name = "차단목록 항목 있음", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun BlockedMembersListPreview() = RodiTheme {
    BlockedMembersContent(BlockedMembersUiState(members = PreviewBlockedMembers), {}, {}, {})
}

@Preview(name = "차단목록 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun BlockedMembersEmptyPreview() = RodiTheme {
    BlockedMembersContent(BlockedMembersUiState(), {}, {}, {})
}
