package com.example.yandexmaps.ui.theme

// ——— Размеры (в dp) ———
object MapTheme {
    const val LineWidth = 2f
    const val TextPadding = 8f
    const val TextCornerRadius = 6f
    const val TextBorderWidth = 1f
    const val MarkerScale = 0.9f
}

// ——— Текстовые константы ———
object MapStrings {
    const val PerimeterFormat = "Периметр: %s"
    const val HintNewTriangle = "Старая точка удалена, построен новый треугольник"
    const val HintNeedThirdPoint = "Поставьте 3-ю точку для построения треугольника"
    const val HintEmpty = "Поставьте две метки на карте"
    const val DistancePlaceholder = "—"
    const val MapCleared = "Карта очищена"
    const val EnterQuery = "Введите адрес"
    const val SearchNotFound = "Ничего не найдено"
    const val SearchError = "Ошибка поиска"
    const val SearchFoundFormat = "Найдено: %s"
    const val DistanceMetersFormat = "%d м"
    const val DistanceKmFormat = "%.2f км"

    fun getMarkersCountString(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "$count метка"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "$count метки"
            else -> "$count меток"
        }
    }
}