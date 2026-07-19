package com.dororong.rodi.feature.home.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
                    heading.value = ((Math.toDegrees(orientation[0].toDouble()) + 360) % 360)
                        .roundToInt()
                        .toFloat()
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    return heading
}
