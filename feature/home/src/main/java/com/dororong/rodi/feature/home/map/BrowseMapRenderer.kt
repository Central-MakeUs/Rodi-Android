package com.dororong.rodi.feature.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.dororong.rodi.core.domain.model.place.PlaceBounds
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.feature.home.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import java.util.WeakHashMap

sealed interface BrowseLabelTag {
    data class Cluster(val bounds: PlaceBounds) : BrowseLabelTag

    data class Place(val id: Long) : BrowseLabelTag
}

/**
 * 카메라가 멈출 때마다 호출된다. 전부 지우고 다시 그리면 마커가 깜빡이므로, 이미 그린 마커는 두고
 * 사라진 것만 지우고 새로 생긴 것만 더한다. 마커 이미지도 같은 내용이면 다시 만들지 않는다.
 */
internal fun KakaoMap.renderClusters(
    context: Context,
    markers: List<BrowseMapMarker>,
    style: MapBitmapStyle,
) {
    val manager = labelManager ?: return
    val layer = browseLabelLayer() ?: return
    var state = browseRenderStates[this]
    if (state == null || state.style != style || state.labels.values.any { !layer.hasLabel(it) }) {
        layer.removeAll()
        state = BrowseRenderState(style)
        browseRenderStates[this] = state
    }
    state.selectedParkingId?.let { parkingId ->
        changeParkingMarkerStyle(context, parkingId, R.drawable.ic_pin_parking_default)
        state.selectedParkingId = null
    }

    val desired = markers.toSet()
    val removed = state.labels.keys.filterNot(desired::contains)
    removed.forEach { marker -> state.labels.remove(marker)?.let { label -> layer.remove(label) } }

    val density = context.resources.displayMetrics.density
    markers.filterNot(state.labels::containsKey).forEach { marker ->
        val label = when (marker) {
            is BrowseMapMarker.Cluster -> {
                val styles = state.clusterStyles.getOrPut(marker.count) {
                    manager.addLabelStyles(
                        LabelStyles.from(LabelStyle.from(createClusterTooltipBitmap(marker.count, density, style))),
                    )
                }
                layer.addLabel(
                    LabelOptions.from(LatLng.from(marker.point.lat, marker.point.lng))
                        .setStyles(styles)
                        .setClickable(true)
                        .setTag(BrowseLabelTag.Cluster(marker.bounds)),
                )
            }

            is BrowseMapMarker.Place -> {
                val place = marker.place
                val styles = if (place.type == PlaceType.PARKING) {
                    state.parkingStyles ?: manager.parkingMarkerStyles(context, R.drawable.ic_pin_parking_default)
                        .also { state.parkingStyles = it }
                } else {
                    state.courseChipStyles.getOrPut(place.name) {
                        manager.addLabelStyles(
                            LabelStyles.from(LabelStyle.from(createCourseChipBitmap(place.name, density, style))),
                        )
                    }
                }
                layer.addLabel(
                    LabelOptions.from(LatLng.from(place.point.lat, place.point.lng))
                        .setStyles(styles)
                        .setClickable(true)
                        .setTag(BrowseLabelTag.Place(place.id)),
                )
            }
        }
        if (label != null) state.labels[marker] = label
    }
}

/** 지도마다 지금 그려 둔 탐색 마커와 재사용할 마커 이미지를 기억한다. */
private class BrowseRenderState(val style: MapBitmapStyle) {
    val labels = LinkedHashMap<BrowseMapMarker, Label>()
    val clusterStyles = HashMap<Int, LabelStyles?>()
    val courseChipStyles = HashMap<String, LabelStyles?>()
    var parkingStyles: LabelStyles? = null
    var selectedParkingId: Long? = null
}

private val browseRenderStates = WeakHashMap<KakaoMap, BrowseRenderState>()

internal fun KakaoMap.forgetBrowseRenderState() {
    browseRenderStates.remove(this)
}

/** 마커를 다시 만들지 않고 재사용하므로, 다른 주차장으로 바꿀 때 이전 선택을 직접 되돌린다. */
fun KakaoMap.selectParkingMarker(context: Context, parkingId: Long): Boolean {
    val state = browseRenderStates[this]
    val previousId = state?.selectedParkingId
    if (previousId != null && previousId != parkingId) {
        changeParkingMarkerStyle(context, previousId, R.drawable.ic_pin_parking_default)
        state.selectedParkingId = null
    }
    return changeParkingMarkerStyle(context, parkingId, R.drawable.ic_pin_parking_selected).also { selected ->
        state?.selectedParkingId = if (selected) parkingId else null
    }
}

fun KakaoMap.deselectParkingMarker(
    context: Context,
    parkingId: Long,
): Boolean {
    return changeParkingMarkerStyle(context, parkingId, R.drawable.ic_pin_parking_default)
}

private fun KakaoMap.changeParkingMarkerStyle(
    context: Context,
    parkingId: Long,
    drawableRes: Int,
): Boolean {
    val manager = labelManager ?: return false
    val layer = browseLabelLayer() ?: return false
    val label = layer.allLabels.firstOrNull { it.tag == BrowseLabelTag.Place(parkingId) } ?: return false
    val labelsAtSamePosition = layer.allLabels.filter { candidate ->
        candidate !== label &&
            candidate.tag is BrowseLabelTag.Place &&
            candidate.position.latitude == label.position.latitude &&
            candidate.position.longitude == label.position.longitude
    }
    if (drawableRes == R.drawable.ic_pin_parking_selected) {
        labelsAtSamePosition.forEach { it.hide() }
    } else {
        labelsAtSamePosition.forEach { it.show() }
    }
    label.hide()
    label.setStyles(manager.parkingMarkerStyles(context, drawableRes))
    label.invalidate(false)
    label.show()
    return true
}

fun KakaoMap.renderSelectedParkingMarker(context: Context, parking: PlaceCoordinate) {
    val manager = labelManager ?: return
    val layer = browseLabelLayer() ?: return
    clearBrowseLabels()
    layer.addLabel(
        LabelOptions.from(LatLng.from(parking.point.lat, parking.point.lng))
            .setStyles(
                manager.parkingMarkerStyles(context, R.drawable.ic_pin_parking_selected),
            )
            .setTag(BrowseLabelTag.Place(parking.id)),
    )
}

private fun createClusterTooltipBitmap(
    count: Int,
    density: Float,
    style: MapBitmapStyle,
): Bitmap {
    val horizontalPadding = 10 * density
    val verticalPadding = 4 * density
    val tailWidth = 14 * density
    val tailHeight = 8 * density
    val cornerRadius = 8 * density
    val shadowPadding = 3 * density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.clusterText.color
        textSize = style.clusterText.textSizePx
        textAlign = Paint.Align.CENTER
        typeface = style.clusterText.typeface
    }
    val fontMetrics = textPaint.fontMetrics
    val bubbleHeight = ((fontMetrics.descent - fontMetrics.ascent) + verticalPadding * 2).toInt()
    val bubbleWidth = (textPaint.measureText(count.toString()) + horizontalPadding * 2).toInt()
    val width = (bubbleWidth + shadowPadding * 2).toInt().coerceAtLeast(1)
    val height = (bubbleHeight + tailHeight + shadowPadding * 2).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val bubbleLeft = shadowPadding
    val bubbleTop = shadowPadding
    val bubbleRight = bubbleLeft + bubbleWidth
    val bubbleBottom = bubbleTop + bubbleHeight
    val silhouette = createClusterSilhouette(
        left = bubbleLeft,
        top = bubbleTop,
        right = bubbleRight,
        bottom = bubbleBottom,
        cornerRadius = cornerRadius,
        tailWidth = tailWidth,
        tailHeight = tailHeight,
    )
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.clusterBackgroundColor
        setShadowLayer(1.5f * density, 0f, 0f, style.clusterShadowColor)
    }
    canvas.drawPath(silhouette, shadowPaint)
    shadowPaint.clearShadowLayer()
    canvas.drawPath(silhouette, shadowPaint)
    val baseline = bubbleTop + bubbleHeight / 2f - (fontMetrics.descent + fontMetrics.ascent) / 2f
    canvas.drawText(count.toString(), width / 2f, baseline, textPaint)
    return bitmap
}

internal fun createClusterSilhouette(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float,
    tailWidth: Float,
    tailHeight: Float,
): Path {
    val centerX = (left + right) / 2f
    val tailLeft = centerX - tailWidth / 2f
    val tailRight = centerX + tailWidth / 2f
    val geometry = clusterSilhouetteGeometry(bottom)
    return Path().apply {
        moveTo(left + cornerRadius, top)
        lineTo(right - cornerRadius, top)
        quadTo(right, top, right, top + cornerRadius)
        lineTo(right, geometry.bodyBottom - cornerRadius)
        quadTo(right, geometry.bodyBottom, right - cornerRadius, geometry.bodyBottom)
        lineTo(tailRight, geometry.tailTop)
        lineTo(centerX, geometry.tailTop + tailHeight)
        lineTo(tailLeft, geometry.tailTop)
        lineTo(left + cornerRadius, geometry.bodyBottom)
        quadTo(left, geometry.bodyBottom, left, geometry.bodyBottom - cornerRadius)
        lineTo(left, top + cornerRadius)
        quadTo(left, top, left + cornerRadius, top)
        close()
    }
}

internal data class ClusterSilhouetteGeometry(
    val bodyBottom: Float,
    val tailTop: Float,
)

internal fun clusterSilhouetteGeometry(bodyBottom: Float): ClusterSilhouetteGeometry =
    ClusterSilhouetteGeometry(bodyBottom = bodyBottom, tailTop = bodyBottom)

private fun LabelManager.parkingMarkerStyles(
    context: Context,
    drawableRes: Int,
): LabelStyles {
    return parkingMarkerStyles(
        bitmap = context.drawableToBitmap(drawableRes),
        anchorY = if (drawableRes == R.drawable.ic_pin_parking_selected) 1f else 0.5f,
    )
}

private fun LabelManager.parkingMarkerStyles(
    bitmap: Bitmap,
    anchorY: Float,
): LabelStyles {
    val styles = LabelStyles.from(
        LabelStyle.from(bitmap)
            .setAnchorPoint(0.5f, anchorY),
    )
    return addLabelStyles(styles) ?: styles
}

private fun Context.drawableToBitmap(drawableRes: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(this, drawableRes) ?: return createBitmap(1, 1)
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    return createBitmap(width, height).also { bitmap ->
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(bitmap))
    }
}
