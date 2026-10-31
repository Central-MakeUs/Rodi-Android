package com.dororong.rodi.feature.home.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.svg.SvgDecoder
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.ui.text.graphemeLength
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.components.input.RodiFieldLabel
import com.dororong.rodi.feature.home.detail.components.label
import com.dororong.rodi.feature.home.review.ReviewWriteUiState

@Composable
fun ReviewWriteBasics(
    state: ReviewWriteUiState,
    onRecommend: (Boolean) -> Unit,
    onDifficulty: (ReviewDifficulty) -> Unit,
    onCongestion: (ReviewCongestion) -> Unit,
    onCaution: (String) -> Unit,
    onCourseIssue: (String) -> Unit = {},
    onCourseIssueDetail: (String) -> Unit = {},
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
        ReviewChoiceBlock(state.isRecommended, onRecommend, state.issueForm != null)
        if (state.isRecommended == false && state.issueForm != null) {
            CourseIssuesBlock(state.issueForm, state.courseIssues, state.courseIssueDetail, onCourseIssue, onCourseIssueDetail)
        } else if (state.issueForm == null || state.isRecommended == true) {
            ReviewScale("난이도", ReviewDifficulty.entries, state.difficulty, { it.label }, onDifficulty)
            ReviewScale("혼잡도", ReviewCongestion.entries, state.congestion, ReviewCongestion::label, onCongestion)
            CautionBlock(state.caution, onCaution)
        }
    }
}

@Composable
internal fun CourseIssuesBlock(
    form: ReviewIssueForm,
    selected: List<String>,
    detail: String,
    onToggle: (String) -> Unit,
    onDetail: (String) -> Unit,
    showRequiredMarker: Boolean = true,
    selectionRole: Role = Role.Checkbox,
    optionTextStyle: TextStyle? = null,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()
    }
    Column {
        if (showRequiredMarker) {
            RodiFieldLabel(form.title, required = true)
        } else {
            Text(form.title, style = RodiTheme.typography.heading2, color = RodiTheme.colors.black)
        }
        Spacer(Modifier.height(16.dp))
        Column {
            form.options.forEach { option ->
                val isSelected = option.code in selected
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).toggleable(
                            value = isSelected,
                            role = selectionRole,
                            onValueChange = { onToggle(option.code) },
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = if (isSelected) "file:///android_asset/review_issue_selected.svg" else "file:///android_asset/review_issue_default.svg",
                            imageLoader = imageLoader,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(option.label, style = optionTextStyle ?: RodiTheme.typography.body3Medium, color = RodiTheme.colors.black)
                    }
                    if (isSelected && option.requiresTextInput) {
                        val max = option.textInputMaxLength ?: 30
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReviewTextField(detail, onDetail, option.textInputPlaceholder ?: "이유를 작성해주세요.", false, max)
                            Text(
                                text = "${detail.graphemeLength()}/$max",
                                modifier = Modifier.fillMaxWidth(),
                                style = RodiTheme.typography.body3Medium,
                                color = RodiTheme.colors.gray600,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            }
        }
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
        RodiFieldLabel(label, required = true)
        ScalePicker(values, selected, display, onSelect)
    }
}

@Composable
private fun ReviewChoiceBlock(
    selected: Boolean?,
    onSelect: (Boolean) -> Unit,
    hasIssueForm: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RodiFieldLabel("이 코스를 다른 초보 운전자에게 추천하시겠어요?", required = true)
        ChoicePairRow(if (hasIssueForm) "별로에요" else "별로예요", "추천해요", selected, onSelect)
    }
}

@Composable
private fun CautionBlock(
    caution: String,
    onCaution: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RodiFieldLabel("주의사항", required = false)
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
