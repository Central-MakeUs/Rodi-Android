package com.dororong.rodi.feature.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.dororong.rodi.feature.home.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.TransformMethod

private sealed interface CurrentLocationLabelTag {
    data object Base : CurrentLocationLabelTag
    data object Heading : CurrentLocationLabelTag
}

fun KakaoMap.renderCurrentLocationMarker(
    context: Context,
    location: LatLng,
    heading: Float?,
) {
    val manager = labelManager ?: return
    val layer = currentLocationLabelLayer() ?: return
    val labels = layer.allLabels
    val base = labels.firstOrNull { it.tag == CurrentLocationLabelTag.Base }
        ?: layer.addLabel(
            LabelOptions.from(location)
                .setStyles(manager.currentLocationStyles(context, R.drawable.ic_map_current_location_base))
                .setTag(CurrentLocationLabelTag.Base),
        )
    if (base.position != location) {
        base.moveTo(location, CURRENT_LOCATION_MOVE_DURATION_MILLIS)
    }

    val headingLabel = labels.firstOrNull { it.tag == CurrentLocationLabelTag.Heading }
    if (heading == null) {
        headingLabel?.hide()
        return
    }

    val arrow = headingLabel ?: layer.addLabel(
        LabelOptions.from(location)
            .setStyles(manager.currentLocationStyles(context, R.drawable.ic_map_current_location_heading))
            .setTransform(TransformMethod.AbsoluteRotation)
            .setTag(CurrentLocationLabelTag.Heading),
    )
    arrow.show()
    if (arrow.position != location) {
        arrow.moveTo(location, CURRENT_LOCATION_MOVE_DURATION_MILLIS)
    }
    arrow.rotateTo(heading, HEADING_ROTATE_DURATION_MILLIS)
}

private fun LabelManager.currentLocationStyles(
    context: Context,
    drawableRes: Int,
): LabelStyles {
    val styles = LabelStyles.from(
        LabelStyle.from(context.currentLocationDrawableToBitmap(drawableRes))
            .setAnchorPoint(0.5f, 0.5f),
    )
    return addLabelStyles(styles) ?: styles
}

private fun Context.currentLocationDrawableToBitmap(drawableRes: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(this, drawableRes) ?: return createBitmap(1, 1)
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    return createBitmap(width, height).also { bitmap ->
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(bitmap))
    }
}

private const val CURRENT_LOCATION_MOVE_DURATION_MILLIS = 250
private const val HEADING_ROTATE_DURATION_MILLIS = 120
