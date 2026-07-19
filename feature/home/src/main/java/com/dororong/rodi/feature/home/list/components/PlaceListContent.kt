package com.dororong.rodi.feature.home.list.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData

@Composable
fun PlaceListContent(
    places: List<PlaceSummary>,
    onPlaceClick: (Long) -> Unit,
    onLoadNextPage: () -> Unit,
    isNextPageLoading: Boolean,
    topContentPadding: Dp,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val currentPlaces by rememberUpdatedState(places)
    val shouldLoadNext by remember(listState) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            currentPlaces.isNotEmpty() && lastVisible >= currentPlaces.lastIndex - 5
        }
    }
    LaunchedEffect(shouldLoadNext, isNextPageLoading) {
        if (shouldLoadNext && !isNextPageLoading) onLoadNextPage()
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = topContentPadding, bottom = 8.dp),
    ) {
        itemsIndexed(places, key = { _, item -> item.id }) { index, place ->
            PlaceCard(place = place, onClick = { onPlaceClick(place.id) })
            if (index != places.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = RodiTheme.colors.gray100,
                )
            }
        }
        if (isNextPageLoading) {
            item(key = "paging") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = RodiTheme.colors.primary600,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        item(key = "navigation-inset") {
            Box(Modifier.navigationBarsPadding())
        }
    }
}

@Preview(name = "Place list - partial", showBackground = true, widthDp = 375, heightDp = 320)
@Composable
private fun PlaceListPartialPreview() {
    RodiTheme {
        PlaceListContent(HomePreviewData.summaries, {}, {}, false, topContentPadding = 0.dp)
    }
}

@Preview(name = "Place list - full paging", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PlaceListFullPreview() {
    RodiTheme {
        PlaceListContent(
            places = List(7) { HomePreviewData.summaries[it % 3].copy(id = 100L + it) },
            onPlaceClick = {},
            onLoadNextPage = {},
            isNextPageLoading = true,
            topContentPadding = 20.dp,
        )
    }
}
