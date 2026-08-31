package com.dororong.rodi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.spike.driving.DrivingTrackingService
import com.dororong.rodi.ui.RodiApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val openDrivingArrival = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (intent?.action == DrivingTrackingService.ACTION_OPEN_ARRIVAL) {
            openDrivingArrival.value = true
        }
        setContent {
            RodiTheme {
                RodiApp(
                    openDrivingArrival = openDrivingArrival.value,
                    onDrivingArrivalHandled = { openDrivingArrival.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == DrivingTrackingService.ACTION_OPEN_ARRIVAL) {
            openDrivingArrival.value = true
        }
    }
}
