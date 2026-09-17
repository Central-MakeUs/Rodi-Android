package com.dororong.rodi.feature.mypage.registeredcourses.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.dialog.RodiDialog
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
internal fun RegisteredCourseDeleteDialog(
    status: CourseApprovalStatus = CourseApprovalStatus.APPROVED,
    enabled: Boolean = true,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    RodiDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(280.dp)
            .height(226.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        dismissible = false,
    ) {
        Text(
            text = "정말 삭제하시겠습니까?",
            modifier = Modifier.fillMaxWidth(),
            style = RodiTheme.typography.price1,
            color = RodiTheme.colors.black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when (status) {
                    CourseApprovalStatus.APPROVED ->
                        "이 코스는 다른 초보운전자에게도 도움이 되고 있어요. 삭제하면 더 이상 공개되지 않아요."
                    CourseApprovalStatus.PENDING ->
                        "현재 검토 중인 코스예요.\n삭제하면 코스 검토가 중단돼요."
                    CourseApprovalStatus.REJECTED ->
                        "삭제하면 해당 코스를 내 활동에서 더 이상 확인할 수 없어요."
                },
                modifier = Modifier.fillMaxWidth(),
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RodiButton(
                text = "삭제하기",
                onClick = onDelete,
                modifier = Modifier.width(116.dp),
                variant = RodiButtonVariant.Secondary,
                enabled = enabled,
                fillMaxWidth = false,
                height = 42.dp,
            )
            RodiButton(
                text = "취소",
                onClick = onDismiss,
                modifier = Modifier.width(116.dp),
                enabled = enabled,
                fillMaxWidth = false,
                height = 42.dp,
            )
        }
    }
}
