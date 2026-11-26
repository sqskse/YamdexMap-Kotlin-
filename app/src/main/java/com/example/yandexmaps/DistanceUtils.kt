package com.example.yandexmaps

import com.yandex.mapkit.geometry.Point
import java.util.Locale
import kotlin.math.roundToInt

object DistanceUtils {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceBetween(a: Point, b: Point): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)

        val sinLat = Math.sin(dLat / 2.0)
        val sinLon = Math.sin(dLon / 2.0)
        val h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon
        return 2.0 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun formatDistance(meters: Double): String = if (meters < 1000) {
        String.format(Locale.getDefault(), MapConfig.DISTANCE_METERS_FORMAT, meters.roundToInt())
    } else {
        String.format(Locale.getDefault(), MapConfig.DISTANCE_KM_FORMAT, meters / 1000.0)
    }
}