package com.dororong.rodi.feature.home.map

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
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
import java.util.WeakHashMap

sealed interface BrowseLabelTag {
    data class Cluster(
        val point: GeoPoint,
        val targetZoom: Int,
    ) : BrowseLabelTag

    data class Place(val id: Long) : BrowseLabelTag
}

fun KakaoMap.renderClusters(
    context: Context,
    clusters: List<MapCluster>,
    placesById: Map<Long, PlaceCoordinate>,
    @ColorInt backgroundColor: Int,
    @ColorInt textColor: Int,
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
                backgroundColor = backgroundColor,
                textColor = textColor,
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
                    LabelStyles.from(LabelStyle.from(createCourseChipBitmap(place.name, context.resources.displayMetrics.density))),
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

fun KakaoMap.animateParkingMarkerSelection(context: Context, parkingId: Long): Boolean {
    return animateParkingMarker(context, parkingId, target = 1f)
}

fun KakaoMap.animateParkingMarkerDeselection(
    context: Context,
    parkingId: Long,
    onFinished: () -> Unit,
): Boolean = animateParkingMarker(context, parkingId, target = 0f, onFinished = onFinished)

private fun KakaoMap.animateParkingMarker(
    context: Context,
    parkingId: Long,
    target: Float,
    onFinished: () -> Unit = {},
): Boolean {
    val manager = labelManager ?: return false
    val layer = browseLabelLayer() ?: return false
    val label = layer.allLabels.firstOrNull { it.tag == BrowseLabelTag.Place(parkingId) } ?: return false
    parkingMarkerAnimators.remove(label)?.cancel()
    val start = parkingMarkerProgress[label] ?: if (target == 1f) 0f else 1f
    ValueAnimator.ofFloat(start, target).apply {
        duration = parkingMarkerMorphDuration(start, target)
        interpolator = DecelerateInterpolator()
        addUpdateListener { animator ->
            val progress = animator.animatedValue as Float
            parkingMarkerProgress[label] = progress
            val styles = when (progress) {
                0f -> manager.parkingMarkerStyles(context, R.drawable.ic_pin_parking_default)
                1f -> manager.parkingMarkerStyles(
                    bitmap = createParkingMarkerMorphBitmap(context.resources.displayMetrics.density, 1f),
                    anchorY = 1f,
                )
                else -> manager.parkingMarkerStyles(
                    bitmap = createParkingMarkerMorphBitmap(context.resources.displayMetrics.density, progress),
                    anchorY = 0.5f + progress * 0.5f,
                )
            }
            label.changeStyles(styles, false)
        }
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parkingMarkerAnimators.remove(label)
                    parkingMarkerProgress[label] = target
                    onFinished()
                }

                override fun onAnimationCancel(animation: Animator) {
                    parkingMarkerAnimators.remove(label)
                }
            },
        )
        parkingMarkerAnimators[label] = this
        start()
    }
    return true
}

fun KakaoMap.renderSelectedParkingMarker(context: Context, parking: PlaceCoordinate) {
    val manager = labelManager ?: return
    val layer = browseLabelLayer() ?: return
    clearBrowseLabels()
    layer.addLabel(
        LabelOptions.from(LatLng.from(parking.point.lat, parking.point.lng))
            .setStyles(
                manager.parkingMarkerStyles(
                    bitmap = createParkingMarkerMorphBitmap(context.resources.displayMetrics.density, 1f),
                    anchorY = 1f,
                ),
            )
            .setTag(BrowseLabelTag.Place(parking.id)),
    )
}

fun KakaoMap.renderIndividualMarkers(context: Context, places: List<PlaceCoordinate>) {
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
        backgroundColor = 0,
        textColor = 0,
    )
}

private fun createClusterTooltipBitmap(
    count: Int,
    density: Float,
    @ColorInt backgroundColor: Int,
    @ColorInt textColor: Int,
): Bitmap {
    val horizontalPadding = 10 * density
    val verticalPadding = 4 * density
    val tailWidth = 14 * density
    val tailHeight = 8 * density
    val cornerRadius = 8 * density
    val shadowPadding = 3 * density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 14 * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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
        color = backgroundColor
        setShadowLayer(1.5f * density, 0f, 0f, 0x4D000000)
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

internal fun parkingMarkerMorphDuration(start: Float, target: Float): Long =
    (PARKING_MARKER_MORPH_DURATION_MILLIS * kotlin.math.abs(target - start))
        .toLong()
        .coerceAtLeast(1L)

private fun LabelManager.parkingMarkerStyles(
    context: Context,
    drawableRes: Int,
): LabelStyles {
    return parkingMarkerStyles(
        bitmap = context.drawableToBitmap(drawableRes),
        anchorY = if (drawableRes == R.drawable.ic_pin_parking_default) 0.5f else 1f,
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

private fun createParkingMarkerMorphBitmap(density: Float, progress: Float): Bitmap {
    val size = (34 * density).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val normalizedProgress = progress.coerceIn(0f, 1f)
    val left = lerp(5f, 6f, normalizedProgress) * density
    val top = lerp(5f, 1.5f, normalizedProgress) * density
    val right = size - left
    val bodyHeight = lerp(24f, 22f, normalizedProgress) * density
    val bottom = top + bodyHeight
    val radius = lerp(8f, 11f, normalizedProgress) * density
    val fill = interpolateColor(PARKING_DEFAULT_FILL, PARKING_SELECTED_FILL, normalizedProgress)
    val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fill
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, shapePaint)
    if (normalizedProgress > 0f) {
        val tailHalfWidth = lerp(0f, 6f, normalizedProgress) * density
        val tailTop = bottom - (lerp(0f, 2.5f, normalizedProgress) * density)
        val tailBottom = lerp(29f, 32.5f, normalizedProgress) * density
        canvas.drawPath(
            Path().apply {
                moveTo((size / 2f) - tailHalfWidth, tailTop)
                lineTo((size / 2f) + tailHalfWidth, tailTop)
                lineTo(size / 2f, tailBottom)
                close()
            },
            shapePaint,
        )
    }
    if (normalizedProgress < 1f) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PARKING_SELECTED_FILL
            style = Paint.Style.STROKE
            strokeWidth = density
            alpha = ((1f - normalizedProgress) * 255).toInt()
        }.also { canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, it) }
    }
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PARKING_TEXT_COLOR
        textSize = 12 * density
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }.also { textPaint ->
        val metrics = textPaint.fontMetrics
        val textCenterY = top + (bodyHeight * 0.5f)
        val baseline = textCenterY - ((metrics.ascent + metrics.descent) / 2f)
        canvas.drawText("P", size / 2f, baseline, textPaint)
    }
    return bitmap
}

private fun lerp(start: Float, end: Float, progress: Float): Float = start + ((end - start) * progress)

private fun interpolateColor(start: Int, end: Int, progress: Float): Int {
    val alpha = lerp((start ushr 24).toFloat(), (end ushr 24).toFloat(), progress).toInt()
    val red = lerp(((start shr 16) and 0xFF).toFloat(), ((end shr 16) and 0xFF).toFloat(), progress).toInt()
    val green = lerp(((start shr 8) and 0xFF).toFloat(), ((end shr 8) and 0xFF).toFloat(), progress).toInt()
    val blue = lerp((start and 0xFF).toFloat(), (end and 0xFF).toFloat(), progress).toInt()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

private val parkingMarkerAnimators = WeakHashMap<com.kakao.vectormap.label.Label, ValueAnimator>()
private val parkingMarkerProgress = WeakHashMap<com.kakao.vectormap.label.Label, Float>()

private const val PARKING_MARKER_MORPH_DURATION_MILLIS = 260L
private const val PARKING_DEFAULT_FILL = 0xFF9D97FF.toInt()
private const val PARKING_SELECTED_FILL = 0xFF5640FF.toInt()
private const val PARKING_TEXT_COLOR = 0xFFFFFFFF.toInt()
