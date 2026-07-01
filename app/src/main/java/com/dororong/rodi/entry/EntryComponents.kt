package com.dororong.rodi.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dororong.rodi.R
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

private val APP_BAR_HEIGHT = 56.dp
private val BUTTON_HEIGHT = 48.dp
private const val TOTAL_STEPS = 3

/**
 * 진입 플로우 공통 골격: 앱바(56dp, 뒤로) + 진행 인디케이터 + 콘텐츠 + 하단 고정 버튼.
 *
 * @param currentStep 1..3 (진행 인디케이터 채움 개수)
 * @param onBack null 이면 뒤로 버튼 숨김(1단계)
 */
@Composable
fun EntryScaffold(
    currentStep: Int,
    onBack: (() -> Unit)?,
    buttonText: String,
    buttonEnabled: Boolean,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        // 앱바 56dp
        Box(
            Modifier
                .fillMaxWidth()
                .height(APP_BAR_HEIGHT)
                .padding(horizontal = RodiSpacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (onBack != null) {
                // 좌향 화살표 에셋 부재 → chevron_right 180° 미러로 임시 사용
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = "뒤로",
                    tint = RodiTheme.colors.gray900,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .rotate(180f),
                )
            }
        }
        // 진행 인디케이터: top 4, 좌우 16, 세그먼트 gap 2, bottom 32
        StepProgressIndicator(
            currentStep = currentStep,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RodiSpacing.md)
                .padding(top = RodiSpacing.xs),
        )
        Spacer(Modifier.height(RodiSpacing.xl)) // bottom 32

        // 콘텐츠 영역
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = RodiSpacing.md),
            content = content,
        )

        // 하단 고정 버튼 48dp + 상하 10dp
        PrimaryButton(
            text = buttonText,
            enabled = buttonEnabled,
            onClick = onButtonClick,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = RodiSpacing.md, vertical = 10.dp),
        )
    }
}

@Composable
fun StepProgressIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(TOTAL_STEPS) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < currentStep) RodiTheme.colors.primary600 else RodiTheme.colors.gray300),
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT),
        shape = RoundedCornerShape(RodiRadius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = RodiTheme.colors.primary600,
            contentColor = RodiTheme.colors.white,
            disabledContainerColor = RodiTheme.colors.gray300,
            disabledContentColor = RodiTheme.colors.gray500,
        ),
    ) {
        Text(text, style = RodiTheme.typography.button1)
    }
}

/**
 * 체크 행: 체크 아이콘(미선택 gray300 / 선택 primary600) + 라벨 + 선택적 chevron.
 */
@Composable
fun CheckRow(
    checked: Boolean,
    text: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = RodiTheme.colors.black,
    trailingChevron: Boolean = false,
    onChevronClick: (() -> Unit)? = null,
) {
    val checkInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                interactionSource = checkInteractionSource,
                indication = null,
                onValueChange = { onToggle() },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = if (checked) RodiTheme.colors.primary600 else RodiTheme.colors.gray300,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(RodiSpacing.sm))
        Text(text, style = RodiTheme.typography.body1Medium, color = textColor, modifier = Modifier.weight(1f))
        if (trailingChevron) {
            IconButton(
                onClick = { onChevronClick?.invoke() },
                enabled = onChevronClick != null,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = "상세 보기",
                    tint = RodiTheme.colors.gray500,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
