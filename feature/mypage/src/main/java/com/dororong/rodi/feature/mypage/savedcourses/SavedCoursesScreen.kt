package com.dororong.rodi.feature.mypage.savedcourses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CourseFeatures
import com.dororong.rodi.core.domain.model.course.Waypoint
import com.dororong.rodi.core.domain.model.course.WaypointType
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.savedcourses.components.SavedCourseRow
import com.dororong.rodi.feature.mypage.savedcourses.components.SavedCoursesEmpty
import com.dororong.rodi.feature.mypage.savedcourses.components.SavedCoursesTopBar

@Composable
fun SavedCoursesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedCoursesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    SavedCoursesContent(
        courses = uiState.courses,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun SavedCoursesContent(
    courses: List<Course>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        SavedCoursesTopBar(onBack = onBack)
        if (courses.isEmpty()) {
            SavedCoursesEmpty(modifier = Modifier.weight(1f))
        } else {
            Text(
                text = "${courses.size}개",
                style = RodiTheme.typography.caption2Medium,
                color = RodiTheme.colors.gray700,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = Course::id) { course ->
                    SavedCourseRow(course = course)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SavedCoursesEmptyPreview() {
    RodiTheme {
        SavedCoursesContent(courses = emptyList(), onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SavedCoursesFilledPreview() {
    RodiTheme {
        SavedCoursesContent(
            courses = List(100) { index -> savedCoursePreview(id = index) },
            onBack = {},
        )
    }
}

private fun savedCoursePreview(id: Int): Course = Course(
    id = id,
    courseName = "망원한강공원",
    courseNickname = "망원한강공원",
    areaName = "마포구",
    region = "seoul",
    difficulty = 2,
    trafficDensity = null,
    source = "preview",
    sourceUrl = "",
    crawledAt = "",
    waypoints = listOf(
        Waypoint(
            order = 0,
            type = WaypointType.START,
            name = "망원한강공원",
            lat = 37.551,
            lng = 126.902,
            address = "서울특별시 마포구 망원동",
            category = "도로",
        ),
        Waypoint(
            order = 1,
            type = WaypointType.END,
            name = "망원한강공원",
            lat = 37.641,
            lng = 126.902,
            address = "서울특별시 마포구 망원동",
            category = "도로",
        ),
    ),
    features = CourseFeatures(intersection = true, highway = true),
    recommendation = 1,
    caution = "",
    bestTime = "",
    enrichedDescription = "한강 뷰 보면서 드라이브 연습! 주말 오후엔 차량 적어요.",
)
