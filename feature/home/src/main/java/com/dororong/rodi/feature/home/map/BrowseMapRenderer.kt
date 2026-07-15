package com.dororong.rodi.feature.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.feature.home.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

sealed interface BrowseLabelTag {
    data class Cluster(
        val point: GeoPoint,
        val targetZoom: Int,
    ) : BrowseLabelTag

    data class Course(val id: Int) : BrowseLabelTag
}

fun KakaoMap.renderClusters(
    context: Context,
    clusters: List<MapCluster>,
    coursesById: Map<Int, Course>,
    @ColorInt backgroundColor: Int,
    @ColorInt textColor: Int,
) {
    clearBrowseLabels()
    val manager = labelManager ?: return
    val layer = browseLabelLayer() ?: return
    val parkingStyles by lazy { manager.addLabelStyles(LabelStyles.from(LabelStyle.from(context.drawableToBitmap(R.drawable.ic_pin_parking, 34)))) }

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
            val course = coursesById[cluster.memberIds.single()] ?: return@forEach
            val styles = if (course.isParking) {
                parkingStyles
            } else {
                manager.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(createCourseChipBitmap(course.courseNickname, context.resources.displayMetrics.density))),
                )
            }
            layer.addLabel(
                LabelOptions.from(LatLng.from(course.startWaypoint.lat, course.startWaypoint.lng))
                    .setStyles(styles)
                    .setClickable(true)
                    .setTag(BrowseLabelTag.Course(course.id)),
            )
        }
    }
}

fun KakaoMap.renderIndividualMarkers(context: Context, courses: List<Course>) {
    val clusters = courses.map { course ->
        MapCluster(
            memberIds = listOf(course.id),
            representativePoint = GeoPoint(course.startWaypoint.lat, course.startWaypoint.lng),
            targetZoom = 14,
        )
    }
    renderClusters(
        context = context,
        clusters = clusters,
        coursesById = courses.associateBy(Course::id),
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
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        setShadowLayer(1.5f * density, 0f, 0f, 0x4D000000)
    }

    canvas.drawRoundRect(
        RectF(bubbleLeft, bubbleTop, bubbleRight, bubbleBottom),
        cornerRadius,
        cornerRadius,
        backgroundPaint,
    )
    canvas.drawPath(
        Path().apply {
            moveTo((width - tailWidth) / 2f, bubbleBottom - 1f)
            lineTo((width + tailWidth) / 2f, bubbleBottom - 1f)
            lineTo(width / 2f, bubbleBottom + tailHeight)
            close()
        },
        backgroundPaint,
    )
    val baseline = bubbleTop + bubbleHeight / 2f - (fontMetrics.descent + fontMetrics.ascent) / 2f
    canvas.drawText(count.toString(), width / 2f, baseline, textPaint)
    return bitmap
}

private fun Context.drawableToBitmap(drawableRes: Int, sizeDp: Int): Bitmap {
    val size = (sizeDp * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(size, size)
    val drawable = ContextCompat.getDrawable(this, drawableRes) ?: return bitmap
    drawable.setBounds(0, 0, size, size)
    drawable.draw(Canvas(bitmap))
    return bitmap
}
