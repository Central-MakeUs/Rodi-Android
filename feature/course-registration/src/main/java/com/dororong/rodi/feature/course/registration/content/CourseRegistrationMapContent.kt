package com.dororong.rodi.feature.course.registration.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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
import com.dororong.rodi.core.ui.components.RodiIllustratedEmptyState
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.RodiTextEmptyState
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.components.input.rodiCursorBrush
import com.dororong.rodi.feature.course.registration.CourseMapLoadState
import com.dororong.rodi.feature.course.registration.CourseRegistrationIntent
import com.dororong.rodi.feature.course.registration.CourseRegistrationLoadingIndicator
import com.dororong.rodi.feature.course.registration.CourseWaypointRole
import com.dororong.rodi.feature.course.registration.InitialLocationState
import com.dororong.rodi.feature.course.registration.R
import com.dororong.rodi.feature.course.registration.map.CourseRegistrationMapView
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationCurrentLocationButton

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
private fun PinEditAddressRow(
    waypoint: RegistrationWaypoint,
    pendingSuggestion: CourseLocationSuggestion?,
    isPendingAddressLoading: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(RodiRadius.sm)
    val pendingLabel = pendingAddressLabel(pendingSuggestion, isPendingAddressLoading)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(shape)
            .background(RodiTheme.colors.white, shape)
            .border(1.dp, RodiTheme.colors.gray300, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics {
                contentDescription = "핀 수정 위치 검색"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WaypointDot(type = waypoint.type, dimmed = false)
        Text(
            text = pendingLabel ?: waypoint.address.ifBlank { waypoint.name.ifBlank { "주소를 확인할 수 없어요" } },
            style = RodiTheme.typography.body2Medium,
            color = if (pendingLabel != null) RodiTheme.colors.gray600 else RodiTheme.colors.black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CourseRegistrationWaypointCard(
    waypoints: List<RegistrationWaypoint>,
    maxVias: Int,
    selectedWaypointRole: CourseWaypointRole,
    pendingSuggestion: CourseLocationSuggestion?,
    isPendingAddressLoading: Boolean,
    onRoleSelected: (CourseWaypointRole) -> Unit,
    onSearch: () -> Unit,
    onRemoveVia: (Int) -> Unit,
) {
    val hasStart = waypoints.any { it.type == RegistrationWaypointType.START }
    val hasDestination = waypoints.any { it.type == RegistrationWaypointType.DESTINATION }
    // 원본 인덱스를 같이 들고 다닌다. waypoints.indexOf(waypoint)로 찾으면 좌표가 같은
    // 경유지 두 개를 구조적 동일성으로 헷갈려 엉뚱한 항목이 삭제된다.
    val vias = waypoints.withIndex().filter { it.value.type == RegistrationWaypointType.VIA }
    val isPlacingVia = selectedWaypointRole == CourseWaypointRole.Via
    val canAddVia = !isPlacingVia && vias.size < maxVias
    val pendingLabel = pendingAddressLabel(pendingSuggestion, isPendingAddressLoading)

    val hasViaRow = vias.isNotEmpty() || isPlacingVia
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            WaypointSelectionRow(
                type = RegistrationWaypointType.START,
                waypoint = waypoints.firstOrNull { it.type == RegistrationWaypointType.START },
                // 회색 미리보기 주소는 "비어 있는 첫 칸"이 아니라 "지금 선택 중인 역할"의
                // 칸에만 뜬다. 출발지를 건너뛰고 도착지부터 고른 경우에도 도착지 칸에 미리보기가
                // 떠야 한다(비어 있는 칸 기준이면 항상 출발지 칸에 잘못 떴다).
                pendingLabel = pendingLabel.takeIf { selectedWaypointRole == CourseWaypointRole.Start },
                onClick = {
                    onRoleSelected(CourseWaypointRole.Start)
                    onSearch()
                },
            )
            vias.forEachIndexed { index, (waypointIndex, waypoint) ->
                WaypointSelectionRow(
                    type = RegistrationWaypointType.VIA,
                    waypoint = waypoint,
                    label = "경유지 " + (index + 1),
                    onClick = {
                        onRoleSelected(CourseWaypointRole.Via)
                        onSearch()
                    },
                    trailing = {
                        WaypointActionIcon(
                            add = false,
                            enabled = true,
                            onClick = { onRemoveVia(waypointIndex) },
                        )
                    },
                )
            }
            if (isPlacingVia) {
                WaypointSelectionRow(
                    type = RegistrationWaypointType.VIA,
                    waypoint = null,
                    label = "경유지 " + (vias.size + 1),
                    pendingLabel = pendingLabel,
                    onClick = onSearch,
                    trailing = {
                        WaypointActionIcon(
                            add = false,
                            enabled = true,
                            onClick = { onRoleSelected(CourseWaypointRole.Destination) },
                        )
                    },
                )
            }
            WaypointSelectionRow(
                type = RegistrationWaypointType.DESTINATION,
                waypoint = waypoints.firstOrNull { it.type == RegistrationWaypointType.DESTINATION },
                pendingLabel = pendingLabel.takeIf { selectedWaypointRole == CourseWaypointRole.Destination },
                onClick = {
                    onRoleSelected(CourseWaypointRole.Destination)
                    onSearch()
                },
                trailing = if (hasViaRow) {
                    {
                        WaypointActionIcon(
                            add = true,
                            enabled = canAddVia,
                            onClick = { onRoleSelected(CourseWaypointRole.Via) },
                        )
                    }
                } else null,
            )
        }
        // 경유지 줄이 없을 때 +는 출발지·도착지 칸 사이 경계에 걸쳐 놓는다(디자인 3659:79178).
        if (!hasViaRow) {
            WaypointActionIcon(
                add = true,
                enabled = hasStart && hasDestination && canAddVia,
                onClick = { onRoleSelected(CourseWaypointRole.Via) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-24).dp, y = 32.dp),
            )
        }
    }
}

/** 입력 칸 앞의 지점 표시. 지도 위 핀이 아니라 색 점이다(디자인 3659:79169). */
@Composable
private fun WaypointDot(type: RegistrationWaypointType, dimmed: Boolean) {
    Image(
        painter = painterResource(
            when (type) {
                RegistrationWaypointType.START -> R.drawable.ic_registration_dot_start
                RegistrationWaypointType.VIA -> R.drawable.ic_registration_dot_via
                RegistrationWaypointType.DESTINATION -> R.drawable.ic_registration_dot_destination
            },
        ),
        contentDescription = null,
        alpha = if (dimmed) 0.5f else 1f,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun WaypointActionIcon(
    add: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 디자인(icon 24/plus-circle)의 바인딩 변수 기준: 원 배경 white, 원 테두리 gray300,
    // +/− 글리프 gray600 — 서로 다른 회색이다. 예전엔 테두리와 글리프가 같은 색이라 테두리가
    // 디자인보다 진하게 보였다.
    val glyphColor = if (enabled) RodiTheme.colors.gray600 else RodiTheme.colors.gray300
    // 디자인에 비활성 테두리 색 정의가 없어 활성/비활성 모두 gray300으로 둔다(기존 동작 유지).
    val borderColor = RodiTheme.colors.gray300
    val backgroundColor = RodiTheme.colors.white
    Box(
        modifier = modifier
            .requiredSize(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = if (add) "경유지 추가/선택" else "경유지 삭제"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 1.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            // 두 칸 경계에 걸쳐 놓기 때문에 배경을 깔지 않으면 칸 테두리가 아이콘을 관통한다.
            drawCircle(color = backgroundColor, radius = size.minDimension / 2f)
            drawLine(
                color = glyphColor,
                start = Offset(center.x - 5.dp.toPx(), center.y),
                end = Offset(center.x + 5.dp.toPx(), center.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = borderColor,
                radius = size.minDimension / 2f - stroke / 2f,
                style = Stroke(width = stroke),
            )
            if (add) {
                drawLine(
                    color = glyphColor,
                    start = Offset(center.x, center.y - 5.dp.toPx()),
                    end = Offset(center.x, center.y + 5.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun pendingAddressLabel(suggestion: CourseLocationSuggestion?, isLoading: Boolean): String? = when {
    isLoading -> "위치를 확인하고 있어요"
    suggestion != null -> suggestion.address.ifBlank { suggestion.title }
    else -> null
}

@Composable
private fun WaypointSelectionRow(
    type: RegistrationWaypointType,
    waypoint: RegistrationWaypoint?,
    onClick: () -> Unit,
    trailing: (@Composable (() -> Unit))? = null,
    pendingLabel: String? = null,
    label: String = when (type) {
        RegistrationWaypointType.START -> "출발지"
        RegistrationWaypointType.VIA -> "경유지"
        RegistrationWaypointType.DESTINATION -> "도착지"
    },
) {
    val placeholder = when (type) {
        RegistrationWaypointType.START -> "출발지 입력"
        RegistrationWaypointType.VIA -> "경유지 입력"
        RegistrationWaypointType.DESTINATION -> "도착지 입력"
    }
    val shape = RoundedCornerShape(RodiRadius.sm)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(shape)
            .background(RodiTheme.colors.white, shape)
            .border(1.dp, RodiTheme.colors.gray300, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics {
                contentDescription = if (waypoint == null) "$placeholder 선택" else "$label " + waypoint.name + " 선택"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WaypointDot(type = type, dimmed = waypoint == null && pendingLabel == null)
        Text(
            text = waypoint?.address?.ifBlank { waypoint.name } ?: pendingLabel ?: placeholder,
            style = RodiTheme.typography.body2Medium,
            color = when {
                waypoint != null -> RodiTheme.colors.black
                pendingLabel != null -> RodiTheme.colors.gray600
                else -> RodiTheme.colors.gray500
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun FixedCenterPin(
    role: CourseWaypointRole,
    modifier: Modifier = Modifier,
) {
    val resource = when (role) {
        CourseWaypointRole.Start -> R.drawable.ic_registration_pin_start
        CourseWaypointRole.Via -> R.drawable.ic_registration_pin_via
        CourseWaypointRole.Destination -> R.drawable.ic_registration_pin_destination
    }
    val pinSize = if (role == CourseWaypointRole.Via) 30.dp else 34.dp
    val shadowColor = when (role) {
        CourseWaypointRole.Start -> RodiTheme.semantic.pinStart.copy(alpha = 0.5f)
        CourseWaypointRole.Via -> RodiTheme.semantic.pinVia.copy(alpha = 0.8f)
        CourseWaypointRole.Destination -> RodiTheme.colors.infoCancel.copy(alpha = 0.5f)
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 아직 놓지 않은 핀에는 바닥 그림자가 붙는다(디자인 3659:79158). 핀 끝이 지도 중심에
        // 오도록 그림자 높이(2dp)만큼 보정해서 위로 올린다.
        Box(
            modifier = Modifier
                .size(width = pinSize, height = pinSize + 2.dp)
                .offset(y = -(pinSize / 2) + 1.dp),
        ) {
            Canvas(
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 20.dp, height = 6.dp),
            ) {
                drawOval(color = shadowColor)
            }
            Image(
                painter = painterResource(resource),
                contentDescription = when (role) {
                    CourseWaypointRole.Start -> "출발지 선택 위치"
                    CourseWaypointRole.Via -> "경유지 선택 위치"
                    CourseWaypointRole.Destination -> "도착지 선택 위치"
                },
                modifier = Modifier.align(Alignment.TopCenter).size(pinSize),
            )
        }
    }
}

@Composable
private fun CourseRegistrationSearchField(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(RodiTheme.colors.gray200, RoundedCornerShape(RodiRadius.sm))
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = "등록 장소 검색" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RodiIconButton(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            onClick = onBack,
            contentDescription = "검색 닫기",
            tint = RodiTheme.colors.black,
        )
        BasicTextField(
            value = keyword,
            onValueChange = onKeywordChanged,
            modifier = Modifier.weight(1f),
            textStyle = RodiTheme.typography.body2Medium.copy(color = RodiTheme.colors.black),
            singleLine = true,
            cursorBrush = rodiCursorBrush(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (keyword.isBlank()) {
                        Text(
                            text = placeholder,
                            style = RodiTheme.typography.body2Medium,
                            color = RodiTheme.colors.gray500,
                        )
                    }
                    inner()
                }
            },
        )
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

@Composable
private fun CourseRegistrationMapLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(RodiTheme.colors.white.copy(alpha = 0.92f), RoundedCornerShape(RodiRadius.md)),
            contentAlignment = Alignment.Center,
        ) {
            CourseRegistrationLoadingIndicator()
        }
    }
}

@Composable
private fun CourseRegistrationMapFormLoading(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RodiTheme.colors.white, RectangleShape)
            .navigationBarsPadding()
            .imePadding()
            .padding(RodiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RodiSkeleton(Modifier.fillMaxWidth().height(44.dp), RoundedCornerShape(RodiRadius.sm))
        RodiSkeleton(Modifier.fillMaxWidth().height(44.dp), RoundedCornerShape(RodiRadius.sm))
        RodiSkeleton(Modifier.fillMaxWidth().height(48.dp), RoundedCornerShape(RodiRadius.sm))
    }
}

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
private fun SearchLoadingContent() {
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
private fun RecentSearchContent(
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
private fun RecentSearchLoadingContent() {
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
private fun SearchNoResultContent(keyword: String) {
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
private fun SearchResultsContent(
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
private fun SearchRow(
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
private fun SearchMessage(
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

@Composable
private fun CourseRegistrationMapError(modifier: Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier
            .background(RodiTheme.colors.white, RoundedCornerShape(RodiRadius.md))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("지도를 불러오지 못했어요", style = RodiTheme.typography.body3SemiBold, color = RodiTheme.colors.black)
        Spacer(Modifier.height(12.dp))
        RodiButton(
            text = "다시 시도",
            onClick = onRetry,
            fillMaxWidth = false,
            modifier = Modifier.width(120.dp),
            height = 42.dp,
        )
    }
}

@Composable
private fun CourseRegistrationPinEditBar(
    modifier: Modifier,
    waypoint: RegistrationWaypoint?,
    originalPoint: GeoPoint?,
    temporaryPin: GeoPoint?,
    mapCenter: GeoPoint?,
    isLoading: Boolean,
    pendingSuggestion: CourseLocationSuggestion?,
    isPendingAddressLoading: Boolean,
    onSelect: () -> Unit,
    onReset: () -> Unit,
    onCommit: () -> Unit,
) {
    val hasTemporarySelection = temporaryPin != null && temporaryPin != originalPoint
    val targetLabel = when (waypoint?.type) {
        RegistrationWaypointType.START -> "출발지 선택"
        RegistrationWaypointType.VIA -> "경유지 선택"
        RegistrationWaypointType.DESTINATION -> "도착지 선택"
        null -> "위치 선택"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RodiTheme.colors.white, RectangleShape)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = RodiSpacing.md, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        RodiButton(
            text = if (hasTemporarySelection) "다시하기" else targetLabel,
            onClick = if (hasTemporarySelection) onReset else onSelect,
            variant = RodiButtonVariant.Secondary,
            modifier = Modifier.weight(1f),
            height = 48.dp,
            enabled = !isLoading && (
                hasTemporarySelection ||
                    (mapCenter != null && !isPendingAddressLoading && pendingSuggestion != null)
                ),
        )
        RodiButton(
            text = "완료",
            onClick = onCommit,
            modifier = Modifier.weight(1f),
            height = 48.dp,
            enabled = !isLoading && hasTemporarySelection,
        )
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
