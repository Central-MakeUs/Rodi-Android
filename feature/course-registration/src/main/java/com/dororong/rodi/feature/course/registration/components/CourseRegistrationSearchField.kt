package com.dororong.rodi.feature.course.registration.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.input.rodiCursorBrush
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
internal fun CourseRegistrationSearchField(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(RodiTheme.colors.gray200, RoundedCornerShape(RodiRadius.sm))
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = "등록 장소 검색" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RodiIconButton(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            onClick = onBack,
            contentDescription = "검색 닫기",
            tint = RodiTheme.colors.black,
        )
        BasicTextField(
            value = keyword,
            onValueChange = onKeywordChanged,
            modifier = Modifier.weight(1f),
            textStyle = RodiTheme.typography.body2Medium.copy(color = RodiTheme.colors.black),
            singleLine = true,
            cursorBrush = rodiCursorBrush(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (keyword.isBlank()) {
                        Text(
                            text = placeholder,
                            style = RodiTheme.typography.body2Medium,
                            color = RodiTheme.colors.gray500,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
