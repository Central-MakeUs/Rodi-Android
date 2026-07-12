package com.dororong.rodi.feature.home.component.sheet

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.Difficulty
import com.dororong.rodi.core.domain.model.course.PracticeTag
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.feature.home.displayStepAddress

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
            modifier = Modifier.clickable(
                onClick = onChevronClick,
                role = Role.Button,
            ),
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

@Preview(name = "VerticalStepList - Default", showBackground = true, widthDp = 360)
@Composable
private fun VerticalStepListPreview() {
    RodiTheme {
        VerticalStepList(
            course = HomePreviewData.courses.first(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "DifficultyTag - AllLevels", showBackground = true, widthDp = 360)
@Composable
private fun DifficultyTagAllLevelsPreview() {
    RodiTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(16.dp)) {
            Difficulty.entries.forEach { DifficultyTag(it) }
        }
    }
}

@Preview(name = "RatingRegionRow - Default", showBackground = true, widthDp = 360)
@Composable
private fun RatingRegionRowPreview() {
    RodiTheme {
        RatingRegionRow(rating = 4.5, region = "서울 강동구", onChevronClick = {})
    }
}

@Preview(name = "ExpandableAddressCard - Default", showBackground = true, widthDp = 360)
@Composable
private fun ExpandableAddressCardPreview() {
    RodiTheme {
        ExpandableAddressCard(
            roadAddress = "고덕로 219",
            jibunAddress = "고덕동 200-4",
            modifier = Modifier.padding(16.dp),
        )
    }
}
