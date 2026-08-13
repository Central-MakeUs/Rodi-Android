package com.dororong.rodi.feature.home.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.ui.theme.RodiTheme

enum class FilterCategory(
    val label: String,
    val practiceOptions: List<FilterPracticeOption>,
) {
    BASIC_DRIVING(
        label = "기초 주행",
        practiceOptions = listOf(
            FilterPracticeOption.STRAIGHT,
            FilterPracticeOption.LEFT_RIGHT_TURN,
            FilterPracticeOption.LANE_CHANGE,
            FilterPracticeOption.ALL,
        ),
    ),
    URBAN_BASICS(
        label = "도심 기본",
        practiceOptions = listOf(
            FilterPracticeOption.INTERSECTION,
            FilterPracticeOption.U_TURN,
            FilterPracticeOption.ALL,
        ),
    ),
    PARKING(
        label = "주차",
        practiceOptions = emptyList(),
    ),
    ROAD_FLOW(
        label = "도로 흐름",
        practiceOptions = listOf(
            FilterPracticeOption.MULTILANE,
            FilterPracticeOption.MERGING,
            FilterPracticeOption.HIGHWAY_ENTRY,
            FilterPracticeOption.ALL,
        ),
    ),
    COMPLEX_SITUATIONS(
        label = "복합 상황",
        practiceOptions = listOf(
            FilterPracticeOption.ROUNDABOUT,
            FilterPracticeOption.UNPROTECTED_LEFT_TURN,
            FilterPracticeOption.NARROW_ROAD,
            FilterPracticeOption.CORNERING,
            FilterPracticeOption.ALL,
        ),
    ),
}

enum class FilterPracticeOption(
    val label: String,
    val practiceType: PracticeType?,
) {
    STRAIGHT("직선주행", PracticeType.STRAIGHT),
    LEFT_RIGHT_TURN("좌우회전", PracticeType.LEFT_RIGHT_TURN),
    LANE_CHANGE("차선변경", PracticeType.LANE_CHANGE),
    INTERSECTION("교차로", PracticeType.INTERSECTION),
    U_TURN("유턴", PracticeType.U_TURN),
    MULTILANE("다차로주행", PracticeType.MULTILANE),
    MERGING("합류", PracticeType.MERGING),
    HIGHWAY_ENTRY("고속도로", PracticeType.HIGHWAY_ENTRY),
    ROUNDABOUT("회전교차로", PracticeType.ROUNDABOUT),
    UNPROTECTED_LEFT_TURN("비보호좌회전", PracticeType.UNPROTECTED_LEFT_TURN),
    NARROW_ROAD("좁은도로", PracticeType.NARROW_ROAD),
    CORNERING("코너링", PracticeType.CORNERING),
    ALL("전체", null),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    activeCategory: FilterCategory?,
    selectedPracticeTypes: Set<PracticeType>,
    onCategorySelect: (FilterCategory) -> Unit,
    onPracticeOptionToggle: (FilterPracticeOption) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = RodiTheme.colors.white,
        contentColor = RodiTheme.colors.black,
        tonalElevation = 0.dp,
        scrimColor = RodiTheme.colors.black.copy(alpha = 0.5f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        FilterBottomSheetContent(
            activeCategory = activeCategory,
            selectedPracticeTypes = selectedPracticeTypes,
            onCategorySelect = onCategorySelect,
            onPracticeOptionToggle = onPracticeOptionToggle,
            onReset = onReset,
            onApply = onApply,
            isSaving = isSaving,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheetContent(
    activeCategory: FilterCategory?,
    selectedPracticeTypes: Set<PracticeType>,
    onCategorySelect: (FilterCategory) -> Unit,
    onPracticeOptionToggle: (FilterPracticeOption) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    isSaving: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(401.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(width = 60.dp, height = 4.dp)
                .background(RodiTheme.colors.handleBar, RoundedCornerShape(3.dp)),
        )
        Text(
            text = "필터",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 22.dp),
        )
        Text(
            text = "카테고리",
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 65.dp),
        )
        FlowRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, end = 16.dp, top = 98.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterCategory.entries.forEach { category ->
                FilterChip(
                    label = category.label,
                    selected = category == activeCategory,
                    onClick = { onCategorySelect(category) },
                    enabled = !isSaving,
                )
            }
        }
        activeCategory?.takeIf { it.practiceOptions.isNotEmpty() }?.let { category ->
            Text(
                text = "연습유형",
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 186.dp),
            )
            FlowRow(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, end = 16.dp, top = 219.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                category.practiceOptions.forEach { option ->
                    FilterChip(
                        label = option.label,
                        selected = option.isSelected(category, selectedPracticeTypes),
                        onClick = { onPracticeOptionToggle(option) },
                        enabled = !isSaving,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            HorizontalDivider(color = RodiTheme.colors.gray200)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(69.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                FilterActionButton(
                    text = "초기화",
                    containerColor = RodiTheme.colors.white,
                    borderColor = RodiTheme.colors.gray300,
                    textColor = RodiTheme.colors.gray800,
                    onClick = onReset,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                )
                FilterActionButton(
                    text = "결과보기",
                    containerColor = RodiTheme.colors.primary600,
                    borderColor = Color.Transparent,
                    textColor = RodiTheme.colors.white,
                    onClick = onApply,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(34.dp))
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) RodiTheme.colors.primary600 else RodiTheme.colors.primary200
    val background = if (selected) RodiTheme.colors.primary100 else RodiTheme.colors.white
    val textColor = if (selected) RodiTheme.colors.primary800 else RodiTheme.colors.gray600
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(shape)
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = RodiTheme.typography.body3Medium,
            color = textColor,
        )
    }
}

@Composable
private fun FilterActionButton(
    text: String,
    containerColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .border(1.dp, borderColor, shape)
            .background(containerColor, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = RodiTheme.typography.button1,
            color = textColor,
        )
    }
}

@Preview(name = "Filter - basic driving", showBackground = true, widthDp = 375, heightDp = 401)
@Composable
private fun BasicDrivingFilterPreview() {
    RodiTheme {
        FilterBottomSheetContent(
            activeCategory = FilterCategory.BASIC_DRIVING,
            selectedPracticeTypes = setOf(PracticeType.LEFT_RIGHT_TURN, PracticeType.INTERSECTION),
            onCategorySelect = {},
            onPracticeOptionToggle = {},
            onReset = {},
            onApply = {},
            isSaving = false,
        )
    }
}

@Preview(name = "Filter - no category", showBackground = true, widthDp = 375, heightDp = 401)
@Composable
private fun NoCategoryFilterPreview() {
    RodiTheme {
        FilterBottomSheetContent(
            activeCategory = null,
            selectedPracticeTypes = setOf(PracticeType.STRAIGHT, PracticeType.PARKING),
            onCategorySelect = {},
            onPracticeOptionToggle = {},
            onReset = {},
            onApply = {},
            isSaving = false,
        )
    }
}

@Preview(name = "Filter - parking", showBackground = true, widthDp = 375, heightDp = 401)
@Composable
private fun ParkingFilterPreview() {
    RodiTheme {
        FilterBottomSheetContent(
            activeCategory = FilterCategory.PARKING,
            selectedPracticeTypes = setOf(PracticeType.PARKING),
            onCategorySelect = {},
            onPracticeOptionToggle = {},
            onReset = {},
            onApply = {},
            isSaving = false,
        )
    }
}

@Preview(name = "Filter - complex situations", showBackground = true, widthDp = 375, heightDp = 401)
@Composable
private fun ComplexSituationsFilterPreview() {
    RodiTheme {
        FilterBottomSheetContent(
            activeCategory = FilterCategory.COMPLEX_SITUATIONS,
            selectedPracticeTypes = setOf(PracticeType.CORNERING, PracticeType.STRAIGHT),
            onCategorySelect = {},
            onPracticeOptionToggle = {},
            onReset = {},
            onApply = {},
            isSaving = false,
        )
    }
}

internal fun FilterCategory.practiceTypes(): Set<PracticeType> =
    practiceOptions.mapNotNull(FilterPracticeOption::practiceType).toSet()

private fun FilterPracticeOption.isSelected(
    category: FilterCategory,
    selectedPracticeTypes: Set<PracticeType>,
): Boolean = when (this) {
    FilterPracticeOption.ALL -> category.practiceTypes().all(selectedPracticeTypes::contains)
    else -> practiceType in selectedPracticeTypes
}
