package com.dororong.rodi.feature.home.detail.coursereport

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewIssueOption
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.review.components.CourseIssuesBlock

@Composable
internal fun CourseReportContent(
    form: ReviewIssueForm,
    selected: List<String>,
    detail: String,
    isSubmissionEnabled: Boolean,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onDetail: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(Modifier.statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                RodiIconButton(
                    painter = painterResource(CoreUiR.drawable.ic_chevron_left),
                    onClick = onBack,
                    iconSize = 24.dp,
                    contentDescription = "뒤로가기",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
                )
                Text(
                    text = "신고하기",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                CourseIssuesBlock(
                    form = form,
                    selected = selected,
                    detail = detail,
                    onToggle = onToggle,
                    onDetail = onDetail,
                    useHeadingStyle = true,
                    optionTextStyle = RodiTheme.typography.body1Medium,
                )
            }
            HorizontalDivider(color = RodiTheme.colors.gray200)
            RodiButton(
                text = "제출",
                onClick = onSubmit,
                enabled = isSubmissionEnabled,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

private val previewForm = ReviewIssueForm(
    title = "해당 코스에 어떤 문제가 있나요?",
    options = listOf(
        ReviewIssueOption("1", "차량 진입이 제한된 장소예요", false, null, null),
        ReviewIssueOption("2", "도로 정보가 실제와 달라요", false, null, null),
        ReviewIssueOption("3", "공사·통제 등으로 현재 주행할 수 없어요", false, null, null),
        ReviewIssueOption("4", "연습 코스로 적합하지 않아요", false, null, null),
        ReviewIssueOption("5", "기타", true, "이유를 작성해주세요.", 30),
    ),
)

@Preview(name = "코스 신고 - 기타 입력", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseReportContentPreview() {
    RodiTheme {
        CourseReportContent(
            form = previewForm,
            selected = listOf("5"),
            detail = "",
            isSubmissionEnabled = false,
            onBack = {}, onToggle = {}, onDetail = {}, onSubmit = {},
        )
    }
}
