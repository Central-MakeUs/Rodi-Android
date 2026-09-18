package com.dororong.rodi.feature.course.registration.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.CourseWaypointRole
import com.dororong.rodi.feature.course.registration.R

@Composable
internal fun CourseRegistrationWaypointCard(
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
internal fun WaypointDot(type: RegistrationWaypointType, dimmed: Boolean) {
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
internal fun WaypointActionIcon(
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

@Composable
internal fun WaypointSelectionRow(
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

internal fun pendingAddressLabel(suggestion: CourseLocationSuggestion?, isLoading: Boolean): String? = when {
    isLoading -> "위치를 확인하고 있어요"
    suggestion != null -> suggestion.address.ifBlank { suggestion.title }
    else -> null
}

@Composable
internal fun PinEditAddressRow(
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
