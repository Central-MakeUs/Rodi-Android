package com.dororong.rodi.feature.home.component.sheet

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
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.shortenJibunAddress
import com.dororong.rodi.feature.home.shortenRoadAddress
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


@Preview(name = "CourseListContent - Collapsed", showBackground = true, widthDp = 360)
@Composable
private fun CourseListContentCollapsedPreview() {
    BottomSheetPreviewWrapper {
        CourseListContent(
            courses = HomePreviewData.courses.take(3),
            onCourseClick = {},
            expandFraction = 0f,
        )
    }
}

@Preview(name = "CourseListContent - Expanded", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun CourseListContentExpandedPreview() {
    BottomSheetPreviewWrapper {
        CourseListContent(
            courses = HomePreviewData.all,
            onCourseClick = {},
            expandFraction = 1f,
        )
    }
}

@Preview(name = "CourseListContent - Empty", showBackground = true, widthDp = 360, heightDp = 420)
@Composable
private fun CourseListContentEmptyPreview() {
    BottomSheetPreviewWrapper {
        CourseEmptyContent()
    }
}

@Preview(name = "CourseCard - Default", showBackground = true, widthDp = 360)
@Composable
private fun CourseCardDefaultPreview() {
    RodiTheme {
        CourseCard(
            course = HomePreviewData.courses.first(),
            onClick = {},
        )
    }
}

@Preview(name = "CourseCard - AddressExpanded", showBackground = true, widthDp = 360)
@Composable
private fun CourseCardAddressExpandedPreview() {
    RodiTheme {
        CourseCard(
            course = HomePreviewData.courses.first(),
            onClick = {},
            initialAddressExpanded = true,
        )
    }
}

@Preview(name = "CourseCard - Mixed", showBackground = true, widthDp = 360)
@Composable
private fun CourseCardMixedPreview() {
    RodiTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CourseCard(
                course = HomePreviewData.courses.first(),
                onClick = {},
            )
            CourseCard(
                course = HomePreviewData.freeParking,
                onClick = {},
            )
        }
    }
}
