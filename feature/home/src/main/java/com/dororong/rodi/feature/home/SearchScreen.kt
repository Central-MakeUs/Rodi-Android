package com.dororong.rodi.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.model.search.SearchTargetType
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.search.RegionOfficeLocation
import com.dororong.rodi.feature.home.search.RegionOfficeLocationResolver
import com.dororong.rodi.feature.home.search.SearchEffect
import com.dororong.rodi.feature.home.search.SearchIntent
import com.dororong.rodi.feature.home.search.SearchResultState
import com.dororong.rodi.feature.home.search.SearchUiState
import com.dororong.rodi.feature.home.search.SearchViewModel

@Composable
fun SearchScreen(
    origin: GeoPoint,
    onBack: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    onRegionClick: (RegionOfficeLocation, List<PlaceSummary>) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(origin) {
        viewModel.initialize(origin)
        focusRequester.requestFocus()
    }
    BackHandler(onBack = onBack)
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is SearchEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            is SearchEffect.NavigatePlace -> onPlaceClick(effect.placeId)
            is SearchEffect.NavigateRegion -> onRegionClick(effect.region, effect.initialPlaces)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = RodiTheme.colors.white,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    ) { contentPadding ->
        SearchScreenContent(
            state = state,
            focusRequester = focusRequester,
            onIntent = viewModel::onIntent,
            onBack = onBack,
            modifier = Modifier.padding(contentPadding),
        )
    }
}

@Composable
private fun SearchScreenContent(
    state: SearchUiState,
    focusRequester: FocusRequester,
    onIntent: (SearchIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        SearchInput(
            query = state.query,
            focusRequester = focusRequester,
            onQueryChange = { onIntent(SearchIntent.OnQueryChange(it)) },
            onImeSearch = { onIntent(SearchIntent.OnImeSearch) },
            onBack = onBack,
        )
        when {
            state.query.isBlank() -> RecentSearchList(
                searches = state.recentSearches,
                isLoading = state.isRecentSearchesLoading,
                isDeletingAll = state.isDeletingAllRecentSearches,
                deletingIds = state.deletingRecentSearchIds,
                onSearchClick = { onIntent(SearchIntent.OnRecentSearchClick(it)) },
                onDeleteAll = { onIntent(SearchIntent.OnDeleteAllRecentSearches) },
                onDelete = { onIntent(SearchIntent.OnDeleteRecentSearch(it)) },
                modifier = Modifier.weight(1f),
            )

            state.resultState == SearchResultState.Loading -> SearchSuggestionSkeletonList(Modifier.weight(1f))
            state.resultState == SearchResultState.Content -> SearchSuggestionList(
                regions = state.regionSuggestions,
                places = state.places,
                onRegionClick = { onIntent(SearchIntent.OnRegionSuggestionClick(it)) },
                onPlaceClick = { onIntent(SearchIntent.OnPlaceSuggestionClick(it)) },
                onLoadNextPage = { onIntent(SearchIntent.OnLoadNextPage) },
                isNextPageLoading = state.isNextPageLoading,
                modifier = Modifier.weight(1f),
            )

            state.resultState == SearchResultState.Empty -> SearchEmptyContent(state.query.trim())
            state.resultState == SearchResultState.RegionEmpty -> RegionSearchEmptyContent()
            state.resultState == SearchResultState.Idle -> SearchEmptyContent(state.query.trim())
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onImeSearch: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .height(46.dp)
            .background(RodiTheme.colors.gray200, RoundedCornerShape(8.dp))
            .padding(start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            contentDescription = "뒤로가기",
            tint = RodiTheme.colors.black,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = RodiTheme.typography.body2Medium.copy(color = RodiTheme.colors.black),
            cursorBrush = SolidColor(RodiTheme.colors.black),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onImeSearch() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .semantics { contentDescription = "지역 또는 장소 검색어 입력" },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isBlank()) {
                        Text(
                            text = "시/군/구/코스명으로 검색하기",
                            style = RodiTheme.typography.body2Medium,
                            color = RodiTheme.colors.gray500,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun RecentSearchList(
    searches: List<RecentSearch>,
    isLoading: Boolean,
    isDeletingAll: Boolean,
    deletingIds: Set<Long>,
    onSearchClick: (RecentSearch) -> Unit,
    onDeleteAll: () -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> RecentSearchSkeletonList(modifier)
        searches.isEmpty() -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "최근 검색 내역이 없습니다",
                style = RodiTheme.typography.body1Medium,
                color = RodiTheme.colors.gray600,
            )
        }
        else -> LazyColumn(modifier = modifier.fillMaxWidth()) {
            item(key = "recent_search_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "최근 검색어",
                        style = RodiTheme.typography.caption2Medium,
                        color = RodiTheme.colors.gray700,
                    )
                    Text(
                        text = "전체삭제",
                        style = RodiTheme.typography.caption2Medium,
                        color = RodiTheme.colors.gray500,
                        modifier = Modifier.clickable(enabled = !isDeletingAll, onClick = onDeleteAll),
                    )
                }
            }
            items(searches, key = RecentSearch::id) { search ->
                SearchRow(
                    text = search.keyword,
                    iconRes = if (search.type == SearchTargetType.PLACE || search.placeId != null) {
                        R.drawable.ic_map_pin
                    } else {
                        R.drawable.ic_search
                    },
                    onClick = { onSearchClick(search) },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_x),
                            contentDescription = "최근 검색어 삭제",
                            tint = RodiTheme.colors.black,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    enabled = search.id !in deletingIds && !isDeletingAll,
                                    onClick = { onDelete(search.id) },
                                ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RecentSearchSkeletonList(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item(key = "recent_search_skeleton_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RodiSkeleton(Modifier.size(width = 52.dp, height = 12.dp))
                RodiSkeleton(Modifier.size(width = 40.dp, height = 12.dp))
            }
        }
        items(3) { SearchRowSkeleton(showTrailing = true) }
    }
}

@Composable
private fun SearchRow(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = RodiTheme.typography.body2Medium,
                color = RodiTheme.colors.black,
                modifier = Modifier.weight(1f),
            )
            trailingContent?.invoke()
        }
        HorizontalDivider(color = RodiTheme.colors.gray100)
    }
}

@Composable
private fun SearchSuggestionList(
    regions: List<RegionOfficeLocation>,
    places: List<PlaceSuggestion>,
    onRegionClick: (RegionOfficeLocation) -> Unit,
    onPlaceClick: (PlaceSuggestion) -> Unit,
    onLoadNextPage: () -> Unit,
    isNextPageLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = regions.size + places.size + if (regions.isNotEmpty() && places.isNotEmpty()) 1 else 0
    val lastItemIndex = itemCount - 1
    val isAtListEnd by remember(listState, lastItemIndex) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lastItemIndex
        }
    }
    LaunchedEffect(isAtListEnd, isNextPageLoading, places.size) {
        if (isAtListEnd && !isNextPageLoading && places.isNotEmpty()) onLoadNextPage()
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
    ) {
        items(regions, key = RegionOfficeLocation::regionKey) { region ->
            SearchRow(
                text = region.displayName,
                iconRes = R.drawable.ic_search,
                onClick = { onRegionClick(region) },
            )
        }
        if (regions.isNotEmpty() && places.isNotEmpty()) {
            item { HorizontalDivider(color = RodiTheme.colors.gray200, thickness = 4.dp) }
        }
        items(places, key = PlaceSuggestion::placeId) { place ->
            SearchRow(
                text = place.name,
                iconRes = R.drawable.ic_map_pin,
                onClick = { onPlaceClick(place) },
            )
        }
        if (isNextPageLoading) {
            item { SearchLoadingContent(Modifier.fillParentMaxWidth().height(62.dp)) }
        }
    }
}

@Composable
private fun SearchSuggestionSkeletonList(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(6) { SearchRowSkeleton() }
    }
}

@Composable
private fun SearchRowSkeleton(showTrailing: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RodiSkeleton(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
            )
            RodiSkeleton(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp),
            )
            if (showTrailing) {
                RodiSkeleton(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                )
            }
        }
        HorizontalDivider(color = RodiTheme.colors.gray100)
    }
}

@Composable
private fun SearchLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = RodiTheme.colors.primary600,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun SearchEmptyContent(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 136.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.illust_course_empty),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
        )
        Text(
            text = "‘$query’검색 결과가 없어요.",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.gray600,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = "검색어의 철자가 맞는지 확인해주세요.\n시/군/구/코스명으로 검색해주세요.",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun RegionSearchEmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 129.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.illust_course_empty),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
        )
        Text(
            text = "추천할 수 있는 연습 코스를 찾지 못했어요.",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.gray600,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "다른 지역의\n연습 코스를 둘러보세요.",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(name = "Search - recent", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchRecentPreview() {
    RodiTheme {
        SearchScreenContent(
            state = SearchUiState(
                recentSearches = List(15) { index ->
                    if (index % 2 == 0) {
                        RecentSearch(index.toLong(), "서울 중구", SearchTargetType.REGION)
                    } else {
                        RecentSearch(
                            id = index.toLong(),
                            keyword = "인천 미단시티 코스",
                            type = SearchTargetType.PLACE,
                            placeId = index.toLong(),
                        )
                    }
                },
                isRecentSearchesLoading = false,
            ),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search - loading", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchLoadingPreview() {
    RodiTheme {
        SearchScreenContent(
            state = SearchUiState(query = "중구", resultState = SearchResultState.Loading),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search - recent loading", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchRecentLoadingPreview() {
    RodiTheme {
        SearchScreenContent(
            state = SearchUiState(isRecentSearchesLoading = true),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search - empty", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchEmptyPreview() {
    RodiTheme {
        SearchScreenContent(
            state = SearchUiState(query = "중군", resultState = SearchResultState.Empty),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search - suggestions", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchSuggestionPreview() {
    RodiTheme {
        SearchScreenContent(
            state = SearchUiState(
                query = "중구",
                resultState = SearchResultState.Content,
                regionSuggestions = listOfNotNull(
                    RegionOfficeLocationResolver.find("서울 중구"),
                    RegionOfficeLocationResolver.find("부산 중구"),
                ),
                places = listOf(searchPreviewPlace()),
            ),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search - no recent", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchNoRecentPreview() {
    RodiTheme {
        SearchScreenContent(
            state = SearchUiState(isRecentSearchesLoading = false),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search - region empty", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchRegionEmptyPreview() {
    RodiTheme {
        SearchScreenContent(
            state = SearchUiState(query = "서울 중구", resultState = SearchResultState.RegionEmpty),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
            onBack = {},
        )
    }
}

private fun searchPreviewPlace() = PlaceSuggestion(
    placeId = 1L,
    name = "중구 초보 운전 연습 코스",
    region = "서울 중구",
)
