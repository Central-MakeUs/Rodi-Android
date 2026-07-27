package com.dororong.rodi.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.core.ui.R as CoreUiR

private val recentSearches = List(4) { "서울 중구" }
private val regionSuggestions = listOf("서울 중구", "대전 중구", "울산 중구", "부산 중구", "대구 중구")

@Composable
fun SearchScreen(
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BackHandler(onBack = onBack)

    SearchScreenContent(
        query = query,
        focusRequester = focusRequester,
        onQueryChange = { query = it },
        onBack = onBack,
    )
}

@Composable
private fun SearchScreenContent(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        SearchInput(
            query = query,
            focusRequester = focusRequester,
            onQueryChange = onQueryChange,
            onBack = onBack,
        )
        when {
            query.isBlank() -> RecentSearchList()
            regionSuggestionsFor(query).isNotEmpty() -> SuggestionList(regionSuggestionsFor(query))
            else -> SearchEmptyContent(query)
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
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
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .semantics { contentDescription = "지역 검색어 입력" },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isBlank()) {
                        Text(
                            text = "시/군/구로 검색하기",
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
private fun RecentSearchList() {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            )
        }
        recentSearches.forEach { searchTerm ->
            SearchRow(
                text = searchTerm,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_x),
                        contentDescription = "최근 검색어 삭제",
                        tint = RodiTheme.colors.black,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun SuggestionList(suggestions: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        suggestions.forEach { suggestion ->
            SearchRow(text = suggestion)
        }
    }
}

@Composable
private fun SearchRow(
    text: String,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = RodiTheme.typography.body1Medium,
                color = RodiTheme.colors.gray800,
            )
            trailingContent?.invoke()
        }
        HorizontalDivider(color = RodiTheme.colors.gray100)
    }
}

@Composable
private fun SearchEmptyContent(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 132.dp),
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
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "검색어의 철자가 맞는지 확인해주세요.\n시/군/구로 검색해주세요.",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

internal fun regionSuggestionsFor(query: String): List<String> =
    regionSuggestions.filter { it.contains(query.trim()) }

@Preview(name = "Search - recent", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchRecentPreview() {
    RodiTheme {
        SearchScreenContent(
            query = "",
            focusRequester = remember { FocusRequester() },
            onQueryChange = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search - suggestions", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchSuggestionsPreview() {
    RodiTheme {
        SearchScreenContent(
            query = "중구",
            focusRequester = remember { FocusRequester() },
            onQueryChange = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search - empty", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SearchEmptyPreview() {
    RodiTheme {
        SearchScreenContent(
            query = "중군",
            focusRequester = remember { FocusRequester() },
            onQueryChange = {},
            onBack = {},
        )
    }
}
