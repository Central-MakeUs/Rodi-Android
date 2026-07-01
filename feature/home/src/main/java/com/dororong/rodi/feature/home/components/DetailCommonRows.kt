package com.dororong.rodi.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.data.SampleCourses
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.Difficulty
import com.dororong.rodi.core.domain.PracticeTag
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RatingRegionRow(
    rating: Double,
    region: String,
    onChevronClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_star),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "%.1f".format(rating),
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.primary600,
        )
        Text(" ･ ", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Row(
            modifier = Modifier.clickable(onClick = onChevronClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(region, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = "주소 보기",
                tint = RodiTheme.colors.gray800,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun ExpandableAddressCard(
    roadAddress: String,
    jibunAddress: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 1.dp, color = RodiTheme.colors.primary200),
        color = RodiTheme.colors.primary50,
    ) {
        Column(
            modifier = Modifier.padding(top = 10.dp, bottom = 11.dp, start = 10.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("도로명", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
                Text(roadAddress, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("지번", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
                Text(jibunAddress, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
            }
        }
    }
}

@Composable
fun TagRow(difficulty: Difficulty, tags: Set<PracticeTag>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DifficultyTag(difficulty)
        tags.take(2).forEach { tag ->
            PracticeTagChip(tag.label)
        }
    }
}

@Composable
fun DifficultyTag(difficulty: Difficulty) {
    val bgColor = when (difficulty) {
        Difficulty.LV1 -> Color(0xFFCDF2F6)
        Difficulty.LV2 -> Color(0xFFD0F7DF)
        Difficulty.LV3 -> Color(0xFFFFF6A4)
        Difficulty.LV4 -> Color(0xFFFFE6C0)
        Difficulty.LV5 -> Color(0xFFFFD6D6)
    }
    Surface(shape = RoundedCornerShape(2.dp), color = bgColor) {
        Text(
            difficulty.label,
            style = RodiTheme.typography.caption3Medium,
            color = RodiTheme.colors.gray800,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PracticeTagChip(label: String) {
    Surface(shape = RoundedCornerShape(2.dp), color = RodiTheme.colors.gray200) {
        Text(
            label,
            style = RodiTheme.typography.caption3Medium,
            color = RodiTheme.colors.gray700,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun SummaryBox(
    text: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
    ) {
        Text(
            text,
            style = RodiTheme.typography.caption1Regular,
            color = RodiTheme.colors.gray700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
fun VerticalStepList(course: Course, modifier: Modifier = Modifier) {
    val points = course.waypoints.sortedBy { it.order }
    Column(modifier = modifier) {
        points.forEachIndexed { i, point ->
            val isStart = i == 0
            val isEnd = i == points.lastIndex
            val dotColor = when {
                isStart -> RodiTheme.colors.pinStart
                isEnd -> RodiTheme.colors.pinArrival
                else -> RodiTheme.colors.gray400
            }
            val roleLabel = when {
                isStart -> "출발지"
                isEnd -> "도착지"
                else -> "경유지 $i"
            }
            val roleLabelColor = when {
                isStart -> RodiTheme.colors.pinStart
                isEnd -> RodiTheme.colors.pinArrival
                else -> RodiTheme.colors.gray800
            }
            val roleLabelWeight = if (isStart || isEnd) FontWeight.SemiBold else FontWeight.Medium

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = roleLabel,
                    style = RodiTheme.typography.caption1Medium.copy(fontWeight = roleLabelWeight),
                    color = roleLabelColor,
                    modifier = Modifier.width(54.dp),
                )
                Text(
                    text = point.displayStepAddress(),
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!isEnd) {
                Box(
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .width(1.dp)
                        .height(12.dp)
                        .background(RodiTheme.colors.gray400, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerticalStepListPreview() {
    RodiTheme {
        VerticalStepList(
            course = SampleCourses.ALL.first(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
