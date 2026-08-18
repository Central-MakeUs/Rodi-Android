package com.dororong.rodi.feature.mypage.myposts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.RodiIllustratedEmptyState
import com.dororong.rodi.core.ui.components.RodiPopupMenu
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R
import com.dororong.rodi.feature.mypage.registeredcourses.RegisteredCoursesContent
import com.dororong.rodi.feature.mypage.registeredcourses.RegisteredCourseFilter
import com.dororong.rodi.feature.mypage.registeredcourses.RegisteredCoursesViewModel
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
    onRegisterCourseClick: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val registeredCoursesViewModel: RegisteredCoursesViewModel = hiltViewModel()
    val registeredCoursesState by registeredCoursesViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MyActivityTab.RegisteredCourses) }
    val registeredCoursesAllListState = rememberLazyListState()
    val registeredCoursesApprovedListState = rememberLazyListState()
    val registeredCoursesPendingListState = rememberLazyListState()
    val registeredCoursesRejectedListState = rememberLazyListState()
    val registeredCoursesListState = when (registeredCoursesState.selectedFilter) {
        RegisteredCourseFilter.ALL -> registeredCoursesAllListState
        RegisteredCourseFilter.APPROVED -> registeredCoursesApprovedListState
        RegisteredCourseFilter.PENDING -> registeredCoursesPendingListState
        RegisteredCourseFilter.REJECTED -> registeredCoursesRejectedListState
    }
    val reviewListState = rememberLazyListState()
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
    MyActivityContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onBack = onBack,
        registeredCoursesState = registeredCoursesState,
        registeredCoursesViewModel = registeredCoursesViewModel,
        registeredCoursesListState = registeredCoursesListState,
        onRegisterCourseClick = onRegisterCourseClick,
        reviewState = state,
        reviewListState = reviewListState,
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
private fun MyActivityContent(
    selectedTab: MyActivityTab,
    onTabSelected: (MyActivityTab) -> Unit,
    onBack: () -> Unit,
    registeredCoursesState: com.dororong.rodi.feature.mypage.registeredcourses.RegisteredCoursesUiState,
    registeredCoursesViewModel: RegisteredCoursesViewModel,
    registeredCoursesListState: LazyListState,
    onRegisterCourseClick: () -> Unit,
    reviewState: MyPostsUiState,
    reviewListState: LazyListState,
    onPracticeRecordsClick: () -> Unit,
    onEditReviewClick: (MyPost) -> Unit,
    onLoadInitial: () -> Unit,
    onLoadNext: () -> Unit,
    onClearError: () -> Unit,
    onDelete: (MyPost) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PostsTopBar(onBack = onBack)
            MyActivityTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == MyActivityTab.RegisteredCourses) {
                    RegisteredCoursesContent(
                        state = registeredCoursesState,
                        onFilterSelected = registeredCoursesViewModel::selectFilter,
                        onRegisterCourseClick = onRegisterCourseClick,
                        onLoadInitial = registeredCoursesViewModel::loadInitial,
                        onLoadNext = registeredCoursesViewModel::loadNextPage,
                        onRetry = registeredCoursesViewModel::retry,
                        onClearError = registeredCoursesViewModel::clearError,
                        onDelete = registeredCoursesViewModel::delete,
                        applyNavigationBarsPadding = false,
                        listState = registeredCoursesListState,
                    )
                } else {
                    MyPostsContent(
                        state = reviewState,
                        onBack = {},
                        onPracticeRecordsClick = onPracticeRecordsClick,
                        onEditReviewClick = onEditReviewClick,
                        onLoadInitial = onLoadInitial,
                        onLoadNext = onLoadNext,
                        onClearError = onClearError,
                        onDelete = onDelete,
                        showTopBar = false,
                        applyInsets = false,
                        listState = reviewListState,
                    )
                }
            }
        }
    }
}

@Composable
private fun MyActivityTabs(
    selectedTab: MyActivityTab,
    onTabSelected: (MyActivityTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(45.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MyActivityTabItem(
            text = "등록한 코스",
            selected = selectedTab == MyActivityTab.RegisteredCourses,
            onClick = { onTabSelected(MyActivityTab.RegisteredCourses) },
            modifier = Modifier.weight(1f),
        )
        MyActivityTabItem(
            text = "작성한 후기",
            selected = selectedTab == MyActivityTab.Reviews,
            onClick = { onTabSelected(MyActivityTab.Reviews) },
            modifier = Modifier.weight(1f),
        )
    }
    HorizontalDivider(color = RodiTheme.colors.gray200)
}

@Composable
private fun MyActivityTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 인디케이터는 탭 행 맨 아래에 붙는다(디자인 3800:68268). 텍스트는 행 중앙 정렬.
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = if (selected) RodiTheme.typography.body1SemiBold else RodiTheme.typography.body1Medium,
            color = if (selected) RodiTheme.colors.black else RodiTheme.colors.gray400,
            modifier = Modifier.align(Alignment.Center),
        )
        if (selected) {
            Spacer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(RodiTheme.colors.black),
            )
        }
    }
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
    showTopBar: Boolean = true,
    applyInsets: Boolean = true,
    listState: LazyListState? = null,
) {
    var menuPostId by remember { mutableStateOf(initialMenuPostId) }
    var deleteTarget by remember { mutableStateOf<MyPost?>(null) }
    val scrollState = listState ?: rememberLazyListState()
    LaunchedEffect(scrollState, state.posts.size, state.hasNext) {
        snapshotFlow { scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .map { index -> index != null && index >= state.posts.lastIndex - 2 }
            .distinctUntilChanged()
            .collect { shouldLoad -> if (shouldLoad) onLoadNext() }
    }
    Surface(modifier = modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(
            modifier = Modifier.fillMaxSize().let {
                if (applyInsets) it.statusBarsPadding().navigationBarsPadding() else it
            },
        ) {
            if (showTopBar) PostsTopBar(onBack = onBack)
            when {
                state.isLoading -> MyPostsLoading()
                state.errorMessage != null && state.posts.isEmpty() -> MyPostsError(
                    message = state.errorMessage,
                    onRetry = { onClearError(); onLoadInitial() },
                )
                state.posts.isEmpty() -> MyPostsEmpty(
                    onPracticeRecordsClick = onPracticeRecordsClick,
                    showPracticeRecordsButton = state.hasPracticeRecords,
                )
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
            description = "이 후기는 다른 초보운전자에게\n도움이 되고 있어요.",
            descriptionMaxLines = 2,
            dismissText = "삭제하기",
            confirmText = "취소",
            onDismiss = { deleteTarget = null; onDelete(target) },
            onConfirm = { deleteTarget = null },
            onDismissRequest = { deleteTarget = null },
        )
    }
}

@Composable
private fun MyPostsLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
    ) {
        repeat(4) { MyPostSkeletonRow() }
    }
}

@Composable
private fun MyPostSkeletonRow() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RodiSkeleton(modifier = Modifier.width(128.dp).height(20.dp))
            Spacer(Modifier.weight(1f))
            RodiSkeleton(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(14.dp),
            )
        }
        RodiSkeleton(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(48.dp)
                .height(12.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(37.dp)
                .background(RodiTheme.colors.gray50, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            RodiSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(14.dp),
                shape = RoundedCornerShape(4.dp),
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 14.dp),
            color = RodiTheme.colors.gray100,
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
        Text("내 활동", style = RodiTheme.typography.headline1, color = RodiTheme.colors.black, modifier = Modifier.align(Alignment.Center))
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
                    menuWidth = 75.dp,
                    scrollState = scrollState,
                )
            }
        }
        Text(MyPostDateFormatter.format(post.review.createdAt), style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray600, modifier = Modifier.padding(top = 4.dp))
        post.review.content
            ?.takeIf { it.isNotBlank() }
            ?.let { content ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(37.dp)
                        .background(RodiTheme.colors.white, RoundedCornerShape(8.dp))
                        .border(1.dp, RodiTheme.colors.gray200, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = content,
                        style = RodiTheme.typography.caption1Regular,
                        color = RodiTheme.colors.gray700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = RodiTheme.colors.gray100)
    }
}

@Composable
private fun MyPostsEmpty(
    onPracticeRecordsClick: () -> Unit,
    showPracticeRecordsButton: Boolean,
) {
    RodiIllustratedEmptyState(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        painter = painterResource(R.drawable.illust_my_activity_empty),
        imageSize = 60.dp,
        title = "아직 작성한 후기가 없어요!",
        description = "다녀온 코스의 경험을 기록해보세요.",
        footer = {
            if (showPracticeRecordsButton) {
                OutlinedButton(
                    onClick = onPracticeRecordsClick,
                    modifier = Modifier.padding(top = 20.dp).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RodiTheme.colors.primary600),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RodiTheme.colors.primary600),
                ) { Text("연습기록 보러가기", style = RodiTheme.typography.body3Medium) }
            }
        },
    )
}

private val MyPostDateFormatter = DateTimeFormatter.ofPattern("yy.MM.dd").withZone(ZoneId.systemDefault())

private val PreviewPosts = listOf(
    MyPost(
        placeId = 1,
        placeName = "망원한강공원",
        review = Review(1, 1, "로디", OnboardingLevel.ROOKIE, true, ReviewDifficulty.EASY, ReviewCongestion.QUIET, PracticeMethod.SOLO, "차선 변경 연습에 좋아요.", null, true, true, false, Instant.parse("2026-05-10T00:00:00Z"), true),
    ),
    MyPost(
        placeId = 2,
        placeName = "용산구 교차로",
        review = Review(2, 1, "로디", OnboardingLevel.ROOKIE, true, ReviewDifficulty.NORMAL, ReviewCongestion.NORMAL, PracticeMethod.WITH_COMPANION, "회전 구간은 천천히 진입하세요.", null, true, true, false, Instant.parse("2026-05-08T00:00:00Z"), true),
    ),
)

@Preview(name = "내 활동 목록", showBackground = true, widthDp = 375, heightDp = 812)
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

@Preview(name = "내 활동 등록 코스", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyActivityRegisteredCoursesPreview() = RodiTheme {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PostsTopBar(onBack = {})
        MyActivityTabs(
            selectedTab = MyActivityTab.RegisteredCourses,
            onTabSelected = {},
        )
        RegisteredCoursesContent(
            state = com.dororong.rodi.feature.mypage.registeredcourses.RegisteredCoursesUiState(
                courses = listOf(
                    com.dororong.rodi.core.domain.model.course.RegisteredCourse(
                        courseId = 1L,
                        name = "서울 성북구 길음동 4938-3",
                        approvalStatus = com.dororong.rodi.core.domain.model.course.CourseApprovalStatus.APPROVED,
                        createdAt = Instant.parse("2026-05-10T00:00:00Z"),
                    ),
                ),
            ),
            onFilterSelected = {},
            onRegisterCourseClick = {},
            onLoadInitial = {},
            onLoadNext = {},
            onRetry = {},
            onClearError = {},
            onDelete = {},
            applyNavigationBarsPadding = false,
        )
    }
}

@Preview(name = "내 활동 로딩", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPostsLoadingPreview() = RodiTheme {
    MyPostsContent(
        state = MyPostsUiState(isLoading = true),
        onBack = {},
        onPracticeRecordsClick = {},
        onEditReviewClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "내 활동 메뉴", showBackground = true, widthDp = 375, heightDp = 812)
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

@Preview(name = "내 활동 삭제 확인", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPostsDeletePreview() = RodiTheme {
    RodiAlertDialog(title = "정말 삭제하시겠습니까?", description = "이 후기는 다른 초보운전자에게\n도움이 되고 있어요.", descriptionMaxLines = 2, dismissText = "삭제하기", confirmText = "취소", onDismiss = {}, onConfirm = {}, onDismissRequest = {})
}

@Preview(name = "내 활동 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
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
