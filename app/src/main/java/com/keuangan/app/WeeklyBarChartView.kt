package com.keuangan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val days = listOf(
        BarData("Sen", 0.45f, false),
        BarData("Sel", 0.60f, false),
        BarData("Rab", 0.40f, false),
        BarData("Kam", 0.95f, true),
        BarData("Jum", 0.50f, false),
        BarData("Sab", 0.70f, false),
        BarData("Min", 0.35f, false)
    )

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val rectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = days.size
        val availableWidth = width.toFloat()
        val slotWidth = availableWidth / n
        val barWidth = slotWidth * 0.45f
        val maxBarHeight = height * 0.75f
        val chartBottom = height * 0.78f

        for (i in 0 until n) {
            val item = days[i]
            val cx = slotWidth * i + slotWidth / 2f
            val barH = maxBarHeight * item.ratio
            val top = chartBottom - barH

            rectF.set(cx - barWidth / 2f, top, cx + barWidth / 2f, chartBottom)
            barPaint.color = if (item.isActive) 0xFF0B57D0.toInt() else 0xFFC2DAFF.toInt()
            val radius = barWidth / 2f
            canvas.drawRoundRect(rectF, radius, radius, barPaint)

            textPaint.color = if (item.isActive) 0xFF0B57D0.toInt() else 0xFF6B7280.toInt()
            textPaint.isFakeBoldText = item.isActive
            canvas.drawText(item.label, cx, height.toFloat() - 10f, textPaint)
        }
    }

    data class BarData(val label: String, val ratio: Float, val isActive: Boolean)
}
