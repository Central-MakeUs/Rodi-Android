package com.dororong.rodi.feature.entry.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.entry.R
import com.dororong.rodi.core.ui.R as CoreUiR

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
                    painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                    contentDescription = "상세 보기",
                    tint = RodiTheme.colors.gray500,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
