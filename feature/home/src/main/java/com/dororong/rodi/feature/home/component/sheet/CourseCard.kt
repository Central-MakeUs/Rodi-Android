package com.dororong.rodi.feature.home.component.sheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.shortenJibunAddress
import com.dororong.rodi.feature.home.shortenRoadAddress

@Composable
fun CourseCard(
    course: Course,
    onClick: () -> Unit,
    initialAddressExpanded: Boolean = false,
) {
    var addressExpanded by rememberSaveable(course.id) { mutableStateOf(initialAddressExpanded) }

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
