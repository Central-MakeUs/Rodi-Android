package com.dororong.rodi.feature.course.registration.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onSizeChanged
import com.dororong.rodi.core.domain.model.course.CourseLocationKind
import com.dororong.rodi.core.domain.model.course.CourseLocationSearchResult
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.CourseMapLoadState
import com.dororong.rodi.feature.course.registration.CourseRegistrationIntent
import com.dororong.rodi.feature.course.registration.CourseWaypointRole
import com.dororong.rodi.feature.course.registration.InitialLocationState
import com.dororong.rodi.feature.course.registration.map.CourseRegistrationMapView
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationCurrentLocationButton
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationMapError
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationMapFormLoading
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationMapLoadingOverlay
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationPinEditBar
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationWaypointCard
import com.dororong.rodi.feature.course.registration.components.FixedCenterPin
import com.dororong.rodi.feature.course.registration.components.PinEditAddressRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private const val DEFAULT_MAX_VIAS = 4

@Composable
fun CourseRegistrationMapContent(
    mapLoadState: CourseMapLoadState,
    mapRetryToken: Int,
    mapCenter: GeoPoint?,
    mapCenterGeneration: Long,
    mapCenterKeepsZoom: Boolean = false,
    waypoints: List<RegistrationWaypoint>,
    route: RouteResult?,
    isRouteLoading: Boolean,
    selectedWaypointRole: CourseWaypointRole,
    editingWaypointIndex: Int?,
    temporaryPin: GeoPoint?,
    isSearchVisible: Boolean,
    searchKeyword: String,
    searchLoading: Boolean,
    recentSearchLoading: Boolean = false,
    searchResult: CourseLocationSearchResult,
    searchError: String?,
    canFinish: Boolean,
    onIntent: (CourseRegistrationIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isMapPointLoading: Boolean = false,
    maxVias: Int = DEFAULT_MAX_VIAS,
    isFormLoading: Boolean = false,
    pendingSuggestion: CourseLocationSuggestion? = null,
    isPendingAddressLoading: Boolean = false,
    initialLocationState: InitialLocationState = InitialLocationState.NotRequested,
) {
    if (isSearchVisible) {
        CourseRegistrationSearchContent(
            keyword = searchKeyword,
            isLoading = searchLoading || isMapPointLoading,
            isRecentLoading = recentSearchLoading,
            result = searchResult,
            error = searchError,
            onBack = { onIntent(CourseRegistrationIntent.SearchVisibilityChanged(false)) },
            onKeywordChanged = { onIntent(CourseRegistrationIntent.SearchKeywordChanged(it)) },
            onSubmit = { onIntent(CourseRegistrationIntent.SearchSubmitted) },
            onSelect = { onIntent(CourseRegistrationIntent.SearchSuggestionSelected(it)) },
            onDeleteRecent = { onIntent(CourseRegistrationIntent.DeleteRecentSearch(it)) },
            onDeleteAll = { onIntent(CourseRegistrationIntent.DeleteAllRecentSearches) },
            onRetry = { onIntent(CourseRegistrationIntent.SearchSubmitted) },
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.gray100),
    ) {
        var headerHeightPx by remember { mutableIntStateOf(0) }
        var bottomPanelHeightPx by remember { mutableIntStateOf(0) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val headerHeight = with(density) { headerHeightPx.toDp() }
        val bottomPanelHeight = with(density) { bottomPanelHeightPx.toDp() }

        if (mapCenter != null || initialLocationState == InitialLocationState.Unavailable) {
            CourseRegistrationMapView(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "지도를 움직여 핀을 놓을 위치를 정하세요" },
                retryToken = mapRetryToken,
                center = mapCenter,
                centerGeneration = mapCenterGeneration,
                centerKeepsZoom = mapCenterKeepsZoom,
                waypoints = waypoints,
                route = route,
                editingWaypointIndex = editingWaypointIndex,
                temporaryPin = temporaryPin,
                onReady = { onIntent(CourseRegistrationIntent.MapReady(it)) },
                onCameraCenterChanged = { onIntent(CourseRegistrationIntent.MapCenterChanged(it)) },
                onMapTapped = {},
                onWaypointTapped = { onIntent(CourseRegistrationIntent.BeginPinEdit(it)) },
                topPaddingPx = headerHeightPx,
                bottomPaddingPx = bottomPanelHeightPx,
            )
        }

        val hasStart = waypoints.any { it.type == RegistrationWaypointType.START }
        val hasDestination = waypoints.any { it.type == RegistrationWaypointType.DESTINATION }
        val isPlacingVia = selectedWaypointRole == CourseWaypointRole.Via
        val showCenterReticle = editingWaypointIndex != null ||
            !hasStart || !hasDestination || isPlacingVia

        val centerPinRole = editingWaypointIndex?.let { index ->
            when (waypoints.getOrNull(index)?.type) {
                RegistrationWaypointType.START -> CourseWaypointRole.Start
                RegistrationWaypointType.VIA -> CourseWaypointRole.Via
                RegistrationWaypointType.DESTINATION -> CourseWaypointRole.Destination
                null -> null
            }
        } ?: selectedWaypointRole
        if (showCenterReticle) {
            FixedCenterPin(
                role = centerPinRole,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeight, bottom = bottomPanelHeight),
            )
        }

        CourseRegistrationMapHeader(
            waypoints = waypoints,
            maxVias = maxVias,
            selectedWaypointRole = selectedWaypointRole,
            pendingSuggestion = pendingSuggestion,
            isPendingAddressLoading = isPendingAddressLoading,
            editingWaypoint = waypoints.getOrNull(editingWaypointIndex ?: -1),
            temporaryPin = temporaryPin,
            onBack = onBack,
            onSearch = { onIntent(CourseRegistrationIntent.SearchVisibilityChanged(true)) },
            onRoleSelected = { onIntent(CourseRegistrationIntent.SelectWaypointRole(it)) },
            onRemoveVia = { onIntent(CourseRegistrationIntent.RemoveWaypoint(it)) },
            modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
        )

        when (mapLoadState) {
            CourseMapLoadState.Loading -> CourseRegistrationMapLoadingOverlay()
            CourseMapLoadState.Error -> CourseRegistrationMapError(
                modifier = Modifier.align(Alignment.Center),
                onRetry = { onIntent(CourseRegistrationIntent.Retry) },
            )
            CourseMapLoadState.Ready -> Unit
        }
        if (isMapPointLoading || initialLocationState == InitialLocationState.Requesting) {
            CourseRegistrationMapLoadingOverlay()
        }

        if (isFormLoading) {
            CourseRegistrationMapFormLoading(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomPanelHeightPx = it.height },
            )
        } else if (editingWaypointIndex != null) {
            CourseRegistrationPinEditBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomPanelHeightPx = it.height },
                waypoint = waypoints.getOrNull(editingWaypointIndex),
                originalPoint = waypoints.getOrNull(editingWaypointIndex)?.let { GeoPoint(it.lat, it.lng) },
                temporaryPin = temporaryPin,
                mapCenter = mapCenter,
                isLoading = isMapPointLoading,
                pendingSuggestion = pendingSuggestion,
                isPendingAddressLoading = isPendingAddressLoading,
                onSelect = { mapCenter?.let { onIntent(CourseRegistrationIntent.MapPointSelected(it)) } },
                onReset = { onIntent(CourseRegistrationIntent.ResetPinEdit) },
                onCommit = { onIntent(CourseRegistrationIntent.CommitPinEdit) },
            )
        } else {
            CourseRegistrationMapBottomPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomPanelHeightPx = it.height },
                waypoints = waypoints,
                mapCenter = mapCenter,
                selectedWaypointRole = selectedWaypointRole,
                isRouteLoading = isRouteLoading,
                canFinish = canFinish,
                maxVias = maxVias,
                pendingSuggestion = pendingSuggestion,
                isPendingAddressLoading = isPendingAddressLoading,
                onIntent = onIntent,
            )
        }

        CourseRegistrationCurrentLocationButton(
            onIntent = onIntent,
            autoRequest = initialLocationState == InitialLocationState.Requesting,
            visible = initialLocationState != InitialLocationState.Requesting,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = bottomPanelHeight + 8.dp),
        )
    }
}

@Composable
private fun CourseRegistrationMapHeader(
    waypoints: List<RegistrationWaypoint>,
    maxVias: Int,
    selectedWaypointRole: CourseWaypointRole,
    pendingSuggestion: CourseLocationSuggestion?,
    isPendingAddressLoading: Boolean,
    editingWaypoint: RegistrationWaypoint?,
    temporaryPin: GeoPoint?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onRoleSelected: (CourseWaypointRole) -> Unit,
    onRemoveVia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            RodiIconButton(
                painter = painterResource(CoreUiR.drawable.ic_chevron_left),
                onClick = onBack,
                contentDescription = "코스 등록 나가기",
                tint = RodiTheme.colors.black,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = if (editingWaypoint == null) "코스 등록" else "핀 수정하기",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
            )
        }
        if (editingWaypoint == null) {
            CourseRegistrationWaypointCard(
                waypoints = waypoints,
                maxVias = maxVias,
                selectedWaypointRole = selectedWaypointRole,
                pendingSuggestion = pendingSuggestion,
                isPendingAddressLoading = isPendingAddressLoading,
                onRoleSelected = onRoleSelected,
                onSearch = onSearch,
                onRemoveVia = onRemoveVia,
            )
        } else {
            PinEditAddressRow(
                waypoint = editingWaypoint,
                pendingSuggestion = pendingSuggestion,
                isPendingAddressLoading = isPendingAddressLoading,
                onClick = onSearch,
            )
        }
    }
}

@Composable
private fun CourseRegistrationMapBottomPanel(
    modifier: Modifier,
    waypoints: List<RegistrationWaypoint>,
    mapCenter: GeoPoint?,
    selectedWaypointRole: CourseWaypointRole,
    isRouteLoading: Boolean,
    canFinish: Boolean,
    maxVias: Int,
    pendingSuggestion: CourseLocationSuggestion?,
    isPendingAddressLoading: Boolean,
    onIntent: (CourseRegistrationIntent) -> Unit,
) {
    val hasStart = waypoints.any { it.type == RegistrationWaypointType.START }
    val hasDestination = waypoints.any { it.type == RegistrationWaypointType.DESTINATION }
    val viaCount = waypoints.count { it.type == RegistrationWaypointType.VIA }
    val isPlacingVia = selectedWaypointRole == CourseWaypointRole.Via
    val isAddViaMode = hasStart && hasDestination && !isPlacingVia
    val addressReady = mapCenter != null && !isPendingAddressLoading && pendingSuggestion != null

    val startWaypoint = waypoints.firstOrNull { it.type == RegistrationWaypointType.START }
    val candidatePoint = pendingSuggestion?.point
    val isDestinationSameAsStart = selectedWaypointRole == CourseWaypointRole.Destination &&
        startWaypoint != null && candidatePoint != null &&
        startWaypoint.lat == candidatePoint.lat && startWaypoint.lng == candidatePoint.lng

    val selectionEnabled = !isAddViaMode && addressReady && !isDestinationSameAsStart &&
        (selectedWaypointRole != CourseWaypointRole.Via || viaCount < maxVias)
    val selectionLabel = when (selectedWaypointRole) {
        CourseWaypointRole.Start -> "출발지 선택"
        CourseWaypointRole.Destination -> "도착지 선택"
        CourseWaypointRole.Via -> "경유지 선택"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // 하단 패널은 직각으로 그린다(QA: 지도 바텀탭은 radius 없이).
            .background(RodiTheme.colors.white, RectangleShape)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = RodiSpacing.md, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            RodiButton(
                text = if (isAddViaMode) "경유지 추가" else selectionLabel,
                onClick = {
                    if (isAddViaMode) {
                        onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Via))
                    } else {
                        mapCenter?.let { onIntent(CourseRegistrationIntent.MapPointSelected(it)) }
                    }
                },
                enabled = !isRouteLoading && (if (isAddViaMode) viaCount < maxVias else selectionEnabled),
                variant = RodiButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
                height = 48.dp,
            )
            RodiButton(
                text = "완료",
                onClick = { onIntent(CourseRegistrationIntent.ContinueToForm) },
                enabled = canFinish,
                modifier = Modifier.weight(1f),
                height = 48.dp,
            )
        }
    }
}

@Preview(name = "Map Form Loading", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapFormLoadingPreview() {
    PreviewMap(isFormLoading = true)
}

@Preview(name = "Map Loading", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapLoadingPreview() {
    PreviewMap(mapLoadState = CourseMapLoadState.Loading)
}

@Preview(name = "Map Initial", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapInitialPreview() {
    PreviewMap()
}

@Preview(name = "Map Start", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapStartPreview() {
    PreviewMap(waypoints = previewWaypoints.take(1), selectedRole = CourseWaypointRole.Destination)
}

@Preview(name = "Map Destination", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapDestinationPreview() {
    PreviewMap(waypoints = previewWaypoints, selectedRole = CourseWaypointRole.Via)
}

@Preview(name = "Map Route", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapRoutePreview() {
    PreviewMap(waypoints = previewWaypoints, selectedRole = CourseWaypointRole.Via, canFinish = true)
}

@Preview(name = "Map One Via", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapOneViaPreview() {
    PreviewMap(
        waypoints = previewWaypointsWithVias.take(3),
        selectedRole = CourseWaypointRole.Via,
        maxVias = DEFAULT_MAX_VIAS,
    )
}

@Preview(name = "Map Two Vias", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapTwoViasPreview() {
    PreviewMap(
        waypoints = previewWaypointsWithVias,
        selectedRole = CourseWaypointRole.Via,
        maxVias = DEFAULT_MAX_VIAS,
    )
}

@Preview(name = "Map Max Vias", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapMaxViasPreview() {
    PreviewMap(
        waypoints = previewWaypoints + RegistrationWaypoint(RegistrationWaypointType.VIA, "한강대교", "서울 용산구", lat = 37.5, lng = 126.95),
        selectedRole = CourseWaypointRole.Via,
        maxVias = 1,
    )
}

@Preview(name = "Map Address Unavailable", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapAddressUnavailablePreview() {
    PreviewMap(waypoints = previewWaypoints.map { it.copy(address = "") }, selectedRole = CourseWaypointRole.Via)
}

@Preview(name = "Map Route Loading", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationMapRouteLoadingPreview() {
    PreviewMap(waypoints = previewWaypoints, selectedRole = CourseWaypointRole.Via, isRouteLoading = true)
}

@Preview(name = "Pin Edit Original", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationPinEditOriginalPreview() {
    PreviewMap(waypoints = previewWaypoints, editingIndex = 0, temporaryPin = null)
}

@Preview(name = "Pin Edit Selected", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationPinEditSelectedPreview() {
    PreviewMap(waypoints = previewWaypoints, editingIndex = 0, temporaryPin = GeoPoint(37.56, 126.98), mapCenter = GeoPoint(37.56, 126.98))
}

@Preview(name = "Pin Edit Reset", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationPinEditResetPreview() {
    PreviewMap(waypoints = previewWaypoints, editingIndex = 1, temporaryPin = null, mapCenter = GeoPoint(37.5512, 126.9882))
}

@Preview(name = "Pin Edit Failure", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationPinEditFailurePreview() {
    PreviewMap(
        waypoints = previewWaypoints,
        editingIndex = 0,
        temporaryPin = GeoPoint(37.56, 126.98),
        mapCenter = GeoPoint(37.56, 126.98),
        isMapPointLoading = true,
    )
}

@Preview(name = "Search Recent", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchRecentPreview() {
    PreviewSearch(result = CourseLocationSearchResult(recent = previewSuggestions))
}

@Preview(name = "Search Recent Loading", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchRecentLoadingPreview() {
    PreviewSearch(isRecentLoading = true)
}

@Preview(name = "Search Loading", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchLoadingPreview() {
    PreviewSearch(keyword = "성북", isLoading = true)
}

@Preview(name = "Search Content", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchContentPreview() {
    PreviewSearch(keyword = "성북", result = CourseLocationSearchResult(places = previewSuggestions))
}

@Preview(name = "Search Empty", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchEmptyPreview() {
    PreviewSearch()
}

@Preview(name = "Search Region", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchRegionPreview() {
    PreviewSearch(keyword = "성북", result = CourseLocationSearchResult(regions = previewSuggestions.take(1)))
}

@Preview(name = "Search Region Place", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchRegionPlacePreview() {
    PreviewSearch(keyword = "성북", result = CourseLocationSearchResult(regions = previewSuggestions.take(1), places = previewSuggestions))
}

@Preview(name = "Search No Result", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchNoResultPreview() {
    PreviewSearch(keyword = "중군")
}

@Preview(name = "Search Partial", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchPartialPreview() {
    PreviewSearch(keyword = "성북", result = CourseLocationSearchResult(regions = previewSuggestions.take(1), places = emptyList()))
}

@Preview(name = "Search Error", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSearchErrorPreview() {
    PreviewSearch(keyword = "성북", error = "검색에 실패했어요.")
}

private val previewWaypoints = listOf(
    RegistrationWaypoint(RegistrationWaypointType.START, "서울역", "서울 중구 한강대로", lat = 37.5547, lng = 126.9707),
    RegistrationWaypoint(RegistrationWaypointType.DESTINATION, "남산", "서울 중구 남산공원길", lat = 37.5512, lng = 126.9882),
)

private val previewWaypointsWithVias = listOf(
    previewWaypoints.first(),
    RegistrationWaypoint(RegistrationWaypointType.VIA, "한강대교", "서울 용산구 한강대로", lat = 37.53, lng = 126.97),
    RegistrationWaypoint(RegistrationWaypointType.VIA, "반포대교", "서울 서초구 신반포로", lat = 37.51, lng = 126.99),
    previewWaypoints.last(),
)

private val previewSuggestions = listOf(
    CourseLocationSuggestion("seoul", "성북구", "서울 성북구", GeoPoint(37.59, 127.02), CourseLocationKind.REGION),
    CourseLocationSuggestion("station", "길음역", "서울 성북구 동소문로", GeoPoint(37.603, 127.025), CourseLocationKind.PLACE),
)

@Composable
private fun PreviewMap(
    mapLoadState: CourseMapLoadState = CourseMapLoadState.Ready,
    waypoints: List<RegistrationWaypoint> = emptyList(),
    selectedRole: CourseWaypointRole = CourseWaypointRole.Start,
    mapCenter: GeoPoint? = GeoPoint(37.5547, 126.9707),
    route: RouteResult? = if (waypoints.size >= 2) {
        RouteResult(
            points = listOf(GeoPoint(37.5547, 126.9707), GeoPoint(37.5512, 126.9882)),
            isRealRoute = true,
            totalDistanceMeters = 2100,
            snappedPoints = waypoints.map { GeoPoint(it.lat, it.lng) },
        )
    } else null,
    isRouteLoading: Boolean = false,
    canFinish: Boolean = false,
    maxVias: Int = DEFAULT_MAX_VIAS,
    editingIndex: Int? = null,
    temporaryPin: GeoPoint? = null,
    isFormLoading: Boolean = false,
    isMapPointLoading: Boolean = false,
) {
    RodiTheme {
        CourseRegistrationMapContent(
            mapLoadState = mapLoadState,
            mapRetryToken = 0,
            mapCenter = mapCenter,
            mapCenterGeneration = 0,
            waypoints = waypoints,
            route = route,
            isRouteLoading = isRouteLoading,
            selectedWaypointRole = selectedRole,
            editingWaypointIndex = editingIndex,
            temporaryPin = temporaryPin,
            isSearchVisible = false,
            searchKeyword = "",
            searchLoading = false,
            searchResult = CourseLocationSearchResult(),
            searchError = null,
            canFinish = canFinish,
            onIntent = {},
            onBack = {},
            maxVias = maxVias,
            isFormLoading = isFormLoading,
            isMapPointLoading = isMapPointLoading,
        )
    }
}

@Composable
private fun PreviewSearch(
    keyword: String = "",
    isLoading: Boolean = false,
    isRecentLoading: Boolean = false,
    result: CourseLocationSearchResult = CourseLocationSearchResult(),
    error: String? = null,
) {
    RodiTheme {
        CourseRegistrationSearchContent(
            keyword = keyword,
            isLoading = isLoading,
            isRecentLoading = isRecentLoading,
            result = result,
            error = error,
            onBack = {},
            onKeywordChanged = {},
            onSubmit = {},
            onSelect = {},
            onDeleteRecent = {},
            onDeleteAll = {},
            onRetry = {},
        )
    }
}
