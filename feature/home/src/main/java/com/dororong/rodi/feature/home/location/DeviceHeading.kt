package com.dororong.rodi.feature.home.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun rememberDeviceHeading(): State<Float?> {
    val context = LocalContext.current
    val heading = remember { mutableStateOf<Float?>(null) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                private val rotationMatrix = FloatArray(9)
                private val remappedMatrix = FloatArray(9)
                private val orientation = FloatArray(3)
                private var lastPublishedHeading: Float? = null
                private var lastPublishedAtMillis = 0L

                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val (axisX, axisY) = when (context.display?.rotation ?: Surface.ROTATION_0) {
                        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                    }
                    SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
                    SensorManager.getOrientation(remappedMatrix, orientation)
                    val measuredHeading = ((Math.toDegrees(orientation[0].toDouble()) + 360) % 360)
                        .roundToInt()
                        .toFloat()
                    val now = SystemClock.elapsedRealtime()
                    val previousHeading = lastPublishedHeading
                    if (
                        previousHeading == null ||
                        (
                            angularDistance(measuredHeading, previousHeading) >= HEADING_CHANGE_THRESHOLD_DEGREES &&
                                now - lastPublishedAtMillis >= HEADING_UPDATE_DEBOUNCE_MILLIS
                            )
                    ) {
                        heading.value = measuredHeading
                        lastPublishedHeading = measuredHeading
                        lastPublishedAtMillis = now
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    return heading
}

private fun angularDistance(first: Float, second: Float): Float =
    abs((first - second + 540f) % 360f - 180f)

private const val HEADING_CHANGE_THRESHOLD_DEGREES = 6f
private const val HEADING_UPDATE_DEBOUNCE_MILLIS = 250L
