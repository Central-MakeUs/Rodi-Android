package com.dororong.rodi.feature.mypage.practicerecords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun PracticeRecordsScreen(
    onBack: () -> Unit,
    onWriteReviewClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeRecordsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val deleteTarget = state.records.firstOrNull { it.practiceId == deleteTargetId }
    LaunchedEffect(state.deletingPracticeId, state.deleteErrorMessage, state.records) {
        val targetId = deleteTargetId ?: return@LaunchedEffect
        if (state.deleteErrorMessage != null ||
            (state.deletingPracticeId == null && state.records.none { it.practiceId == targetId })
        ) {
            deleteTargetId = null
        }
    }
    PracticeRecordsContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onLoadNext = viewModel::loadNextPage,
        onWriteReviewClick = onWriteReviewClick,
        onDeleteClick = { deleteTargetId = it.practiceId },
        modifier = modifier,
    )
    deleteTarget?.let { record ->
        RodiAlertDialog(
            title = "연습기록을 삭제할까요?",
            description = "삭제한 연습기록은 되돌릴 수 없어요.",
            confirmText = if (state.deletingPracticeId == record.practiceId) "삭제 중" else "삭제",
            dismissText = "취소",
            enabled = state.deletingPracticeId == null,
            onConfirm = {
                viewModel.delete(record)
            },
            onDismiss = { if (state.deletingPracticeId == null) deleteTargetId = null },
            onDismissRequest = { if (state.deletingPracticeId == null) deleteTargetId = null },
        )
    }
    state.deleteErrorMessage?.let { message ->
        RodiAlertDialog(
            title = "연습기록을 삭제하지 못했어요",
            description = message,
            confirmText = "확인",
            onConfirm = viewModel::consumeDeleteError,
            onDismissRequest = viewModel::consumeDeleteError,
        )
    }
}

@Composable
private fun PracticeRecordsContent(
    state: PracticeRecordsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadNext: () -> Unit,
    onWriteReviewClick: (Long, String) -> Unit,
    onDeleteClick: (PracticeRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            SubPageTopBar(title = "연습기록", onBack = onBack)
            val listState = rememberLazyListState()
            LaunchedEffect(listState, state.records.size, state.hasNextPage) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .map { index -> index != null && index >= state.records.lastIndex - 2 }
                    .distinctUntilChanged()
                    .collect { shouldLoad -> if (shouldLoad) onLoadNext() }
            }
            when {
                state.isLoading -> PracticeRecordsLoading()
                state.initialError != null -> PracticeRecordsError(state.initialError, onRetry)
                state.records.isEmpty() -> PracticeRecordsEmpty()
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(state.records, key = PracticeRecord::practiceId) { record ->
                        PracticeRecordListItem(record, onWriteReviewClick, onDeleteClick)
                    }
                    if (state.isLoadingMore || state.nextPageError != null) {
                        item(key = "next-page-status") {
                            PracticeRecordsNextPageFooter(
                                isLoading = state.isLoadingMore,
                                errorMessage = state.nextPageError,
                                onRetry = onLoadNext,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubPageTopBar(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_chevron_left),
                contentDescription = "뒤로",
                tint = RodiTheme.colors.black,
            )
        }
        Text(
            text = title,
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun PracticeRecordListItem(
    record: PracticeRecord,
    onWriteReviewClick: (Long, String) -> Unit,
    onDeleteClick: (PracticeRecord) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = record.placeName,
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${record.visitCount}회차",
                    style = RodiTheme.typography.caption1SemiBold,
                    color = RodiTheme.colors.primary600,
                )
                IconButton(onClick = { onDeleteClick(record) }) {
                    Icon(
                        painter = painterResource(com.dororong.rodi.feature.mypage.R.drawable.ic_more_horizontal),
                        contentDescription = "연습기록 관리",
                        tint = RodiTheme.colors.gray500,
                    )
                }
            }
        }
        Text(
            text = record.recordStatusLabel(),
            style = RodiTheme.typography.caption1Medium,
            color = RodiTheme.colors.gray600,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            record.practiceTypes.forEach { type ->
                Text(
                    text = type.label,
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray600,
                    modifier = Modifier
                        .background(RodiTheme.colors.gray200, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        OutlinedButton(
            onClick = { onWriteReviewClick(record.placeId, record.placeName) },
            enabled = !record.hasReview,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(32.dp),
            shape = RoundedCornerShape(8.dp),
            border = if (record.hasReview) null else BorderStroke(1.dp, RodiTheme.colors.primary600),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = RodiTheme.colors.primary600,
                disabledContainerColor = RodiTheme.colors.gray300,
                disabledContentColor = RodiTheme.colors.gray500,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text(if (record.hasReview) "작성 완료" else "후기 작성", style = RodiTheme.typography.body3Medium) }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = RodiTheme.colors.gray100)
    }
}

@Composable
private fun PracticeRecordsEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("아직 연습기록이 없어요!", style = RodiTheme.typography.body2SemiBold, color = RodiTheme.colors.gray600)
            Text("가까운 연습 장소부터 천천히 시작해볼까요?", style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray600, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun PracticeRecordsLoading() {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            RodiSkeleton(Modifier.fillMaxWidth().height(100.dp))
        }
    }
}

@Composable
private fun PracticeRecordsError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray700)
            Spacer(Modifier.height(16.dp))
            RodiButton(text = "다시 시도", onClick = onRetry, fillMaxWidth = false)
        }
    }
}

@Composable
private fun PracticeRecordsNextPageFooter(
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            RodiSkeleton(Modifier.width(96.dp).height(20.dp))
        } else if (errorMessage != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorMessage, style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray600)
                RodiButton(
                    text = "다시 시도",
                    onClick = onRetry,
                    fillMaxWidth = false,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private val PracticeRecordDateFormatter = DateTimeFormatter.ofPattern("yy.MM.dd").withZone(ZoneId.systemDefault())

// visitedAt은 방문(VISITED)에서만 채워진다. 미방문 사유를 제출해도 NOT_VISITED로 남을 뿐
// visitedAt은 비어 있어, visitedAt만으로 분기하면 미방문 처리한 항목이 계속 "방문 예정"으로 보인다.
private fun PracticeRecord.recordStatusLabel(): String = when {
    visitedAt != null -> PracticeRecordDateFormatter.format(visitedAt)
    status == PracticeStatus.NOT_VISITED -> "미방문"
    else -> "방문 예정"
}

private val PreviewPracticeRecords = listOf(
    PracticeRecord(1, 1, "망원한강공원", listOf(PracticeType.ROUNDABOUT), 1, Instant.parse("2026-05-10T00:00:00Z"), true, false),
    PracticeRecord(2, 2, "용산구 교차로", listOf(PracticeType.PARKING), 2, Instant.parse("2026-05-09T00:00:00Z"), true, true),
)

@Preview(name = "연습기록 목록", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PracticeRecordsListPreview() = RodiTheme {
    PracticeRecordsContent(PracticeRecordsUiState(records = PreviewPracticeRecords), {}, {}, {}, { _, _ -> }, {})
}

@Preview(name = "연습기록 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PracticeRecordsEmptyPreview() = RodiTheme {
    PracticeRecordsContent(PracticeRecordsUiState(), {}, {}, {}, { _, _ -> }, {})
}

@Preview(name = "연습기록 로딩", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PracticeRecordsLoadingPreview() = RodiTheme {
    PracticeRecordsContent(PracticeRecordsUiState(isLoading = true), {}, {}, {}, { _, _ -> }, {})
}

@Preview(name = "연습기록 에러", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PracticeRecordsErrorPreview() = RodiTheme {
    PracticeRecordsContent(PracticeRecordsUiState(initialError = "연습기록을 불러오지 못했어요."), {}, {}, {}, { _, _ -> }, {})
}
