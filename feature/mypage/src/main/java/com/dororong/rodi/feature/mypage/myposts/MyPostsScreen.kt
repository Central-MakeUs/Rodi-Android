package com.dororong.rodi.feature.mypage.myposts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.RodiPopupMenu
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun MyPostsScreen(
    onBack: () -> Unit,
    onPracticeRecordsClick: () -> Unit,
    onEditReviewClick: (MyPost) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPostsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasResumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasResumed) viewModel.loadInitial()
                hasResumed = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    MyPostsContent(
        state = state,
        onBack = onBack,
        onPracticeRecordsClick = onPracticeRecordsClick,
        onEditReviewClick = onEditReviewClick,
        onLoadInitial = viewModel::loadInitial,
        onLoadNext = viewModel::loadNextPage,
        onClearError = viewModel::clearError,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@Composable
private fun MyPostsContent(
    state: MyPostsUiState,
    onBack: () -> Unit,
    onPracticeRecordsClick: () -> Unit,
    onEditReviewClick: (MyPost) -> Unit,
    onLoadInitial: () -> Unit,
    onLoadNext: () -> Unit,
    onClearError: () -> Unit,
    onDelete: (MyPost) -> Unit,
    modifier: Modifier = Modifier,
    initialMenuPostId: Long? = null,
) {
    var menuPostId by remember { mutableStateOf(initialMenuPostId) }
    var deleteTarget by remember { mutableStateOf<MyPost?>(null) }
    val scrollState = rememberLazyListState()
    LaunchedEffect(scrollState, state.posts.size, state.hasNext) {
        snapshotFlow { scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .map { index -> index != null && index >= state.posts.lastIndex - 2 }
            .distinctUntilChanged()
            .collect { shouldLoad -> if (shouldLoad) onLoadNext() }
    }
    Surface(modifier = modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PostsTopBar(onBack = onBack)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("불러오는 중…", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600) }
                state.errorMessage != null && state.posts.isEmpty() -> MyPostsError(
                    message = state.errorMessage,
                    onRetry = { onClearError(); onLoadInitial() },
                )
                state.posts.isEmpty() -> MyPostsEmpty(onPracticeRecordsClick)
                else -> LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(state.posts, key = { it.review.reviewId }) { post ->
                        MyPostRow(
                            post = post,
                            menuExpanded = menuPostId == post.review.reviewId,
                            onMenuClick = { menuPostId = post.review.reviewId },
                            onMenuDismiss = { menuPostId = null },
                            onEdit = { menuPostId = null; onEditReviewClick(post) },
                            onDelete = { menuPostId = null; deleteTarget = post },
                            scrollState = scrollState,
                        )
                    }
                    if (state.errorMessage != null) {
                        item(key = "error") {
                            MyPostsNextPageError(
                                message = state.errorMessage,
                                onRetry = { onClearError(); onLoadNext() },
                            )
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { target ->
        RodiAlertDialog(
            title = "정말 삭제하시겠습니까?",
            description = "이 후기는 다른 초보운전자에게도 도움이 되고\n있어요. 삭제하면 더 이상 공개되지 않아요.",
            dismissText = "취소",
            confirmText = "삭제하기",
            onDismiss = { deleteTarget = null },
            onConfirm = { deleteTarget = null; onDelete(target) },
            onDismissRequest = { deleteTarget = null },
        )
    }
}

@Composable
private fun MyPostsError(message: String, onRetry: () -> Unit) {
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
private fun MyPostsNextPageError(message: String, onRetry: () -> Unit) {
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

@Composable
private fun PostsTopBar(onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(painterResource(CoreUiR.drawable.ic_chevron_left), "뒤로", tint = RodiTheme.colors.black)
        }
        Text("내 게시글", style = RodiTheme.typography.headline1, color = RodiTheme.colors.black, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun MyPostRow(
    post: MyPost,
    menuExpanded: Boolean,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    scrollState: androidx.compose.foundation.gestures.ScrollableState,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(post.placeName, style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = onMenuClick, modifier = Modifier.size(28.dp)) {
                    Icon(painterResource(com.dororong.rodi.feature.mypage.R.drawable.ic_more_horizontal), "더보기", tint = RodiTheme.colors.gray600)
                }
                RodiPopupMenu(
                    expanded = menuExpanded,
                    items = listOf("수정하기", "삭제하기"),
                    onSelect = { index -> if (index == 0) onEdit() else onDelete() },
                    onDismissRequest = onMenuDismiss,
                    scrollState = scrollState,
                )
            }
        }
        Text(MyPostDateFormatter.format(post.review.createdAt), style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray600, modifier = Modifier.padding(top = 4.dp))
        Text(
            text = post.review.content.orEmpty().ifBlank { "작성한 후기 내용이 없어요." },
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(RodiTheme.colors.gray100, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(12.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = RodiTheme.colors.gray100)
    }
}

@Composable
private fun MyPostsEmpty(onPracticeRecordsClick: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("아직 작성한 후기가 없어요!", style = RodiTheme.typography.headline1, color = RodiTheme.colors.gray600)
            Text("다녀온 코스의 경험을 기록해보세요.", style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray600, modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = onPracticeRecordsClick,
                modifier = Modifier.padding(top = 20.dp).height(40.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, RodiTheme.colors.primary600),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RodiTheme.colors.primary600),
            ) { Text("연습기록 보러가기", style = RodiTheme.typography.body3Medium) }
        }
    }
}

private val MyPostDateFormatter = DateTimeFormatter.ofPattern("yy.MM.dd").withZone(ZoneId.systemDefault())

private val PreviewPosts = listOf(
    MyPost(
        placeId = 1,
        placeName = "망원한강공원",
        review = Review(1, 1, "로디", OnboardingLevel.ROOKIE, true, ReviewDifficulty.EASY, ReviewCongestion.QUIET, PracticeMethod.SOLO, "차선 변경 연습에 좋아요.", null, true, true, false, Instant.parse("2026-05-10T00:00:00Z")),
    ),
    MyPost(
        placeId = 2,
        placeName = "용산구 교차로",
        review = Review(2, 1, "로디", OnboardingLevel.ROOKIE, true, ReviewDifficulty.NORMAL, ReviewCongestion.NORMAL, PracticeMethod.WITH_COMPANION, "회전 구간은 천천히 진입하세요.", null, true, true, false, Instant.parse("2026-05-08T00:00:00Z")),
    ),
)

@Preview(name = "내 게시글 목록", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPostsListPreview() = RodiTheme {
    MyPostsContent(
        state = MyPostsUiState(posts = PreviewPosts),
        onBack = {},
        onPracticeRecordsClick = {},
        onEditReviewClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "내 게시글 메뉴", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPostsMenuPreview() = RodiTheme {
    MyPostsContent(
        state = MyPostsUiState(posts = PreviewPosts),
        onBack = {},
        onPracticeRecordsClick = {},
        onEditReviewClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onClearError = {},
        onDelete = {},
        initialMenuPostId = 1L,
    )
}

@Preview(name = "내 게시글 삭제 확인", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPostsDeletePreview() = RodiTheme {
    RodiAlertDialog(title = "정말 삭제하시겠습니까?", description = "이 후기는 다른 초보운전자에게도 도움이 되고\n있어요. 삭제하면 더 이상 공개되지 않아요.", dismissText = "취소", confirmText = "삭제하기", onDismiss = {}, onConfirm = {}, onDismissRequest = {})
}

@Preview(name = "내 게시글 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPostsEmptyPreview() = RodiTheme {
    MyPostsContent(
        state = MyPostsUiState(),
        onBack = {},
        onPracticeRecordsClick = {},
        onEditReviewClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onClearError = {},
        onDelete = {},
    )
}
