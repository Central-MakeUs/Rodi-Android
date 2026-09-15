package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

/**
 * 빈 화면 기본값. 화면마다 제각각이던 값을 한 곳으로 모은다.
 *
 * 세로 위치를 화면 중앙이 아니라 위에서부터 고정 간격으로 잡는다. 중앙 정렬은 컨테이너
 * 높이(탭·필터 유무, 바텀시트 등)에 따라 화면마다 다른 자리에 놓이기 때문이다.
 */
object RodiEmptyStateDefaults {
    /** 일러스트가 있는 빈 화면의 위 여백. 등록한 코스 디자인(3800:68261) 기준. */
    val IllustratedTopPadding: Dp = 134.dp

    /** 문구만 있는 빈 화면의 위 여백. 최근 검색어 없음 디자인(3128:34250) 기준. */
    val TextOnlyTopPadding: Dp = 180.dp
}

/**
 * 일러스트 + 제목(+설명, +하단 버튼) 형태의 빈 화면.
 *
 * [description]을 비우면 제목만 그린다. 검토중·반려 필터처럼 설명이 없는 화면이 그렇다.
 */
@Composable
fun RodiIllustratedEmptyState(
    painter: Painter,
    imageSize: Dp,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    topPadding: Dp = RodiEmptyStateDefaults.IllustratedTopPadding,
    imageWidth: Dp = imageSize,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.padding(top = topPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(width = imageWidth, height = imageSize),
            )
            Spacer(Modifier.height(RodiSpacing.md))
            Text(
                text = title,
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            description?.let {
                Spacer(Modifier.height(RodiSpacing.sm))
                Text(
                    text = it,
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray600,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            footer()
        }
    }
}

/** 일러스트 없이 문구만 있는 빈 화면. 이 형태끼리 위치를 맞춘다. */
@Composable
fun RodiTextEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    topPadding: Dp = RodiEmptyStateDefaults.TextOnlyTopPadding,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.padding(top = topPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = RodiTheme.typography.body1Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            description?.let {
                Spacer(Modifier.height(RodiSpacing.sm))
                Text(
                    text = it,
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray600,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            footer()
        }
    }
}
