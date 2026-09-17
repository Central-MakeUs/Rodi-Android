package com.dororong.rodi.feature.course.registration.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.CourseLocationKind
import com.dororong.rodi.core.domain.model.course.CourseLocationSearchResult
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.ui.components.RodiIllustratedEmptyState
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.RodiTextEmptyState
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.R
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationSearchField

@Composable
fun CourseRegistrationSearchContent(
    keyword: String,
    isLoading: Boolean,
    isRecentLoading: Boolean = false,
    result: CourseLocationSearchResult,
    error: String?,
    onBack: () -> Unit,
    onKeywordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onSelect: (String) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding()
            .imePadding(),
    ) {
        CourseRegistrationSearchField(
            keyword = keyword,
            onKeywordChanged = onKeywordChanged,
            onSubmit = onSubmit,
            placeholder = "장소 · 도로명 검색",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        )
        if (isLoading) {
            SearchLoadingContent()
        } else if (error != null) {
            SearchMessage(
                text = error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (keyword.isBlank()) {
            RecentSearchContent(
                recent = result.recent,
                isLoading = isRecentLoading,
                onSelect = onSelect,
                onDelete = onDeleteRecent,
                onDeleteAll = onDeleteAll,
            )
        } else if (result.regions.isEmpty() && result.places.isEmpty()) {
            SearchNoResultContent(keyword = keyword)
        } else {
            SearchResultsContent(result = result, onSelect = onSelect)
        }
    }
}

@Composable
internal fun SearchLoadingContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = RodiSpacing.md, vertical = RodiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(4) {
            RodiSkeleton(Modifier.fillMaxWidth().height(58.dp), RoundedCornerShape(RodiRadius.sm))
        }
        item { HorizontalDivider(color = RodiTheme.colors.gray100, thickness = 8.dp) }
        items(3) {
            RodiSkeleton(Modifier.fillMaxWidth().height(58.dp), RoundedCornerShape(RodiRadius.sm))
        }
    }
}

@Composable
internal fun RecentSearchContent(
    recent: List<CourseLocationSuggestion>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // 최근 검색어가 없을 때는 헤더 행("최근 검색어" + 전체삭제) 자체를 그리지 않는다 —
        // 홈 검색(SearchScreen.kt의 RecentSearchList)과 같은 빈 상태를 보여줘야 한다.
        // 로딩 중에는 아직 있는지 없는지 모르므로 헤더를 그리지 않고 스켈레톤만 보여준다.
        if (!isLoading && recent.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = RodiSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("최근 검색어", style = RodiTheme.typography.caption2Medium, color = RodiTheme.colors.gray700)
                // 글자 높이만큼만 클릭 영역이 잡히지 않도록 헤더 행 전체 높이로 넓힌다.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(onClick = onDeleteAll)
                        .semantics {
                            contentDescription = "최근 검색어 전체 삭제"
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "전체삭제",
                        style = RodiTheme.typography.caption2Medium,
                        color = RodiTheme.colors.gray500,
                    )
                }
            }
        }
        if (isLoading) {
            RecentSearchLoadingContent()
        } else if (recent.isEmpty()) {
            RodiTextEmptyState(
                modifier = Modifier.fillMaxSize(),
                title = "최근 검색 내역이 없습니다",
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(recent, key = CourseLocationSuggestion::id) { item ->
                    SearchRow(item = item, onSelect = onSelect, onDelete = { onDelete(item.id) })
                }
            }
        }
    }
}

@Composable
internal fun RecentSearchLoadingContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = RodiSpacing.md),
    ) {
        items(5) {
            RodiSkeleton(Modifier.fillMaxWidth().height(46.dp), RoundedCornerShape(RodiRadius.sm))
        }
    }
}

@Composable
internal fun SearchNoResultContent(keyword: String) {
    SearchMessage(
        text = "‘$keyword’ 검색 결과가 없어요.",
        details = listOf(
            "검색어의 철자가 맞는지 확인해주세요.",
            "장소 · 도로명으로 검색해주세요.",
        ),
        showIllustration = true,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun SearchResultsContent(
    result: CourseLocationSearchResult,
    onSelect: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (result.regions.isNotEmpty()) {
            items(result.regions, key = CourseLocationSuggestion::id) { item ->
                SearchRow(item = item, onSelect = onSelect)
            }
        }
        if (result.regions.isNotEmpty() && result.places.isNotEmpty()) {
            item { HorizontalDivider(color = RodiTheme.colors.gray200, thickness = 4.dp) }
        }
        if (result.places.isNotEmpty()) {
            items(result.places, key = CourseLocationSuggestion::id) { item ->
                SearchRow(item = item, onSelect = onSelect)
            }
        }
    }
}

@Composable
internal fun SearchRow(
    item: CourseLocationSuggestion,
    onSelect: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val rowDescription = item.address
        .takeIf { it.isNotBlank() && it.trim() != item.title.trim() }
        ?.let { "${item.title}, $it" }
        ?: item.title
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .clickable { onSelect(item.id) }
                .padding(horizontal = RodiSpacing.md)
                .semantics {
                    contentDescription = rowDescription
                    role = Role.Button
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(
                    if (item.kind == CourseLocationKind.REGION) {
                        R.drawable.ic_registration_search
                    } else {
                        R.drawable.ic_registration_map_pin
                    },
                ),
                contentDescription = if (item.kind == CourseLocationKind.REGION) "지역" else "장소",
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = item.title,
                style = RodiTheme.typography.body2Medium,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            onDelete?.let {
                RodiIconButton(
                    painter = painterResource(R.drawable.ic_registration_x),
                    onClick = it,
                    iconSize = 20.dp,
                    contentDescription = "최근 검색어 삭제",
                    tint = RodiTheme.colors.black,
                )
            }
        }
        HorizontalDivider(color = RodiTheme.colors.gray100)
    }
}

@Composable
internal fun SearchMessage(
    text: String,
    modifier: Modifier = Modifier,
    details: List<String> = emptyList(),
    showIllustration: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    val retryFooter: @Composable ColumnScope.() -> Unit = {
        onRetry?.let {
            Spacer(Modifier.height(12.dp))
            RodiButton(
                text = "다시 시도",
                onClick = it,
                variant = RodiButtonVariant.Secondary,
                fillMaxWidth = false,
                height = 38.dp,
            )
        }
    }
    if (showIllustration) {
        RodiIllustratedEmptyState(
            modifier = modifier,
            painter = painterResource(R.drawable.illust_course_registration_empty),
            imageSize = 80.dp,
            title = text,
            description = details.joinToString("\n").takeIf { it.isNotBlank() },
            footer = retryFooter,
        )
    } else {
        RodiTextEmptyState(
            modifier = modifier,
            title = text,
            description = details.joinToString("\n").takeIf { it.isNotBlank() },
            footer = retryFooter,
        )
    }
}
