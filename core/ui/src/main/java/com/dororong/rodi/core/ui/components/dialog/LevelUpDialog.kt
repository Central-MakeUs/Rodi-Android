package com.dororong.rodi.core.ui.components.dialog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.ui.R
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.shimmerSpec

private val LEVEL_UP_DIALOG_HEIGHT = 388.dp
private val LEVEL_UP_DIALOG_SCREEN_MARGIN = 32.dp

@Composable
fun LevelUpDialog(
    level: OnboardingLevel,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val availableDialogHeight = (LocalConfiguration.current.screenHeightDp.dp - LEVEL_UP_DIALOG_SCREEN_MARGIN)
        .coerceAtLeast(0.dp)
    RodiDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(290.dp)
            .heightIn(
                min = minOf(LEVEL_UP_DIALOG_HEIGHT, availableDialogHeight),
                max = availableDialogHeight,
            ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Level Up!",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
                modifier = Modifier.padding(top = 32.dp),
            )
            Text(
                text = "레벨업 했어요,\n앞으로도 새로운 코스에 도전해보세요!",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Image(
                painter = painterResource(level.characterImageRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(150.dp),
            )
            LevelUpLevelChip(level.displayName)
            RodiButton(
                text = "확인",
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 15.dp, end = 16.dp),
                height = 54.dp,
            )
        }
    }
}

@Composable
private fun LevelUpLevelChip(label: String) {
    val shimmerTheme = LocalShimmerTheme.current.copy(
        animationSpec = infiniteRepeatable(
            animation = shimmerSpec(
                durationMillis = 1_100,
                delayMillis = 350,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        shimmerWidth = 96.dp,
    )

    CompositionLocalProvider(LocalShimmerTheme provides shimmerTheme) {
        Box(
            modifier = Modifier.padding(top = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            RodiSkeleton(
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(4.dp),
                color = RodiTheme.colors.primary200,
                shimmerColors = listOf(
                    RodiTheme.colors.primary200,
                    RodiTheme.colors.primary100,
                    RodiTheme.colors.primary200,
                ),
            )
            Text(
                text = label,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.primary600,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private val OnboardingLevel.displayName: String
    get() = name.lowercase().replaceFirstChar { it.titlecase() }

private val OnboardingLevel.characterImageRes: Int
    get() = when (this) {
        OnboardingLevel.SEED -> R.drawable.illust_level_seed
        OnboardingLevel.ROOKIE -> R.drawable.illust_level_rookie
        OnboardingLevel.OWNER -> R.drawable.illust_level_owner
        OnboardingLevel.EXPLORER -> R.drawable.illust_level_explorer
        OnboardingLevel.NAVIGATOR -> R.drawable.illust_level_navigator
    }

@Preview(name = "레벨업 Seed", showBackground = true, widthDp = 360)
@Composable
private fun LevelUpSeedPreview() = RodiTheme { LevelUpDialog(OnboardingLevel.SEED, {}, {}) }

@Preview(name = "레벨업 Rookie", showBackground = true, widthDp = 360)
@Composable
private fun LevelUpRookiePreview() = RodiTheme { LevelUpDialog(OnboardingLevel.ROOKIE, {}, {}) }

@Preview(name = "레벨업 Owner", showBackground = true, widthDp = 360)
@Composable
private fun LevelUpOwnerPreview() = RodiTheme { LevelUpDialog(OnboardingLevel.OWNER, {}, {}) }

@Preview(name = "레벨업 Explorer", showBackground = true, widthDp = 360)
@Composable
private fun LevelUpExplorerPreview() = RodiTheme { LevelUpDialog(OnboardingLevel.EXPLORER, {}, {}) }

@Preview(name = "레벨업 Navigator", showBackground = true, widthDp = 360)
@Composable
private fun LevelUpNavigatorPreview() = RodiTheme { LevelUpDialog(OnboardingLevel.NAVIGATOR, {}, {}) }
