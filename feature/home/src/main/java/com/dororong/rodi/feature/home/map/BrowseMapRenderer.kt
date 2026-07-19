package com.dororong.rodi.feature.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.feature.home.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

sealed interface BrowseLabelTag {
    data class Cluster(
        val point: GeoPoint,
        val targetZoom: Int,
    ) : BrowseLabelTag

    data class Place(val id: Long) : BrowseLabelTag
}

internal fun KakaoMap.renderClusters(
    context: Context,
    clusters: List<MapCluster>,
    placesById: Map<Long, PlaceCoordinate>,
    style: MapBitmapStyle,
) {
    clearBrowseLabels()
    val manager = labelManager ?: return
    val layer = browseLabelLayer() ?: return
    val parkingStyles by lazy { manager.parkingMarkerStyles(context, R.drawable.ic_pin_parking_default) }

    clusters.forEach { cluster ->
        if (cluster.isClusterMarker) {
            val bitmap = createClusterTooltipBitmap(
                count = cluster.count,
                density = context.resources.displayMetrics.density,
                style = style,
            )
            val styles = manager.addLabelStyles(LabelStyles.from(LabelStyle.from(bitmap)))
            layer.addLabel(
                LabelOptions.from(LatLng.from(cluster.representativePoint.lat, cluster.representativePoint.lng))
                    .setStyles(styles)
                    .setClickable(true)
                    .setTag(BrowseLabelTag.Cluster(cluster.representativePoint, cluster.targetZoom)),
            )
        } else {
            val place = placesById[cluster.memberIds.single()] ?: return@forEach
            val styles = if (place.type == PlaceType.PARKING) {
                parkingStyles
            } else {
                manager.addLabelStyles(
                    LabelStyles.from(
                        LabelStyle.from(
                            createCourseChipBitmap(
                                text = place.name,
                                density = context.resources.displayMetrics.density,
                                style = style,
                            ),
                        ),
                    ),
                )
            }
            layer.addLabel(
                LabelOptions.from(LatLng.from(place.point.lat, place.point.lng))
                    .setStyles(styles)
                    .setClickable(true)
                    .setTag(BrowseLabelTag.Place(place.id)),
            )
        }
    }
}

fun KakaoMap.selectParkingMarker(context: Context, parkingId: Long): Boolean =
    changeParkingMarkerStyle(context, parkingId, R.drawable.ic_pin_parking_selected)

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
    label.changeStyles(manager.parkingMarkerStyles(context, drawableRes), false)
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

internal fun KakaoMap.renderIndividualMarkers(
    context: Context,
    places: List<PlaceCoordinate>,
    style: MapBitmapStyle,
) {
    val clusters = places.map { place ->
        MapCluster(
            memberIds = listOf(place.id),
            representativePoint = place.point,
            targetZoom = 14,
        )
    }
    renderClusters(
        context = context,
        clusters = clusters,
        placesById = places.associateBy(PlaceCoordinate::id),
        style = style,
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
