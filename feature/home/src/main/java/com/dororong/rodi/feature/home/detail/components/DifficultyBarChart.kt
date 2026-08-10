package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.ui.theme.RodiTheme

private val ChipShape = RoundedCornerShape(2.dp)
private val BarShape = RoundedCornerShape(6.dp)

@Composable
fun DifficultyBarChart(
    counts: Map<ReviewDifficulty, Long>,
    modifier: Modifier = Modifier,
) {
    val topCount = counts.values.maxOrNull()?.takeIf { it > 0L }
    val max = topCount ?: 1L

    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ReviewDifficulty.entries.forEach { difficulty ->
            val count = counts[difficulty] ?: 0L
            DifficultyBarRow(
                difficulty = difficulty,
                count = count,
                fraction = count.toFloat() / max.toFloat(),
                emphasized = count == topCount,
            )
        }
    }
}

@Composable
private fun DifficultyBarRow(
    difficulty: ReviewDifficulty,
    count: Long,
    fraction: Float,
    emphasized: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DifficultyChip(difficulty = difficulty, emphasized = emphasized)
            Text(
                text = "${count}명",
                style = if (emphasized) {
                    RodiTheme.typography.body1SemiBold
                } else {
                    RodiTheme.typography.body3Medium
                },
                color = if (emphasized) RodiTheme.colors.black else RodiTheme.colors.gray600,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(RodiTheme.colors.gray300, BarShape),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(
                            color = if (emphasized) {
                                RodiTheme.colors.primary400
                            } else {
                                RodiTheme.colors.gray500
                            },
                            shape = BarShape,
                        ),
                )
            }
        }
    }
}

@Composable
internal fun DifficultyChip(
    difficulty: ReviewDifficulty,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = difficulty.label,
        style = RodiTheme.typography.caption1Medium,
        color = if (emphasized) RodiTheme.colors.primary600 else RodiTheme.colors.gray600,
        modifier = modifier
            .background(
                color = if (emphasized) RodiTheme.colors.primary100 else RodiTheme.colors.gray200,
                shape = ChipShape,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Preview(name = "난이도 분포 - 데이터 있음", showBackground = true, widthDp = 375)
@Composable
private fun DifficultyBarChartPreview() {
    RodiTheme {
        DifficultyBarChart(
            counts = mapOf(
                ReviewDifficulty.VERY_EASY to 30L,
                ReviewDifficulty.EASY to 26L,
                ReviewDifficulty.NORMAL to 5L,
                ReviewDifficulty.HARD to 5L,
                ReviewDifficulty.VERY_HARD to 5L,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "난이도 분포 - 데이터 없음", showBackground = true, widthDp = 375)
@Composable
private fun DifficultyBarChartEmptyPreview() {
    RodiTheme {
        DifficultyBarChart(counts = emptyMap(), modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "난이도 분포 - 공동 1위", showBackground = true, widthDp = 375)
@Composable
private fun DifficultyBarChartTiePreview() {
    RodiTheme {
        DifficultyBarChart(
            counts = mapOf(
                ReviewDifficulty.EASY to 12L,
                ReviewDifficulty.VERY_HARD to 12L,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
