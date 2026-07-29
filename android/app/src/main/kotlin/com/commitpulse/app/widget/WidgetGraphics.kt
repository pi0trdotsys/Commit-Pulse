package com.commitpulse.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import com.commitpulse.app.data.DayCommit
import kotlin.math.max
import androidx.compose.ui.graphics.Color as ComposeColor

/** Rysowanie sparklinów i pierścienia celu do bitmapy — Glance nie ma Canvas/Path jak Compose. */
object WidgetGraphics {

    fun sparkline(days: List<DayCommit>, accent: ComposeColor, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (days.isEmpty()) return bmp

        val accentArgb = accent.toArgb()
        val maxVal = max(days.maxOf { it.count }, 1)
        val step = widthPx.toFloat() / max(days.size - 1, 1)
        val points = days.mapIndexed { i, d ->
            val x = i * step
            val y = heightPx - (d.count.toFloat() / maxVal) * (heightPx - 3f) - 1.5f
            x to y
        }

        val linePath = Path().apply {
            points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
        }
        val areaPath = Path(linePath).apply {
            lineTo(widthPx.toFloat(), heightPx.toFloat())
            lineTo(0f, heightPx.toFloat())
            close()
        }

        val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            alpha = (0.16f * 255).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawPath(areaPath, areaPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * (widthPx / 156f).coerceIn(0.8f, 2f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(linePath, linePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            style = Paint.Style.FILL
        }
        val last = points.last()
        canvas.drawCircle(last.first, last.second, 2.4f, dotPaint)

        return bmp
    }

    fun goalRing(value: Int, goal: Int, accent: ComposeColor, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val strokeWidth = sizePx * 0.09f
        val radius = sizePx / 2f - strokeWidth
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val pct = (value.toFloat() / max(goal, 1)).coerceIn(0f, 1f)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#52586A")
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawCircle(cx, cy, radius, trackPaint)

        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent.toArgb()
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        val rect = android.graphics.RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, -90f, 360f * pct, false, progressPaint)

        return bmp
    }
}
