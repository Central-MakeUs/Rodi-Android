package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R
import com.dororong.rodi.core.ui.theme.RodiTheme

enum class RodiBottomNavigationDestination {
    Home,
    CourseRegistration,
    My,
}

@Composable
fun RodiBottomNavigation(
    selectedDestination: RodiBottomNavigationDestination,
    onHomeClick: () -> Unit,
    onCourseRegistrationClick: () -> Unit,
    onMyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RodiTheme.colors.white),
    ) {
        HorizontalDivider(color = RodiTheme.colors.gray100)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            RodiBottomNavigationItem(
                icon = R.drawable.ic_nav_home,
                label = "홈",
                selected = selectedDestination == RodiBottomNavigationDestination.Home,
                onClick = onHomeClick,
            )
            RodiBottomNavigationItem(
                icon = R.drawable.ic_nav_course,
                label = "코스 등록",
                selected = selectedDestination == RodiBottomNavigationDestination.CourseRegistration,
                onClick = onCourseRegistrationClick,
            )
            RodiBottomNavigationItem(
                icon = R.drawable.ic_nav_my,
                label = "마이",
                selected = selectedDestination == RodiBottomNavigationDestination.My,
                onClick = onMyClick,
            )
        }
    }
}

@Composable
private fun RodiBottomNavigationItem(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .padding(top = 8.dp, bottom = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (selected) RodiTheme.colors.black else RodiTheme.colors.gray400,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = RodiTheme.typography.caption1SemiBold,
            color = if (selected) RodiTheme.colors.black else RodiTheme.colors.gray400,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RodiBottomNavigationPreview() {
    RodiTheme {
        RodiBottomNavigation(
            selectedDestination = RodiBottomNavigationDestination.Home,
            onHomeClick = {},
            onCourseRegistrationClick = {},
            onMyClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RodiBottomNavigationCourseRegistrationPreview() {
    RodiTheme {
        RodiBottomNavigation(
            selectedDestination = RodiBottomNavigationDestination.CourseRegistration,
            onHomeClick = {},
            onCourseRegistrationClick = {},
            onMyClick = {},
        )
    }
}
