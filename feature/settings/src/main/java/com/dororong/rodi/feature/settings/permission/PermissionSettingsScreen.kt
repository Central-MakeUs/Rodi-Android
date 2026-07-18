package com.dororong.rodi.feature.settings.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.permission.LocationPermissionAction
import com.dororong.rodi.core.ui.permission.resolveLocationPermissionAction
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.settings.SettingsTopBar

@Composable
fun PermissionSettingsScreen(
    onBack: () -> Unit,
    viewModel: PermissionSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val hasRequestedLocationPermission by viewModel.hasRequestedLocationPermission
        .collectAsStateWithLifecycle(initialValue = false)
    var isLocationGranted by remember(context) { mutableStateOf(context.hasLocationPermission()) }
    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        isLocationGranted = permissions.values.any { it }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isLocationGranted = context.hasLocationPermission()
    }

    PermissionSettingsContent(
        isLocationGranted = isLocationGranted,
        onBack = onBack,
        onLocationClick = {
            when (
                resolveLocationPermissionAction(
                    isLocationGranted = isLocationGranted,
                    hasRequestedLocationPermission = hasRequestedLocationPermission,
                    shouldShowRationale = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    } ?: false,
                )
            ) {
                LocationPermissionAction.RequestSystemPermission -> {
                    viewModel.markLocationPermissionRequested()
                    requestLocationPermission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }

                LocationPermissionAction.OpenAppSettings -> {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            }
        },
    )
}

@Composable
private fun PermissionSettingsContent(
    isLocationGranted: Boolean,
    onBack: () -> Unit,
    onLocationClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RodiTheme.colors.white,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            SettingsTopBar(title = "권한 설정 변경", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLocationClick)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "위치",
                        style = RodiTheme.typography.body1Medium,
                        color = RodiTheme.colors.black,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (isLocationGranted) "허용됨" else "허용 안 됨",
                        style = RodiTheme.typography.body1Medium,
                        color = RodiTheme.colors.gray600,
                    )
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = RodiTheme.colors.gray600,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp),
                    )
                }
                Text(
                    text = "내 주변 운전 연습 코스를 추천하기 위해 필요해요.",
                    style = RodiTheme.typography.caption2Medium,
                    color = RodiTheme.colors.gray600,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun PermissionSettingsGrantedPreview() {
    RodiTheme {
        PermissionSettingsContent(
            isLocationGranted = true,
            onBack = {},
            onLocationClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun PermissionSettingsDeniedPreview() {
    RodiTheme {
        PermissionSettingsContent(
            isLocationGranted = false,
            onBack = {},
            onLocationClick = {},
        )
    }
}
