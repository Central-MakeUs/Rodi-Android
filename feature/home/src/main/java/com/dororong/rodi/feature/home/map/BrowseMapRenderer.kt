package com.dororong.rodi.feature.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.feature.home.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

sealed interface BrowseLabelTag {
    data class Cluster(
        val center: GeoPoint,
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
    val courseBitmap by lazy { context.drawableToBitmap(R.drawable.ic_pin_start, 34) }
    val parkingBitmap by lazy { context.drawableToBitmap(R.drawable.ic_pin_park, 34) }
    val courseStyles by lazy { manager.addLabelStyles(LabelStyles.from(LabelStyle.from(courseBitmap))) }
    val parkingStyles by lazy { manager.addLabelStyles(LabelStyles.from(LabelStyle.from(parkingBitmap))) }
    clusters.forEach { cluster ->
        if (cluster.isClusterMarker) {
            val bitmap = createClusterBitmap(
                count = cluster.count,
                density = context.resources.displayMetrics.density,
                backgroundColor = backgroundColor,
                textColor = textColor,
            )
            val styles = manager.addLabelStyles(LabelStyles.from(LabelStyle.from(bitmap)))
            layer.addLabel(
                LabelOptions.from(
                    LatLng.from(cluster.representativePoint.lat, cluster.representativePoint.lng),
                )
                    .setStyles(styles)
                    .setClickable(true)
                    .setTag(BrowseLabelTag.Cluster(cluster.representativePoint, cluster.targetZoom)),
            )
        } else {
            val course = coursesById[cluster.memberIds.single()] ?: return@forEach
            val point = course.startWaypoint
            layer.addLabel(
                LabelOptions.from(LatLng.from(point.lat, point.lng))
                    .setStyles(if (course.isParking) parkingStyles else courseStyles)
                    .setClickable(true)
                    .setTag(BrowseLabelTag.Course(course.id)),
            )
        }
    }
}

fun KakaoMap.renderIndividualMarkers(context: Context, courses: List<Course>) {
    clearBrowseLabels()
    val manager = labelManager ?: return
    val layer = browseLabelLayer() ?: return
    val courseBitmap = context.drawableToBitmap(R.drawable.ic_pin_start, 34)
    val parkingBitmap = context.drawableToBitmap(R.drawable.ic_pin_park, 34)
    val courseStyles = manager.addLabelStyles(LabelStyles.from(LabelStyle.from(courseBitmap)))
    val parkingStyles = manager.addLabelStyles(LabelStyles.from(LabelStyle.from(parkingBitmap)))
    courses.forEach { course ->
        val point = course.startWaypoint
        layer.addLabel(
            LabelOptions.from(LatLng.from(point.lat, point.lng))
                .setStyles(if (course.isParking) parkingStyles else courseStyles)
                .setClickable(true)
                .setTag(BrowseLabelTag.Course(course.id)),
        )
    }
}

private fun createClusterBitmap(
    count: Int,
    density: Float,
    @ColorInt backgroundColor: Int,
    @ColorInt textColor: Int,
): Bitmap {
    val size = (48 * density).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    canvas.drawCircle(
        center,
        center,
        center,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor },
    )
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 15 * density
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val baseline = center - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(count.toString(), center, baseline, textPaint)
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
