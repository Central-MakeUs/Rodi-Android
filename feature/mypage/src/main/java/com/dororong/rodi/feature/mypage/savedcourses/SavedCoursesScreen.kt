package com.dororong.rodi.feature.mypage.savedcourses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.savedcourses.components.SavedCourseRow
import com.dororong.rodi.feature.mypage.savedcourses.components.SavedCoursesEmpty
import com.dororong.rodi.feature.mypage.savedcourses.components.SavedCoursesTopBar

@Composable
fun SavedCoursesScreen(
    onBack: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedCoursesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    SavedCoursesContent(
        state = uiState,
        onBack = onBack,
        onPlaceClick = onPlaceClick,
        onLoadNextPage = viewModel::loadNextPage,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun SavedCoursesContent(
    state: SavedCoursesUiState,
    onBack: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        SavedCoursesTopBar(onBack = onBack)
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RodiTheme.colors.primary600)
            }
            state.initialError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.initialError,
                        style = RodiTheme.typography.body3Medium,
                        color = RodiTheme.colors.gray700,
                    )
                    Spacer(Modifier.height(16.dp))
                    RodiButton(text = "다시 시도", onClick = onRetry, fillMaxWidth = false)
                }
            }
            state.places.isEmpty() -> SavedCoursesEmpty(modifier = Modifier.weight(1f))
            else -> {
                Text(
                    text = "${state.totalCount ?: state.places.size.toLong()}개",
                    style = RodiTheme.typography.caption2Medium,
                    color = RodiTheme.colors.gray700,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(
                        items = state.places,
                        key = { _, place -> "${place.type}:${place.id}" },
                    ) { index, place ->
                        SavedCourseRow(place = place, onClick = { onPlaceClick(place.id) })
                        if (index == state.places.lastIndex && state.hasNext) {
                            LaunchedEffect(place.id, state.nextCursor) { onLoadNextPage() }
                        }
                    }
                    if (state.isNextPageLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(color = RodiTheme.colors.primary600) }
                        }
                    } else if (state.nextPageError != null) {
                        item {
                            RodiButton(
                                text = "다시 시도",
                                onClick = onRetry,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SavedCoursesFilledPreview() {
    RodiTheme {
        SavedCoursesContent(
            state = SavedCoursesUiState(
                places = listOf(
                    PlaceSummary(
                        id = 1,
                        type = PlaceType.COURSE,
                        name = "망원한강공원 연습 코스",
                        address = "서울특별시 마포구",
                        point = GeoPoint(37.55, 126.9),
                        distanceFromMeMeters = null,
                        practiceTypes = listOf(PracticeType.INTERSECTION, PracticeType.LANE_CHANGE),
                        description = "교차로와 차선 변경을 연습하기 좋은 코스예요.",
                        distanceMeters = 4200,
                        capacity = null,
                        openTime = null,
                    ),
                ),
                totalCount = 1,
                isLoading = false,
            ),
            onBack = {},
            onPlaceClick = {},
            onLoadNextPage = {},
            onRetry = {},
        )
    }
}
