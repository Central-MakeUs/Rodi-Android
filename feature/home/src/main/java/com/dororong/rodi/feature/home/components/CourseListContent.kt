package com.dororong.rodi.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.data.SampleCourses
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.R as CoreUiR

@Composable
fun CourseListContent(
    courses: List<Course>,
    onCourseClick: (Int) -> Unit,
    expandFraction: Float = 0f,
    onCollapse: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                "연습코스",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, bottom = 20.dp)
                    .graphicsLayer { alpha = 1f - expandFraction },
            )
            if (expandFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer { alpha = expandFraction },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "연습코스",
                        style = RodiTheme.typography.headline1,
                        color = RodiTheme.colors.black,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer { alpha = expandFraction },
                    ) {
                        IconButton(onClick = onCollapse, enabled = expandFraction > 0.5f) {
                            Icon(
                                painter = painterResource(CoreUiR.drawable.ic_chevron_left),
                                contentDescription = "접기",
                                tint = RodiTheme.colors.black,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 0.dp),
        ) {
            item {
                Spacer(Modifier.height(20.dp * expandFraction))
            }
            items(courses, key = { it.id }) { course ->
                CourseCard(course = course, onClick = { onCourseClick(course.id) })
                if (course != courses.last()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        thickness = 1.dp,
                        color = RodiTheme.colors.primary100,
                    )
                }
            }
            item {
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(8.dp),
                )
            }
        }
    }
}

@Composable
fun CourseEmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "추천할 수 있는 연습 코스를 찾지 못했어요.",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.gray800,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "지도를 축소시켜, 전체 지역의\n연습 코스를 둘러보세요.",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun CourseCard(course: Course, onClick: () -> Unit) {
    var addressExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            course.title,
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
            maxLines = 1,
        )
        RatingRegionRow(
            rating = course.rating,
            region = course.regionDisplay,
            onChevronClick = { addressExpanded = !addressExpanded },
        )
        if (addressExpanded) {
            ExpandableAddressCard(
                roadAddress = course.roadAddress.shortenRoadAddress(),
                jibunAddress = course.jibunAddress.shortenJibunAddress(),
            )
            Spacer(modifier = Modifier.height(0.5.dp))
        } else {
            TagRow(difficulty = course.difficultyEnum, tags = course.tags)
            Spacer(modifier = Modifier.height(8.dp))
            SummaryBox(text = course.summary, bgColor = RodiTheme.colors.gray50)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CourseListContentPreview() {
    BottomSheetPreviewWrapper {
        CourseListContent(courses = SampleCourses.ALL.filterNot { it.isParking }.take(3), onCourseClick = {}, expandFraction = 0f)
    }
}

@Preview(name = "CourseListContent expanded", showBackground = true, widthDp = 375, heightDp = 720)
@Composable
fun CourseListContentExpandedPreview() {
    BottomSheetPreviewWrapper {
        CourseListContent(courses = SampleCourses.ALL.take(6), onCourseClick = {}, expandFraction = 1f)
    }
}

@Preview(name = "CourseListContent empty", showBackground = true, widthDp = 375, heightDp = 420)
@Composable
fun CourseListContentEmptyPreview() {
    BottomSheetPreviewWrapper {
        CourseEmptyContent()
    }
}

@Preview(showBackground = true)
@Composable
fun CourseCardPreview() {
    RodiTheme {
        CourseCard(
            course = SampleCourses.ALL.first(),
            onClick = {},
        )
    }
}

@Preview(name = "CourseCard mixed", showBackground = true, widthDp = 375, heightDp = 420)
@Composable
fun CourseCardMixedPreview() {
    RodiTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CourseCard(
                course = SampleCourses.ALL.first { !it.isParking },
                onClick = {},
            )
            CourseCard(
                course = SampleCourses.ALL.first { it.isParking },
                onClick = {},
            )
        }
    }
}
