package com.dororong.rodi.feature.settings.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.R as CoreUiR
import android.os.Build
import com.dororong.rodi.core.ui.permission.PermissionAction
import com.dororong.rodi.core.ui.permission.hasLocationPermission
import com.dororong.rodi.core.ui.permission.hasNotificationPermission
import com.dororong.rodi.core.ui.permission.resolvePermissionAction
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
    val hasRequestedNotificationPermission by viewModel.hasRequestedNotificationPermission
        .collectAsStateWithLifecycle(initialValue = false)
    var isLocationGranted by remember(context) { mutableStateOf(context.hasLocationPermission()) }
    var isNotificationGranted by remember(context) { mutableStateOf(context.hasNotificationPermission()) }
    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        isLocationGranted = permissions.values.any { it }
    }
    val requestNotificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isNotificationGranted = it }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isLocationGranted = context.hasLocationPermission()
        isNotificationGranted = context.hasNotificationPermission()
    }

    PermissionSettingsContent(
        isLocationGranted = isLocationGranted,
        isNotificationGranted = isNotificationGranted,
        onBack = onBack,
        onLocationClick = {
            when (
                resolvePermissionAction(
                    isGranted = isLocationGranted,
                    hasRequestedPermission = hasRequestedLocationPermission,
                    shouldShowRationale = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    } ?: false,
                )
            ) {
                PermissionAction.RequestSystemPermission -> {
                    viewModel.markLocationPermissionRequested()
                    requestLocationPermission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }

                PermissionAction.OpenAppSettings -> {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            }
        },
        onNotificationClick = {
            when (
                resolvePermissionAction(
                    isGranted = isNotificationGranted,
                    hasRequestedPermission = hasRequestedNotificationPermission,
                    shouldShowRationale = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            it,
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                    } ?: false,
                )
            ) {
                PermissionAction.RequestSystemPermission -> {
                    viewModel.markNotificationPermissionRequested()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                    }
                }
                PermissionAction.OpenAppSettings -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            }
        },
    )
}

@Composable
private fun PermissionSettingsContent(
    isLocationGranted: Boolean,
    isNotificationGranted: Boolean,
    onBack: () -> Unit,
    onLocationClick: () -> Unit,
    onNotificationClick: () -> Unit,
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
            PermissionRow("위치", isLocationGranted, "내 위치 확인과 주행 거리 측정에 사용해요.", onLocationClick)
            PermissionRow("주행 상태 알림", isNotificationGranted, "앱을 나가도 주행 상태와 진행률을 확인해요.", onNotificationClick)
        }
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, description: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black, modifier = Modifier.weight(1f))
            Text(if (granted) "허용됨" else "허용 필요", style = RodiTheme.typography.body1Medium, color = RodiTheme.colors.gray600)
            Icon(painterResource(CoreUiR.drawable.ic_chevron_right), null, tint = RodiTheme.colors.gray600, modifier = Modifier.padding(start = 8.dp).size(20.dp))
        }
        Text(description, style = RodiTheme.typography.caption2Medium, color = RodiTheme.colors.gray600, modifier = Modifier.padding(top = 8.dp))
    }
}

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
            isNotificationGranted = true,
            onBack = {},
            onLocationClick = {},
            onNotificationClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun PermissionSettingsDeniedPreview() {
    RodiTheme {
        PermissionSettingsContent(
            isLocationGranted = false,
            isNotificationGranted = false,
            onBack = {},
            onLocationClick = {},
            onNotificationClick = {},
        )
    }
}

@Preview(name = "권한 설정 혼재", showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun PermissionSettingsMixedPreview() {
    RodiTheme {
        PermissionSettingsContent(
            isLocationGranted = true,
            isNotificationGranted = false,
            onBack = {},
            onLocationClick = {},
            onNotificationClick = {},
        )
    }
}
