package com.example.yandexmaps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.yandexmaps.ui.theme.*
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.LineStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.runtime.image.ImageProvider
import java.util.Locale
import kotlin.math.roundToInt

class MapRenderer(private val context: Context) {

    private val polylines = mutableListOf<PolylineMapObject>()
    private val distanceLabels = mutableListOf<PlacemarkMapObject>()

    private fun dpToPx(dp: Float): Float = dp * context.resources.displayMetrics.density
    private fun spToPx(sp: Float): Float = sp * context.resources.displayMetrics.scaledDensity

    fun clear(mapObjects: MapObjectCollection) {
        polylines.forEach { mapObjects.remove(it) }
        distanceLabels.forEach { mapObjects.remove(it) }
        polylines.clear()
        distanceLabels.clear()
    }

    fun render(mapObjects: MapObjectCollection, points: List<Point>) {
        clear(mapObjects)
        if (points.size < 2) return

        val segmentsCount = if (points.size == 2) 1 else points.size

        for (i in 0 until segmentsCount) {
            val start = points[i]
            val end = if (i == points.size - 1) points[0] else points[i + 1]

            val line = mapObjects.addPolyline(Polyline(listOf(start, end))).apply {
                setStyle(LineStyle().setStrokeWidth(dpToPx(MapTheme.LineWidth)))
                setStrokeColors(listOf(MapLineColor))
            }
            polylines.add(line)

            val midLat = (start.latitude + end.latitude) / 2.0
            val midLon = (start.longitude + end.longitude) / 2.0
            val midPoint = Point(midLat, midLon)

            val distanceMeters = distanceBetween(start, end)
            val text = formatDistance(distanceMeters)

            val textLabel = mapObjects.addPlacemark().apply {
                geometry = midPoint
                setIcon(
                    ImageProvider.fromBitmap(createTextBitmap(text)),
                    IconStyle().setAnchor(PointF(0.5f, 0.5f))
                )
            }
            distanceLabels.add(textLabel)
        }
    }

    private fun createTextBitmap(text: String): Bitmap {
        val textSize = spToPx(MapTypography.TextSize)
        val padding = dpToPx(MapTheme.TextPadding)
        val cornerRadius = dpToPx(MapTheme.TextCornerRadius)
        val borderWidth = dpToPx(MapTheme.TextBorderWidth)

        val paint = Paint().apply {
            color = MapTextColor
            this.textSize = textSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val width = bounds.width() + (padding * 2).toInt()
        val height = bounds.height() + padding.toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val bgPaint = Paint().apply {
            color = MapTextBgColor
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        val borderPaint = Paint().apply {
            color = MapTextBorderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val textY = (height / 2f) - ((bounds.top + bounds.bottom) / 2f)
        canvas.drawText(text, width / 2f, textY, paint)

        return bitmap
    }

    // Математика перенесена сюда
    private fun distanceBetween(a: Point, b: Point): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = Math.sin(dLat / 2.0)
        val sinLon = Math.sin(dLon / 2.0)
        val h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon
        return 2.0 * 6371000.0 * Math.asin(Math.sqrt(h.coerceIn(0.0, 1.0)))
    }

    private fun formatDistance(meters: Double): String = if (meters < 1000) {
        String.format(Locale.getDefault(), MapStrings.DistanceMetersFormat, meters.roundToInt())
    } else {
        String.format(Locale.getDefault(), MapStrings.DistanceKmFormat, meters / 1000.0)
    }
}