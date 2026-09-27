package com.commitpulse.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.level
import java.time.LocalDate
import kotlin.math.max
import androidx.compose.ui.graphics.Color as ComposeColor

/** Rysowanie sparklinów i pierścienia celu do bitmapy — Glance nie ma Canvas/Path jak Compose. */
object WidgetGraphics {

    fun sparkline(days: List<DayCommit>, accent: ComposeColor, widthPx: Int, heightPx: Int, density: Float): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (days.isEmpty()) return bmp

        val accentArgb = accent.toArgb()
        val strokePx = 1.8f * density
        val dotRadiusPx = 2.6f * density
        val inset = dotRadiusPx + strokePx / 2
        val maxVal = max(days.maxOf { it.count }, 1)
        val step = (widthPx - 2 * inset) / max(days.size - 1, 1)
        val points = days.mapIndexed { i, d ->
            val x = inset + i * step
            val y = heightPx - inset - (d.count.toFloat() / maxVal) * (heightPx - 2 * inset)
            x to y
        }

        val linePath = Path().apply {
            points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
        }
        val areaPath = Path(linePath).apply {
            lineTo(points.last().first, heightPx.toFloat())
            lineTo(points.first().first, heightPx.toFloat())
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
            strokeWidth = strokePx
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(linePath, linePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            style = Paint.Style.FILL
        }
        val last = points.last()
        canvas.drawCircle(last.first, last.second, dotRadiusPx, dotPaint)

        return bmp
    }

    /**
     * Siatka kontrybucji jako jedna bitmapa — Glance obcina Row/Column do 10 dzieci, więc
     * siatka złożona z komórek-Boxów gubiła wiersze i kolumny. Bitmapa nie ma tego limitu.
     */
    fun contributionGrid(
        cells: List<List<LocalDate?>>,
        counts: Map<LocalDate, Int>,
        maxCount: Int,
        heat: List<ComposeColor>,
        cellPx: Int,
        gapPx: Int,
    ): Bitmap {
        val rows = cells.size
        val cols = cells.firstOrNull()?.size ?: 0
        val width = (cols * cellPx + (cols - 1) * gapPx).coerceAtLeast(1)
        val height = (rows * cellPx + (rows - 1) * gapPx).coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val radius = cellPx * 0.25f
        val heatArgb = heat.map { it.toArgb() }

        cells.forEachIndexed { r, row ->
            row.forEachIndexed { c, date ->
                if (date != null) {
                    paint.color = heatArgb[level(counts[date] ?: 0, maxCount)]
                    val left = (c * (cellPx + gapPx)).toFloat()
                    val top = (r * (cellPx + gapPx)).toFloat()
                    canvas.drawRoundRect(left, top, left + cellPx, top + cellPx, radius, radius, paint)
                }
            }
        }
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
