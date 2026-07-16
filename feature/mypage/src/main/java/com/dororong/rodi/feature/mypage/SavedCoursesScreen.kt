package com.dororong.rodi.feature.mypage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiTheme
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = Course::id) { course ->
                    SavedCourseRow(course = course)
                    HorizontalDivider(color = RodiTheme.colors.gray100)
                }
            }
        }
    }
}

@Composable
private fun SavedCoursesTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            contentDescription = "뒤로가기",
            tint = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Text(
            text = "저장한 코스",
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
        )
    }
}

@Composable
private fun SavedCoursesEmpty(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-122).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.illust_saved_course_empty),
                contentDescription = null,
                modifier = Modifier.size(60.dp),
            )
            Text(
                text = "저장한 코스가 없어요.",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.gray600,
            )
            Text(
                text = "홈에서 나에게 맞는 연습 코스를 찾아\n저장해보세요.",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SavedCourseRow(course: Course) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = course.title,
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = course.formattedDrivingDistance(),
                style = RodiTheme.typography.caption1SemiBold,
                color = RodiTheme.colors.primary600,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "주행거리",
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            course.tags.take(2).forEach { tag ->
                Text(
                    text = tag.label,
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.black,
                    modifier = Modifier
                        .background(RodiTheme.colors.gray100, RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = course.summary,
            style = RodiTheme.typography.caption1Medium,
            color = RodiTheme.colors.gray600,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(RodiTheme.colors.gray50, RoundedCornerShape(8.dp))
                .padding(12.dp),
        )
    }
}

private fun Course.formattedDrivingDistance(): String {
    val distanceKm = allPoints.zipWithNext().sumOf { (from, to) ->
        val latitudeDelta = Math.toRadians(to.lat - from.lat)
        val longitudeDelta = Math.toRadians(to.lng - from.lng)
        val startLatitude = Math.toRadians(from.lat)
        val endLatitude = Math.toRadians(to.lat)
        val haversine = sin(latitudeDelta / 2).let { it * it } +
            cos(startLatitude) * cos(endLatitude) * sin(longitudeDelta / 2).let { it * it }
        6_371.0 * 2 * asin(sqrt(haversine))
    }
    val rounded = (distanceKm * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()}km" else "${rounded}km"
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SavedCoursesEmptyPreview() {
    RodiTheme {
        SavedCoursesContent(courses = emptyList(), onBack = {})
    }
}
