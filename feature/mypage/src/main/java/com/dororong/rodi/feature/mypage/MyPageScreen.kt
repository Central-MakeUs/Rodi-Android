package com.dororong.rodi.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.components.MyPageTopBar
import com.dororong.rodi.feature.mypage.components.ProfileCard
import com.dororong.rodi.feature.mypage.components.SavedCoursesRow

data class MyPageProfile(
    val nickname: String = "",
    val level: OnboardingLevel = OnboardingLevel.SEED,
    val practiceTypes: List<String> = emptyList(),
    val drivingGoal: String = "",
    val savedCourseCount: Int = 0,
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
    MyPageContent(
        profile = uiState.profile,
        onSettingsClick = onSettingsClick,
        onGoalClick = onGoalClick,
        onSavedCoursesClick = onSavedCoursesClick,
        modifier = modifier,
    )
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
            count = profile.savedCourseCount,
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
                practiceTypes = listOf("차선변경", "교차로", "주차"),
                drivingGoal = "복잡한 강남 자신있게 운전하기",
                savedCourseCount = 5,
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
