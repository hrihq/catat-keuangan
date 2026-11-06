package com.keuangan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    var data = emptyList<Float>()
        private set

    private val bgPaint = Paint().apply { color = Color.parseColor("#F0F0F0") }
    private val gridPaint = Paint().apply { color = Color.parseColor("#E0E0E0"); strokeWidth = 1f }
    private val barPaint = Paint().apply { color = Color.parseColor("#0052CC"); style = Paint.Style.FILL }
    private val valuePaint = Paint().apply { color = Color.parseColor("#333333"); textSize = 12f }
    private val labelPaint = Paint().apply { color = Color.parseColor("#666666"); textSize = 10f }

    private val labels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    private val barSpacing = 12f
    private val barWidth = 22f
    private val chartTop = 44f
    private val labelHeight = 28f
    private val maxValue = 1f

    init {
        scaleType = ScaleType.FIT_CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val chartWidth = w - 2 * barSpacing - labels.size * (barWidth + 8f)
        val chartHeight = h - chartTop - labelHeight
        if (chartWidth <= 0 || chartHeight <= 0) return

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val gridStep = chartHeight / 4
        for (i in 1..3) {
            val y = chartTop + i * gridStep
            canvas.drawLine(barSpacing, y, barSpacing + chartWidth, y, gridPaint)
        }

        val step = chartWidth / data.size
        for (i in data.indices) {
            val value = data[i].coerceIn(0f, maxValue)
            val barH = value / maxValue * chartHeight
            val x = barSpacing + i * (barWidth + step)
            val yTop = chartTop + chartHeight - barH
            canvas.drawRect(x, yTop, x + barWidth, chartTop + chartHeight, barPaint)

            val text = String.format("%.2f", value)
            val tw = valuePaint.measureText(text)
            canvas.drawText(text, x + (barWidth - tw) / 2, chartTop + chartHeight + 10f, valuePaint)
        }

        for (i in labels.indices) {
            val x = barSpacing + i * (barWidth + step) + barWidth / 2 - labelPaint.measureText(labels[i]) / 2
            canvas.drawText(labels[i], x, h - labelHeight / 2, labelPaint)
        }
    }

    fun setData(values: List<Float>) {
        data = values
        invalidate()
    }
}
