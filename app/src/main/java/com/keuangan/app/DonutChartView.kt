package com.keuangan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val slices = listOf(
        Slice(0.40f, 0xFF0052CC.toInt()), // Makanan 40% - primary blue
        Slice(0.25f, 0xFFD97706.toInt()), // Transport 25% - orange
        Slice(0.20f, 0xFF0D9488.toInt()), // Tagihan 20% - teal
        Slice(0.15f, 0xFF8B5CF6.toInt())  // Lainnya 15% - purple
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x11000000.toInt()
    }

    private val rectF = RectF()
    private val shadowRectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = width * 0.18f
        paint.strokeWidth = stroke
        shadowPaint.strokeWidth = stroke + 2

        val pad = stroke / 2f + 4f
        rectF.set(pad, pad, width - pad, height - pad)
        shadowRectF.set(pad - 2, pad - 2, width - pad + 2, height - pad + 2)

        // Shadow ring
        var startAngle = -90f
        for (slice in slices) {
            val sweep = slice.percentage * 360f
            canvas.drawArc(shadowRectF, startAngle, sweep, false, shadowPaint)
            startAngle += sweep
        }

        // Main chart
        startAngle = -90f
        for (slice in slices) {
            val sweep = slice.percentage * 360f
            paint.color = slice.color
            canvas.drawArc(rectF, startAngle, sweep, false, paint)
            startAngle += sweep
        }
    }

    data class Slice(val percentage: Float, val color: Int)
}

