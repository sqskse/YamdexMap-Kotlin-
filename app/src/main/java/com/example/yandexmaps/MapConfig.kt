package com.example.yandexmaps

import android.graphics.Color

object MapConfig {
    // --- Цвета ---
    const val LINE_COLOR = 0xFFFF4433.toInt() // Красный
    const val TEXT_COLOR = Color.BLACK
    const val TEXT_BG_COLOR = Color.WHITE
    const val TEXT_BORDER_COLOR = Color.LTGRAY

    // --- Размеры (в dp и sp) ---
    const val LINE_WIDTH_DP = 2f
    const val TEXT_SIZE_SP = 12f
    const val TEXT_PADDING_DP = 8f
    const val TEXT_CORNER_RADIUS_DP = 6f
    const val TEXT_BORDER_WIDTH_DP = 1f
    const val MARKER_SCALE = 0.9f

    // --- Строки ---
    const val PERIMETER_FORMAT = "Периметр: %s"
    const val HINT_NEW_TRIANGLE = "Старая точка удалена, построен новый треугольник"
    const val HINT_NEED_THIRD_POINT = "Поставьте 3-ю точку для построения треугольника"
    const val HINT_EMPTY = "Поставьте две метки на карте"
    const val DISTANCE_PLACEHOLDER = "—"
    const val MAP_CLEARED = "Карта очищена"
    const val ENTER_QUERY = "Введите адрес"
    const val SEARCH_NOT_FOUND = "Ничего не найдено"
    const val SEARCH_ERROR = "Ошибка поиска"
    const val SEARCH_FOUND = "Найдено: %s"
    const val DISTANCE_METERS_FORMAT = "%d м"
    const val DISTANCE_KM_FORMAT = "%.2f км"

    // --- Склонение слова "метка" ---
    fun getMarkersCountString(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "$count метка"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "$count метки"
            else -> "$count меток"
        }
    }
}