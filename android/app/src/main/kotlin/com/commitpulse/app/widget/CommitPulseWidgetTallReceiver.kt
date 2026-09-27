package com.commitpulse.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Drugi odbiornik dla wariantu 1×2 (wąski, wysoki) — ten sam responsywny [CommitPulseWidget],
 * ale osobny wpis w selektorze widgetów Androida z domyślnym pionowym kształtem i podglądem,
 * żeby dało się go dodać obok (lub zamiast) wariantu 2×1 bez usuwania istniejącego widgetu.
 */
class CommitPulseWidgetTallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CommitPulseWidget()
}
