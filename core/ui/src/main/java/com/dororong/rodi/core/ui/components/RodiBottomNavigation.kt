package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    My,
}

@Composable
fun RodiBottomNavigation(
    selectedDestination: RodiBottomNavigationDestination,
    onHomeClick: () -> Unit,
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
            horizontalArrangement = Arrangement.Center,
        ) {
            RodiBottomNavigationItem(
                icon = R.drawable.ic_nav_home,
                label = "홈",
                selected = selectedDestination == RodiBottomNavigationDestination.Home,
                onClick = onHomeClick,
            )
            Spacer(Modifier.width(68.dp))
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
            .clickable(onClick = onClick)
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
            onMyClick = {},
        )
    }
}
