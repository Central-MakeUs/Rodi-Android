package com.dororong.rodi.feature.mypage.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.dialog.RodiDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R

@Composable
fun LevelUpDialog(
    level: OnboardingLevel,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    RodiDialog(onDismissRequest = onDismissRequest, modifier = Modifier.fillMaxWidth(0.82f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Level Up!", style = RodiTheme.typography.headline1, color = RodiTheme.colors.black)
            Text("레벨업 했어요,\n앞으로도 새로운 코스에 도전해보세요!", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600, textAlign = TextAlign.Center)
            Image(painterResource(level.characterImageRes), null, modifier = Modifier.size(120.dp))
            Text(level.displayName, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.primary600, modifier = Modifier.background(RodiTheme.colors.primary100, RoundedCornerShape(100)).padding(horizontal = 12.dp, vertical = 5.dp))
            RodiButton(text = "확인", onClick = onConfirm, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

private val OnboardingLevel.displayName: String get() = name.lowercase().replaceFirstChar { it.titlecase() }
private val OnboardingLevel.characterImageRes: Int get() = when (this) {
    OnboardingLevel.SEED -> R.drawable.illust_profile_seed
    OnboardingLevel.ROOKIE -> R.drawable.illust_profile_rookie
    OnboardingLevel.OWNER -> R.drawable.illust_profile_owner
    OnboardingLevel.EXPLORER -> R.drawable.illust_profile_explorer
    OnboardingLevel.NAVIGATOR -> R.drawable.illust_profile_navigator
}

@Preview(name = "레벨업 Seed", showBackground = true)
@Composable private fun LevelUpSeedPreview() = RodiTheme { LevelUpDialog(OnboardingLevel.SEED, {}, {}) }
@Preview(name = "레벨업 Rookie", showBackground = true)
@Composable private fun LevelUpRookiePreview() = RodiTheme { LevelUpDialog(OnboardingLevel.ROOKIE, {}, {}) }
@Preview(name = "레벨업 Owner", showBackground = true)
@Composable private fun LevelUpOwnerPreview() = RodiTheme { LevelUpDialog(OnboardingLevel.OWNER, {}, {}) }
@Preview(name = "레벨업 Explorer", showBackground = true)
@Composable private fun LevelUpExplorerPreview() = RodiTheme { LevelUpDialog(OnboardingLevel.EXPLORER, {}, {}) }
@Preview(name = "레벨업 Navigator", showBackground = true)
@Composable private fun LevelUpNavigatorPreview() = RodiTheme { LevelUpDialog(OnboardingLevel.NAVIGATOR, {}, {}) }
