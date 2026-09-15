package com.dororong.rodi.feature.home.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.detail.components.label
import com.dororong.rodi.feature.home.review.ReviewWriteUiState

@Composable
fun ReviewWriteBasics(
    state: ReviewWriteUiState,
    onRecommend: (Boolean) -> Unit,
    onDifficulty: (ReviewDifficulty) -> Unit,
    onCongestion: (ReviewCongestion) -> Unit,
    onCaution: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Text(
            text = reviewHeading(state.placeName),
            style = RodiTheme.typography.heading2,
        )
        ReviewChoiceBlock(state.isRecommended, onRecommend)
        ReviewScale("난이도", ReviewDifficulty.entries, state.difficulty, { it.label }, onDifficulty)
        ReviewScale("혼잡도", ReviewCongestion.entries, state.congestion, ReviewCongestion::label, onCongestion)
        CautionBlock(state.caution, onCaution)
    }
}

@Composable
private fun reviewHeading(placeName: String): AnnotatedString = buildAnnotatedString {
    withStyle(RodiTheme.typography.heading2.toSpanStyle().copy(color = RodiTheme.colors.primary600)) {
        append(placeName)
    }
    withStyle(RodiTheme.typography.heading2.toSpanStyle().copy(color = RodiTheme.colors.black)) {
        append("의\n연습은 어땠나요?")
    }
}

@Composable
private fun <T> ReviewScale(
    label: String,
    values: List<T>,
    selected: T?,
    display: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
        ScalePicker(values, selected, display, onSelect)
    }
}

@Composable
private fun ReviewChoiceBlock(
    selected: Boolean?,
    onSelect: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "이 코스를 다른 초보 운전자에게 추천하시겠어요?",
            modifier = Modifier.height(25.dp),
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
        )
        ChoicePairRow("별로예요", "추천해요", selected, onSelect)
    }
}

@Composable
private fun CautionBlock(
    caution: String,
    onCaution: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("주의사항", style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
        ReviewTextField(
            value = caution,
            onValueChange = onCaution,
            placeholder = "예) 갑자기 나오는 자전거 주의!",
            multiline = false,
            maxGraphemes = 50,
        )
    }
}

internal fun ReviewCongestion.label(): String = when (this) {
    ReviewCongestion.QUIET -> "한산해요"
    ReviewCongestion.NORMAL -> "보통이에요"
    ReviewCongestion.CROWDED -> "복잡해요"
}

@Preview(name = "후기 기본 - 미선택", showBackground = true, widthDp = 375)
@Composable
private fun ReviewWriteBasicsEmptyPreview() = RodiTheme {
    ReviewWriteBasics(ReviewWriteUiState(placeName = "강남역 코스"), {}, {}, {}, {})
}

@Preview(name = "후기 기본 - 선택", showBackground = true, widthDp = 375)
@Composable
private fun ReviewWriteBasicsSelectedPreview() = RodiTheme {
    ReviewWriteBasics(
        ReviewWriteUiState(
            placeName = "강남역 코스",
            isRecommended = true,
            difficulty = ReviewDifficulty.NORMAL,
            congestion = ReviewCongestion.QUIET,
        ),
        {}, {}, {}, {},
    )
}

@Preview(name = "후기 기본 - 주의사항", showBackground = true, widthDp = 375)
@Composable
private fun ReviewWriteBasicsCautionPreview() = RodiTheme {
    ReviewWriteBasics(
        ReviewWriteUiState(placeName = "강남역 코스", caution = "자전거가 자주 지나가요."),
        {}, {}, {}, {},
    )
}
