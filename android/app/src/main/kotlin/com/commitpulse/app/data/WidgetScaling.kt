package com.commitpulse.app.data

/**
 * Współczynnik skalowania czcionek/elementów widgetu względem szerokości referencyjnej
 * (domyślnie szerokość widgetu 2×1). Wydzielony jako czysta funkcja liczbowa, żeby dało się
 * go przetestować bez kompozycji Glance/Compose (patrz [com.commitpulse.app.widget]).
 */
fun widgetScaleFactor(
    widthDp: Float,
    referenceWidthDp: Float = 110f,
    min: Float = 0.6f,
    max: Float = 1.5f,
): Float = (widthDp / referenceWidthDp).coerceIn(min, max)
