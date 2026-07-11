package com.dororong.rodi.feature.home.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.displayStepAddress

@Composable
fun VerticalStepList(course: Course, modifier: Modifier = Modifier) {
    val points = course.waypoints.sortedBy { it.order }
    Column(modifier = modifier) {
        points.forEachIndexed { index, point ->
            val isStart = index == 0
            val isEnd = index == points.lastIndex
            val dotColor = when {
                isStart -> RodiTheme.semantic.pinStart
                isEnd -> RodiTheme.semantic.pinArrival
                else -> RodiTheme.colors.gray400
            }
            val roleLabel = when {
                isStart -> "출발지"
                isEnd -> "도착지"
                else -> "경유지 $index"
            }
            val labelColor = when {
                isStart -> RodiTheme.semantic.pinStart
                isEnd -> RodiTheme.semantic.pinArrival
                else -> RodiTheme.colors.gray800
            }
            val labelWeight = if (isStart || isEnd) FontWeight.SemiBold else FontWeight.Medium
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
                Spacer(Modifier.width(4.dp))
                Text(
                    roleLabel,
                    style = RodiTheme.typography.caption1Medium.copy(fontWeight = labelWeight),
                    color = labelColor,
                    modifier = Modifier.width(54.dp),
                )
                Text(
                    point.displayStepAddress(),
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!isEnd) {
                Box(
                    Modifier
                        .padding(start = 3.dp)
                        .width(1.dp)
                        .height(12.dp)
                        .background(RodiTheme.colors.gray400, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
