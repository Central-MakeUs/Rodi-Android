package com.dororong.rodi.feature.home.component.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.ui.components.RodiButton
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.feature.home.routeDistanceValue

@Composable
fun CourseDetailContent(
    course: Course,
    route: RouteResult?,
    isRouting: Boolean,
    isBookmarked: Boolean = false,
    onDismiss: () -> Unit,
    onBookmarkClick: () -> Unit = {},
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = course.title,
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    painter = painterResource(
                        if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
                    ),
                    contentDescription = if (isBookmarked) "저장 취소" else "코스 저장",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = (course.likeCount + if (isBookmarked) 1 else 0).toString(),
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray700,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = "닫기",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = routeDistanceValue(route, isRouting),
                    style = RodiTheme.typography.headline2,
                    color = RodiTheme.colors.primary600,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "주행거리",
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray600,
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }

            TagRow(
                difficulty = course.difficultyEnum,
                tags = course.tags,
                showDifficulty = false,
            )

            if (course.caution.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_alert_circle),
                        contentDescription = null,
                        tint = RodiTheme.colors.primary600,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = course.caution,
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.primary600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = course.summary,
                style = RodiTheme.typography.caption1Regular,
                color = RodiTheme.colors.gray700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onBookmarkClick,
                shape = RoundedCornerShape(RodiRadius.sm),
                color = RodiTheme.colors.white,
                border = BorderStroke(1.dp, RodiTheme.colors.gray300),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
                    ),
                    contentDescription = if (isBookmarked) "저장 취소" else "코스 저장",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier.padding(14.dp),
                )
            }
            RodiButton(
                text = "경로 안내",
                onClick = onNavigate,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(name = "CourseDetailContent", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun CourseDetailContentPreview() {
    BottomSheetPreviewWrapper {
        CourseDetailContent(
            course = HomePreviewData.courses.first(),
            route = RouteResult(
                points = emptyList(),
                isRealRoute = true,
                totalDistanceMeters = 12_400,
            ),
            isRouting = false,
            isBookmarked = false,
            onDismiss = {},
            onBookmarkClick = {},
            onNavigate = {},
        )
    }
}

@Preview(name = "CourseDetailContent - Saved", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun CourseDetailContentSavedPreview() {
    BottomSheetPreviewWrapper {
        CourseDetailContent(
            course = HomePreviewData.courses.first(),
            route = null,
            isRouting = true,
            isBookmarked = true,
            onDismiss = {},
            onBookmarkClick = {},
            onNavigate = {},
        )
    }
}
