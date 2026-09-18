package com.dororong.rodi.feature.course.registration.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.CourseRegistrationLoadingIndicator
import com.dororong.rodi.feature.course.registration.CourseWaypointRole
import com.dororong.rodi.feature.course.registration.R

@Composable
internal fun CourseRegistrationMapLoadingOverlay() {
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
internal fun CourseRegistrationMapFormLoading(modifier: Modifier) {
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
internal fun CourseRegistrationMapError(modifier: Modifier, onRetry: () -> Unit) {
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
internal fun CourseRegistrationPinEditBar(
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

@Composable
internal fun FixedCenterPin(
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
