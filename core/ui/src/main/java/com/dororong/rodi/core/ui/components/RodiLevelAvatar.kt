package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.ui.R
import com.dororong.rodi.core.ui.theme.RodiTheme

/**
 * 레벨별 캐릭터 프로필 이미지. 레벨을 모르면(탈퇴·익명화 등) 빈 원만 그린다.
 */
@Composable
fun RodiLevelAvatar(
    level: OnboardingLevel?,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(RodiTheme.colors.primary100, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        level?.let {
            Image(
                painter = painterResource(it.profileImageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}

/** 마이페이지 프로필 카드와 후기 아바타가 함께 쓰는 레벨별 캐릭터 이미지. */
val OnboardingLevel.profileImageRes: Int
    get() = when (this) {
        OnboardingLevel.SEED -> R.drawable.illust_profile_seed
        OnboardingLevel.ROOKIE -> R.drawable.illust_profile_rookie
        OnboardingLevel.OWNER -> R.drawable.illust_profile_owner
        OnboardingLevel.EXPLORER -> R.drawable.illust_profile_explorer
        OnboardingLevel.NAVIGATOR -> R.drawable.illust_profile_navigator
    }

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RodiLevelAvatarPreview() {
    RodiTheme {
        Row(Modifier.padding(16.dp)) {
            OnboardingLevel.entries.forEach {
                RodiLevelAvatar(level = it, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}

@Preview(name = "레벨 없음", showBackground = true, widthDp = 360)
@Composable
private fun RodiLevelAvatarUnknownPreview() {
    RodiTheme {
        Row(Modifier.padding(16.dp)) {
            RodiLevelAvatar(level = null)
        }
    }
}
