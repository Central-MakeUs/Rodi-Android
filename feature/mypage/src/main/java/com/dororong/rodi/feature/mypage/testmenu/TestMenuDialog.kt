package com.dororong.rodi.feature.mypage.testmenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.dialog.RodiDialog
import com.dororong.rodi.core.ui.theme.RodiTheme

/** 실행 결과 문구를 돌려주면 메뉴가 그대로 보여 준다. 보여줄 게 없으면 null. */
@Immutable
data class TestMenuAction(
    val label: String,
    val run: () -> String?,
)

@Immutable
data class TestMenuSection(
    val title: String,
    val actions: List<TestMenuAction>,
)

@Composable
internal fun TestMenuDialog(
    sections: List<TestMenuSection>,
    onDismissRequest: () -> Unit,
) {
    var selectedSection by remember { mutableStateOf<TestMenuSection?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    RodiDialog(onDismissRequest = onDismissRequest, showCloseButton = true) {
        val section = selectedSection
        val message = result
        Text(
            text = section?.title ?: "테스트",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        when {
            message != null -> {
                Text(
                    text = message,
                    style = RodiTheme.typography.body3Regular,
                    color = RodiTheme.colors.gray800,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                )
                Spacer(Modifier.height(16.dp))
                RodiButton(text = "확인", onClick = { result = null })
            }
            section != null -> {
                TestMenuRows(
                    labels = section.actions.map { it.label },
                    onClick = { index -> result = section.actions[index].run() },
                )
                Spacer(Modifier.height(16.dp))
                RodiButton(
                    text = "뒤로",
                    onClick = { selectedSection = null },
                    variant = RodiButtonVariant.Secondary,
                )
            }
            else -> TestMenuRows(
                labels = sections.map { it.title },
                onClick = { index -> selectedSection = sections[index] },
            )
        }
    }
}

@Composable
private fun TestMenuRows(
    labels: List<String>,
    onClick: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            if (index > 0) HorizontalDivider(color = RodiTheme.colors.gray100)
            Text(
                text = label,
                style = RodiTheme.typography.body1Medium,
                color = RodiTheme.colors.black,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(index) }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

private val previewSections = listOf(
    TestMenuSection(
        title = "라이브 업데이트",
        actions = listOf(
            TestMenuAction("연습 코스로 이동 중") { null },
            TestMenuAction("코스 주행 중") { null },
            TestMenuAction("진단 정보") { "Android 16 (36.1)\n승격 허용: 예" },
        ),
    ),
)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TestMenuDialogPreview() {
    RodiTheme {
        TestMenuDialog(sections = previewSections, onDismissRequest = {})
    }
}
