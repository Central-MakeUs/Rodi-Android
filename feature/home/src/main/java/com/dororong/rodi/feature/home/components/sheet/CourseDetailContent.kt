package com.dororong.rodi.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.dororong.rodi.core.data.SampleCourses
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.RouteResult
import com.dororong.rodi.core.ui.components.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun StableMeasuredDetailSheet(
    itemKey: Int,
    maxHeight: Dp,
    content: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    var measuredHeightPx by rememberSaveable(itemKey, maxHeight.value) { mutableStateOf<Float?>(null) }
    val measuredHeightDp = measuredHeightPx?.let { with(density) { it.toDp() } }
    val maxHeightModifier = if (maxHeight.isSpecified && maxHeight > 0.dp) {
        Modifier.heightIn(max = maxHeight)
    } else {
        Modifier
    }

    if (measuredHeightDp == null) {
        content(
            Modifier
                .fillMaxWidth()
                .then(maxHeightModifier)
                .onGloballyPositioned { coordinates ->
                    val heightPx = coordinates.size.height.toFloat()
                    if (heightPx <= 0f || measuredHeightPx != null) return@onGloballyPositioned
                    val maxHeightPx = with(density) {
                        if (maxHeight.isSpecified && maxHeight > 0.dp) maxHeight.toPx() else Float.POSITIVE_INFINITY
                    }
                    measuredHeightPx = heightPx.coerceAtMost(maxHeightPx)
                },
        )
    } else {
        content(
            Modifier
                .fillMaxWidth()
                .height(measuredHeightDp)
                .then(maxHeightModifier),
        )
    }
}

@Composable
fun CourseDetailContent(
    course: Course,
    route: RouteResult?,
    isRouting: Boolean,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    initialAddressExpanded: Boolean = false,
) {
    var addressExpanded by rememberSaveable(course.id) { mutableStateOf(initialAddressExpanded) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                course.title,
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = "닫기",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RatingRegionRow(
                    rating = course.rating,
                    region = course.regionDisplay,
                    onChevronClick = { addressExpanded = !addressExpanded },
                )
                if (addressExpanded) {
                    Spacer(modifier = Modifier.height(2.dp))
                    ExpandableAddressCard(
                        roadAddress = course.roadAddress.shortenRoadAddress(),
                        jibunAddress = course.jibunAddress.shortenJibunAddress(),
                    )
                } else {
                    Text(
                        distanceText(route, isRouting),
                        style = RodiTheme.typography.body3Medium,
                        color = RodiTheme.colors.gray800,
                    )
                    TagRow(difficulty = course.difficultyEnum, tags = course.tags)

                    Spacer(Modifier.height(8.dp))

                    SummaryBox(
                        text = course.summary,
                        bgColor = RodiTheme.colors.gray100,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            VerticalStepList(
                course = course,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(16.dp))

            RodiButton(
                text = "경로 안내",
                onClick = onNavigate,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}


@Preview(name = "CourseDetailContent - Routing", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun CourseDetailContentRoutingPreview() {
    BottomSheetPreviewWrapper {
        CourseDetailContent(
            course = SampleCourses.ALL.first { !it.isParking },
            route = null,
            isRouting = true,
            onDismiss = {},
            onNavigate = {},
        )
    }
}

@Preview(name = "CourseDetailContent - RouteReady", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun CourseDetailContentRouteReadyPreview() {
    BottomSheetPreviewWrapper {
        CourseDetailContent(
            course = SampleCourses.ALL.first { !it.isParking },
            route = RouteResult(
                points = emptyList(),
                isRealRoute = true,
                totalDistanceMeters = 12_400,
            ),
            isRouting = false,
            onDismiss = {},
            onNavigate = {},
        )
    }
}

@Preview(name = "CourseDetailContent - AddressExpanded", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun CourseDetailContentAddressExpandedPreview() {
    BottomSheetPreviewWrapper {
        CourseDetailContent(
            course = SampleCourses.ALL.first { !it.isParking },
            route = RouteResult(
                points = emptyList(),
                isRealRoute = true,
                totalDistanceMeters = 12_400,
            ),
            isRouting = false,
            onDismiss = {},
            onNavigate = {},
            initialAddressExpanded = true,
        )
    }
}
