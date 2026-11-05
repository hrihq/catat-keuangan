package com.keuangan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Shader
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

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val rectF = RectF()
    private val shadowRectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = days.size
        val availableWidth = width.toFloat()
        val slotWidth = availableWidth / n
        val barWidth = slotWidth * 0.5f
        val maxBarHeight = height * 0.65f
        val chartBottom = height * 0.70f

        for (i in 0 until n) {
            val item = days[i]
            val cx = slotWidth * i + slotWidth / 2f
            val barH = maxBarHeight * item.ratio
            val top = chartBottom - barH

            // Shadow
            shadowRectF.set(cx - barWidth / 2f, chartBottom - 1f, cx + barWidth / 2f, chartBottom + 3f)
            shadowPaint.color = 0x1A000000.toInt()
            canvas.drawRoundRect(shadowRectF, 4f, 4f, shadowPaint)

            // Bar
            rectF.set(cx - barWidth / 2f, top, cx + barWidth / 2f, chartBottom)
            if (item.isActive) {
                barPaint.color = 0xFF0052CC.toInt() // Active: strong blue
            } else {
                barPaint.color = 0xFFB0D4FF.toInt() // Inactive: light blue
            }
            val radius = 5f
            canvas.drawRoundRect(rectF, radius, radius, barPaint)

            // Label
            textPaint.color = if (item.isActive) 0xFF0052CC.toInt() else 0xFF999999.toInt()
            textPaint.isFakeBoldText = item.isActive
            canvas.drawText(item.label, cx, height.toFloat() - 4f, textPaint)
        }
    }

    data class BarData(val label: String, val ratio: Float, val isActive: Boolean)
}

