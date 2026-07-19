package com.dororong.rodi.feature.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.TransformMethod

private data object CurrentLocationLabelTag

fun KakaoMap.renderCurrentLocationMarker(
    context: Context,
    location: LatLng,
    heading: Float?,
    @ColorInt markerColor: Int,
) {
    val manager = labelManager ?: return
    val layer = currentLocationLabelLayer() ?: return
    val marker = layer.allLabels.firstOrNull { it.tag == CurrentLocationLabelTag }
        ?: layer.addLabel(
            LabelOptions.from(location)
                .setStyles(manager.currentLocationStyles(context, markerColor))
                .setTransform(TransformMethod.AbsoluteRotation)
                .setTag(CurrentLocationLabelTag),
        )
    if (marker.position != location) {
        marker.moveTo(location, CURRENT_LOCATION_MOVE_DURATION_MILLIS)
    }
    heading?.let { marker.rotateTo(it, HEADING_ROTATE_DURATION_MILLIS) }
}

private fun LabelManager.currentLocationStyles(
    context: Context,
    @ColorInt markerColor: Int,
): LabelStyles {
    val styles = LabelStyles.from(
        LabelStyle.from(createCurrentLocationMarkerBitmap(context.resources.displayMetrics.density, markerColor))
            .setAnchorPoint(0.5f, currentLocationMarkerAnchorY()),
    )
    return addLabelStyles(styles) ?: styles
}

internal fun currentLocationMarkerAnchorY(): Float =
    CURRENT_LOCATION_CIRCLE_CENTER_Y_DP / CURRENT_LOCATION_MARKER_HEIGHT_DP

internal fun createCurrentLocationMarkerBitmap(
    density: Float,
    @ColorInt markerColor: Int,
): Bitmap {
    val width = (CURRENT_LOCATION_MARKER_WIDTH_DP * density).toInt().coerceAtLeast(1)
    val height = (CURRENT_LOCATION_MARKER_HEIGHT_DP * density).toInt().coerceAtLeast(1)
    return createBitmap(width, height).also { bitmap ->
        val scale = width / CURRENT_LOCATION_MARKER_WIDTH_DP
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = markerColor
            setShadowLayer(
                CURRENT_LOCATION_SHADOW_RADIUS_DP * scale,
                0f,
                0f,
                CURRENT_LOCATION_SHADOW_COLOR,
            )
        }
        val triangle = Path().apply {
            moveTo(10f, 3f)
            lineTo(14f, 9f)
            lineTo(6f, 9f)
            close()
        }
        canvas.drawPath(triangle, shadowPaint)
        shadowPaint.clearShadowLayer()
        canvas.drawPath(triangle, shadowPaint)

        val centerX = CURRENT_LOCATION_MARKER_WIDTH_DP / 2f
        val centerY = CURRENT_LOCATION_CIRCLE_CENTER_Y_DP
        shadowPaint.setShadowLayer(
            CURRENT_LOCATION_SHADOW_RADIUS_DP * scale,
            0f,
            0f,
            CURRENT_LOCATION_SHADOW_COLOR,
        )
        canvas.drawCircle(centerX, centerY, CURRENT_LOCATION_CIRCLE_RADIUS_DP, shadowPaint)
        shadowPaint.clearShadowLayer()
        canvas.drawCircle(centerX, centerY, CURRENT_LOCATION_CIRCLE_RADIUS_DP, shadowPaint)

        canvas.drawCircle(
            centerX,
            centerY,
            CURRENT_LOCATION_CIRCLE_STROKE_RADIUS_DP,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = CURRENT_LOCATION_CIRCLE_STROKE_WIDTH_DP
            },
        )
    }
}

private const val CURRENT_LOCATION_MOVE_DURATION_MILLIS = 250
private const val HEADING_ROTATE_DURATION_MILLIS = 120
private const val CURRENT_LOCATION_MARKER_WIDTH_DP = 20f
private const val CURRENT_LOCATION_MARKER_HEIGHT_DP = 28f
private const val CURRENT_LOCATION_CIRCLE_CENTER_Y_DP = 18f
private const val CURRENT_LOCATION_CIRCLE_RADIUS_DP = 7f
private const val CURRENT_LOCATION_CIRCLE_STROKE_RADIUS_DP = 6f
private const val CURRENT_LOCATION_CIRCLE_STROKE_WIDTH_DP = 2f
private const val CURRENT_LOCATION_SHADOW_RADIUS_DP = 1f
private const val CURRENT_LOCATION_SHADOW_COLOR = 0x4D000000
