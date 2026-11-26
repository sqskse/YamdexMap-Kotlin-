package com.example.yandexmaps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.LineStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.runtime.image.ImageProvider

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

            // Рисуем линию
            val line = mapObjects.addPolyline(Polyline(listOf(start, end))).apply {
                setStyle(LineStyle().setStrokeWidth(dpToPx(MapConfig.LINE_WIDTH_DP)))
                setStrokeColors(listOf(MapConfig.LINE_COLOR))
            }
            polylines.add(line)

            // Вычисляем центр
            val midLat = (start.latitude + end.latitude) / 2.0
            val midLon = (start.longitude + end.longitude) / 2.0
            val midPoint = Point(midLat, midLon)

            // Текст и расстояние
            val distanceMeters = DistanceUtils.distanceBetween(start, end)
            val text = DistanceUtils.formatDistance(distanceMeters)

            // Добавляем текст
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
        val textSize = spToPx(MapConfig.TEXT_SIZE_SP)
        val padding = dpToPx(MapConfig.TEXT_PADDING_DP)
        val cornerRadius = dpToPx(MapConfig.TEXT_CORNER_RADIUS_DP)
        val borderWidth = dpToPx(MapConfig.TEXT_BORDER_WIDTH_DP)

        val paint = Paint().apply {
            color = MapConfig.TEXT_COLOR
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

        // Фон
        val bgPaint = Paint().apply {
            color = MapConfig.TEXT_BG_COLOR
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Рамка
        val borderPaint = Paint().apply {
            color = MapConfig.TEXT_BORDER_COLOR
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // Текст
        val textY = (height / 2f) - ((bounds.top + bounds.bottom) / 2f)
        canvas.drawText(text, width / 2f, textY, paint)

        return bitmap
    }
}