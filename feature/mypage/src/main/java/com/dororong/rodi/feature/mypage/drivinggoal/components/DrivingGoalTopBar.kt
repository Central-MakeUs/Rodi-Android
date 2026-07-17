package com.dororong.rodi.feature.mypage.drivinggoal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R

@Composable
internal fun DrivingGoalTopBar(
    canSave: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            contentDescription = "뒤로가기",
            tint = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Text(
            text = "운전 목표",
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(24.dp)
                .background(
                    color = if (canSave) RodiTheme.colors.primary600 else RodiTheme.colors.primary100,
                    shape = CircleShape,
                )
                .clickable(
                    enabled = canSave,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSave,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "저장",
                tint = RodiTheme.colors.white,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun DrivingGoalTopBarDisabledPreview() {
    RodiTheme {
        DrivingGoalTopBar(canSave = false, onBack = {}, onSave = {})
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun DrivingGoalTopBarEnabledPreview() {
    RodiTheme {
        DrivingGoalTopBar(canSave = true, onBack = {}, onSave = {})
    }
}
