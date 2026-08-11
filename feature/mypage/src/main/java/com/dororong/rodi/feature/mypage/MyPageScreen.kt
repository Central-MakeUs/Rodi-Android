package com.dororong.rodi.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.components.MyPageTopBar
import com.dororong.rodi.feature.mypage.components.ProfileCard
import com.dororong.rodi.feature.mypage.components.PracticeRecordSection
import com.dororong.rodi.feature.mypage.components.SavedCoursesRow
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.feature.mypage.practicerecords.PracticeRecord
import com.dororong.rodi.core.domain.model.place.PracticeType
import java.time.Instant

data class MyPageProfile(
    val nickname: String = "",
    val level: OnboardingLevel = OnboardingLevel.SEED,
    val practiceTypes: List<String> = emptyList(),
    val drivingGoal: String = "",
    val savedPlaceCount: Long = 0,
    val progress: Float = 0f,
    val distanceLabel: String = "0km",
)

@Composable
fun MyPageScreen(
    onSettingsClick: () -> Unit,
    onGoalClick: () -> Unit,
    onSavedCoursesClick: () -> Unit,
    onPracticeRecordsClick: () -> Unit,
    onMyPostsClick: () -> Unit,
    onWriteReviewClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { RodiSnackbarHostState() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.errorMessage, uiState.profile.nickname) {
        val message = uiState.errorMessage
        if (message != null && uiState.profile.nickname.isNotBlank()) {
            snackbarHostState.show(RodiSnackbarData(message = message))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.profile.nickname.isBlank() -> MyPageLoadingContent()
            uiState.errorMessage != null && uiState.profile.nickname.isBlank() -> Box(
                modifier = Modifier.fillMaxSize().background(RodiTheme.colors.white),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = RodiTheme.typography.body3Medium,
                        color = RodiTheme.colors.gray700,
                    )
                    Spacer(Modifier.height(16.dp))
                    RodiButton(text = "다시 시도", onClick = viewModel::refresh, fillMaxWidth = false)
                }
            }
            else -> MyPageContent(
                profile = uiState.profile,
                onSettingsClick = onSettingsClick,
                onGoalClick = onGoalClick,
                onSavedCoursesClick = onSavedCoursesClick,
                onPracticeRecordsClick = onPracticeRecordsClick,
                onMyPostsClick = onMyPostsClick,
                onWriteReviewClick = onWriteReviewClick,
                practiceRecords = uiState.practiceRecords,
                practiceRecordsErrorMessage = uiState.practiceRecordsErrorMessage,
                onPracticeRecordsRetry = viewModel::refresh,
            )
        }
        RodiSnackbarHost(snackbarHostState)
    }
}

@Composable
private fun MyPageLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "마이페이지",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
            )
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_settings),
                contentDescription = null,
                tint = RodiTheme.colors.black,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp),
            )
        }
        MyPageProfileCardLoadingContent()
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = RodiTheme.colors.gray100)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RodiSkeleton(
                modifier = Modifier
                    .width(128.dp)
                    .height(20.dp),
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                contentDescription = null,
                tint = RodiTheme.colors.gray600,
                modifier = Modifier
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun MyPageProfileCardLoadingContent() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(227.dp)
            .background(RodiTheme.colors.gray50, RoundedCornerShape(8.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 11.dp, top = 15.dp, end = 11.dp),
        ) {
            Row(modifier = Modifier.height(90.dp)) {
                RodiSkeleton(modifier = Modifier.size(90.dp), color = RodiTheme.colors.gray200)
                Column(
                    modifier = Modifier.padding(start = 15.dp, top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RodiSkeleton(modifier = Modifier.width(92.dp).height(20.dp), color = RodiTheme.colors.gray200)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        RodiSkeleton(modifier = Modifier.width(28.dp).height(12.dp), color = RodiTheme.colors.gray200)
                        RodiSkeleton(modifier = Modifier.width(56.dp).height(16.dp), color = RodiTheme.colors.gray200)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            RodiSkeleton(modifier = Modifier.width(52.dp).height(12.dp), color = RodiTheme.colors.gray200)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RodiSkeleton(modifier = Modifier.width(40.dp).height(21.dp), color = RodiTheme.colors.gray200)
                RodiSkeleton(modifier = Modifier.width(48.dp).height(21.dp), color = RodiTheme.colors.gray200)
                RodiSkeleton(modifier = Modifier.width(44.dp).height(21.dp), color = RodiTheme.colors.gray200)
            }
            Spacer(Modifier.height(12.dp))
            RodiSkeleton(modifier = Modifier.width(42.dp).height(12.dp), color = RodiTheme.colors.gray200)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RodiSkeleton(modifier = Modifier.width(188.dp).height(19.dp), color = RodiTheme.colors.gray200)
                Spacer(Modifier.weight(1f))
                RodiSkeleton(modifier = Modifier.size(12.dp), color = RodiTheme.colors.gray200)
            }
        }
    }
}

@Composable
private fun MyPageContent(
    profile: MyPageProfile,
    onSettingsClick: () -> Unit,
    onGoalClick: () -> Unit,
    onSavedCoursesClick: () -> Unit,
    onPracticeRecordsClick: () -> Unit,
    onMyPostsClick: () -> Unit,
    onWriteReviewClick: (Long, String) -> Unit,
    practiceRecords: List<PracticeRecord> = emptyList(),
    practiceRecordsErrorMessage: String? = null,
    onPracticeRecordsRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        MyPageTopBar(onSettingsClick = onSettingsClick)
        ProfileCard(profile = profile, onGoalClick = onGoalClick)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = RodiTheme.colors.gray100)
        PracticeRecordSection(
            records = practiceRecords,
            errorMessage = practiceRecordsErrorMessage,
            onAllClick = onPracticeRecordsClick,
            onWriteReviewClick = onWriteReviewClick,
            onRetry = onPracticeRecordsRetry,
        )
        HorizontalDivider(color = RodiTheme.colors.gray100)
        SavedCoursesRow(
            count = profile.savedPlaceCount,
            onClick = onSavedCoursesClick,
        )
        MyPageNavigationRow(text = "내 게시글", onClick = onMyPostsClick)
        // 바텀 네비게이션이 sibling overlay로 얹히므로 그 높이만큼 자리를 비워둔다.
        // RodiBottomNavigation은 `navigationBarsPadding().height(56.dp)` 순서라 실제 높이가
        // navInset + 56dp다. 여기서 순서를 뒤집으면 Spacer가 56dp로 고정돼 마지막 행이 가려진다.
        Spacer(Modifier.navigationBarsPadding().height(56.dp))
    }
}

@Composable
private fun MyPageNavigationRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = RodiTheme.typography.body1Medium, color = RodiTheme.colors.black, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RodiTheme.colors.gray600,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPageContentPreview() {
    RodiTheme {
        MyPageContent(
            profile = MyPageProfile(
                nickname = "흐름타는 고슴도치",
                level = OnboardingLevel.ROOKIE,
                practiceTypes = listOf("유턴", "차선변경", "주차", "교차로"),
                drivingGoal = "복잡한 강남 자신있게 운전하기",
                savedPlaceCount = 5,
                progress = 0.4f,
            ),
            onSettingsClick = {},
            onGoalClick = {},
            onSavedCoursesClick = {},
            onPracticeRecordsClick = {},
            onMyPostsClick = {},
            onWriteReviewClick = { _, _ -> },
            onPracticeRecordsRetry = {},
            practiceRecords = listOf(
                PracticeRecord(1, 1, "망원한강공원", listOf(PracticeType.ROUNDABOUT), 1, Instant.parse("2026-05-10T00:00:00Z"), true, false),
                PracticeRecord(2, 2, "용산구 교차로", listOf(PracticeType.PARKING), 2, Instant.parse("2026-05-09T00:00:00Z"), true, true),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPageLoadingPreview() {
    RodiTheme {
        MyPageLoadingContent()
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPageIncompleteProfilePreview() {
    RodiTheme {
        MyPageContent(
            profile = MyPageProfile(
                nickname = "운전 초보",
                level = OnboardingLevel.SEED,
            ),
            onSettingsClick = {},
            onGoalClick = {},
            onSavedCoursesClick = {},
            onPracticeRecordsClick = {},
            onMyPostsClick = {},
            onWriteReviewClick = { _, _ -> },
            onPracticeRecordsRetry = {},
        )
    }
}
