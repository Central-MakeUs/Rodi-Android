package com.dororong.rodi.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.components.MyPageTopBar
import com.dororong.rodi.feature.mypage.components.ProfileCard
import com.dororong.rodi.feature.mypage.components.SavedCoursesRow
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState

data class MyPageProfile(
    val nickname: String = "",
    val level: OnboardingLevel = OnboardingLevel.SEED,
    val practiceTypes: List<String> = emptyList(),
    val drivingGoal: String = "",
    val savedPlaceCount: Long = 0,
)

@Composable
fun MyPageScreen(
    onSettingsClick: () -> Unit,
    onGoalClick: () -> Unit,
    onSavedCoursesClick: () -> Unit,
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
            uiState.isLoading && uiState.profile.nickname.isBlank() -> MyPageLoadingContent(
            )
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
            RodiSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.18f)
                    .height(20.dp),
            )
            RodiSkeleton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(24.dp)
                    .fillMaxWidth(0.07f),
            )
        }
        RodiSkeleton(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(227.dp),
        )
        HorizontalDivider(color = RodiTheme.colors.gray100)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            RodiSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(16.dp),
            )
            Spacer(Modifier.height(12.dp))
            RodiSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
            )
        }
    }
}

@Composable
private fun MyPageContent(
    profile: MyPageProfile,
    onSettingsClick: () -> Unit,
    onGoalClick: () -> Unit,
    onSavedCoursesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        MyPageTopBar(onSettingsClick = onSettingsClick)
        ProfileCard(profile = profile, onGoalClick = onGoalClick)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = RodiTheme.colors.gray100)
        SavedCoursesRow(
            count = profile.savedPlaceCount,
            onClick = onSavedCoursesClick,
        )
        Spacer(Modifier.weight(1f))
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
            ),
            onSettingsClick = {},
            onGoalClick = {},
            onSavedCoursesClick = {},
        )
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
        )
    }
}
