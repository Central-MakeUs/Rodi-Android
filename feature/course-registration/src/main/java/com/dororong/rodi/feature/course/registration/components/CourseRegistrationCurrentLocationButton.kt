package com.dororong.rodi.feature.course.registration.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.dororong.rodi.core.ui.permission.hasLocationPermission
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.CourseRegistrationIntent
import com.dororong.rodi.feature.course.registration.R
import com.dororong.rodi.feature.course.registration.location.awaitRegistrationCurrentLocation
import kotlinx.coroutines.launch

@Composable
fun CourseRegistrationCurrentLocationButton(
    onIntent: (CourseRegistrationIntent) -> Unit,
    modifier: Modifier = Modifier,
    autoRequest: Boolean = false,
    visible: Boolean = true,
) {
    val context = LocalContext.current
    if (LocalInspectionMode.current) {
        CourseRegistrationCurrentLocationVisual(modifier = modifier, isLoading = false, onClick = {})
        return
    }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    lateinit var loadCurrentLocation: () -> Unit

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) loadCurrentLocation()
        else onIntent(CourseRegistrationIntent.LocationUnavailable)
    }

    loadCurrentLocation = fun() {
        if (isLoading) return
        if (!context.hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
            return
        }
        isLoading = true
        scope.launch {
            val point = context.awaitRegistrationCurrentLocation()
            isLoading = false
            if (point == null) onIntent(CourseRegistrationIntent.LocationUnavailable)
            else onIntent(CourseRegistrationIntent.CurrentLocationSelected(point))
        }
    }

    LaunchedEffect(autoRequest) {
        if (autoRequest) {
            loadCurrentLocation()
        }
    }

    if (!visible) return

    CourseRegistrationCurrentLocationVisual(
        modifier = modifier,
        isLoading = isLoading,
        onClick = loadCurrentLocation,
    )
}

@Composable
private fun CourseRegistrationCurrentLocationVisual(
    modifier: Modifier,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isLoading) "현재 위치 확인 중" else "현재 위치"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(RodiTheme.colors.white),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_registration_crosshair),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (isLoading) RodiTheme.colors.primary600 else RodiTheme.colors.gray900),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
