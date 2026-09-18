package com.dororong.rodi.feature.mypage.registeredcourses.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.RodiIllustratedEmptyState
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R
import com.dororong.rodi.feature.mypage.registeredcourses.RegisteredCourseFilter
import kotlinx.coroutines.flow.filter

@Composable
internal fun RegisteredCoursesLoading() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        repeat(5) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RodiSkeleton(modifier = Modifier.width(144.dp).height(18.dp))
                    Spacer(Modifier.weight(1f))
                    RodiSkeleton(modifier = Modifier.size(28.dp), shape = RoundedCornerShape(14.dp))
                }
                RodiSkeleton(modifier = Modifier.padding(top = 8.dp).width(72.dp).height(13.dp))
                RodiSkeleton(
                    modifier = Modifier.padding(top = 12.dp).width(60.dp).height(22.dp),
                    shape = RoundedCornerShape(12.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = RodiTheme.colors.gray100)
            }
        }
    }
}

@Composable
internal fun RegisteredCoursesEmpty(
    filter: RegisteredCourseFilter,
    onRegisterCourseClick: () -> Unit,
) {
    val isAll = filter == RegisteredCourseFilter.ALL
    RodiIllustratedEmptyState(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        painter = painterResource(R.drawable.illust_registered_course_empty),
        imageWidth = 125.dp,
        imageSize = 50.dp,
        title = when (filter) {
            RegisteredCourseFilter.ALL -> "아직 등록한 코스가 없어요!"
            RegisteredCourseFilter.APPROVED -> "승인된 코스가 없어요!"
            RegisteredCourseFilter.PENDING -> "검토중인 코스가 없어요!"
            RegisteredCourseFilter.REJECTED -> "반려된 코스가 없어요!"
        },
        // 자동 줄바꿈에 맡기면 "좋은 코/스를"처럼 단어 중간에서 끊긴다.
        description = "나만 알고 있는 운전 연습하기 좋은\n코스를 공유해보세요.".takeIf { isAll },
        footer = {
            if (isAll) {
                OutlinedButton(
                    onClick = onRegisterCourseClick,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(147.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RodiTheme.colors.primary600),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RodiTheme.colors.primary600,
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = "코스 등록하기",
                        style = RodiTheme.typography.body3Medium,
                    )
                }
            }
        },
    )
}
