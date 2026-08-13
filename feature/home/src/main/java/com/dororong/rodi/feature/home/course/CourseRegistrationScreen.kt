package com.dororong.rodi.feature.home.course

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R

@Composable
fun CourseRegistrationScreen(
    onBack: () -> Unit = {},
    viewModel: CourseRegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message -> snackbarHostState.showSnackbar(message) }
    }
    BackHandler(onBack = onBack)

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 56.dp),
            )
        },
        containerColor = RodiTheme.colors.white,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    ) { contentPadding ->
        CourseRegistrationScreenContent(
            state = state,
            focusRequester = focusRequester,
            onIntent = viewModel::onIntent,
            modifier = Modifier.padding(contentPadding),
        )
    }
}

@Composable
private fun CourseRegistrationScreenContent(
    state: CourseRegistrationUiState,
    focusRequester: FocusRequester,
    onIntent: (CourseRegistrationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "코스 등록",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
            )
        }
        CourseRegistrationSearchInput(
            query = state.query,
            focusRequester = focusRequester,
            onQueryChange = { onIntent(CourseRegistrationIntent.OnQueryChange(it)) },
            onImeSearch = { onIntent(CourseRegistrationIntent.OnImeSearch) },
        )
        when (state.resultState) {
            CourseRegistrationSearchResultState.Idle -> CourseRegistrationIdleContent(
                modifier = Modifier.weight(1f),
            )
            CourseRegistrationSearchResultState.Loading -> CourseRegistrationLoadingContent(
                modifier = Modifier.weight(1f),
            )
            CourseRegistrationSearchResultState.Content -> CourseRegistrationSuggestionList(
                regions = state.regionSuggestions,
                places = state.placeSuggestions,
                modifier = Modifier.weight(1f),
            )
            CourseRegistrationSearchResultState.Empty -> CourseRegistrationEmptyContent(
                query = state.query.trim(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(56.dp))
    }
}

@Composable
private fun CourseRegistrationSearchInput(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onImeSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .height(46.dp)
            .background(RodiTheme.colors.gray200, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = RodiTheme.colors.gray600,
            modifier = Modifier.size(20.dp),
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
                .semantics { contentDescription = "코스 등록 장소 검색어 입력" },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isBlank()) {
                        Text(
                            text = "지역 또는 장소를 검색해보세요",
                            style = RodiTheme.typography.body2Medium,
                            color = RodiTheme.colors.gray500,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotBlank()) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = "검색어 지우기",
                tint = RodiTheme.colors.gray600,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun CourseRegistrationSuggestionList(
    regions: List<String>,
    places: List<PlaceSuggestion>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        if (regions.isNotEmpty()) {
            item(key = "region_header") {
                SuggestionSectionHeader(text = "지역")
            }
            itemsIndexed(
                items = regions,
                key = { index, region -> "region-$index-$region" },
            ) { _, region ->
                CourseRegistrationSuggestionRow(
                    text = region,
                    detail = "지역",
                    iconRes = R.drawable.ic_search,
                )
            }
        }
        if (regions.isNotEmpty() && places.isNotEmpty()) {
            item(key = "place_divider") {
                HorizontalDivider(color = RodiTheme.colors.gray200, thickness = 4.dp)
            }
        }
        if (places.isNotEmpty()) {
            item(key = "place_header") {
                SuggestionSectionHeader(text = "장소")
            }
            items(places, key = PlaceSuggestion::placeId) { place ->
                CourseRegistrationSuggestionRow(
                    text = place.name,
                    detail = place.region,
                    iconRes = R.drawable.ic_map_pin,
                )
            }
        }
    }
}

@Composable
private fun SuggestionSectionHeader(text: String) {
    Text(
        text = text,
        style = RodiTheme.typography.caption2Medium,
        color = RodiTheme.colors.gray700,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun CourseRegistrationSuggestionRow(
    text: String,
    detail: String,
    iconRes: Int,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (iconRes == R.drawable.ic_map_pin) Color.Unspecified else RodiTheme.colors.gray600,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = RodiTheme.typography.body2Medium,
                    color = RodiTheme.colors.black,
                )
                Text(
                    text = detail,
                    style = RodiTheme.typography.caption2Medium,
                    color = RodiTheme.colors.gray500,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        HorizontalDivider(color = RodiTheme.colors.gray100)
    }
}

@Composable
private fun CourseRegistrationIdleContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "지역이나 장소를 입력하면\n연관 검색어를 보여드릴게요.",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray600,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CourseRegistrationLoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        CircularProgressIndicator(
            color = RodiTheme.colors.primary600,
            strokeWidth = 2.dp,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

@Composable
private fun CourseRegistrationEmptyContent(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.illust_course_empty),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
        )
        Text(
            text = "‘$query’ 연관 검색어가 없어요.",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.gray600,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = "지역명이나 코스명을 입력해보세요.",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray600,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationScreenPreview() {
    RodiTheme {
        CourseRegistrationScreenContent(
            state = CourseRegistrationUiState(
                query = "중구",
                resultState = CourseRegistrationSearchResultState.Content,
                regionSuggestions = listOf("서울 중구", "부산 중구"),
                placeSuggestions = listOf(
                    PlaceSuggestion(1L, "중구 초보 운전 연습 코스", "서울 중구"),
                ),
            ),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
        )
    }
}
