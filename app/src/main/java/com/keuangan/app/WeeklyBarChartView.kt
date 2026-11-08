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

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6E2D8")
        strokeWidth = 1f
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0052CC")
        style = Paint.Style.FILL
    }
    private val barInactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C2DAFF")
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textAlign = Paint.Align.CENTER
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A93A3")
        textAlign = Paint.Align.CENTER
    }

    private val labels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    private val barWidth = 18f
    private val chartTop = 12f
    private val labelHeight = 22f
    private val maxValue = 1f

    private var progress = 1f
    private var progressAnimator: android.animation.ValueAnimator? = null

    init {
        scaleType = ScaleType.FIT_CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        if (data.isEmpty()) {
            emptyPaint.textSize = h * 0.14f
            canvas.drawText("Belum ada data", w / 2f, h / 2f, emptyPaint)
            return
        }

        val chartHeight = h - chartTop - labelHeight
        if (chartHeight <= 0f) return

        val gridStep = chartHeight / 4f
        for (i in 1..3) {
            val y = chartTop + i * gridStep
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        val step = w / data.size
        val maxIndex = data.indices.maxByOrNull { data[it] } ?: -1
        for (i in data.indices) {
            val value = data[i].coerceIn(0f, maxValue)
            val barH = value * chartHeight * progress
            val x = i * step + (step - barWidth) / 2f
            val yTop = chartTop + chartHeight - barH
            val paint = if (i == maxIndex) barPaint else barInactivePaint
            canvas.drawRoundRect(x, yTop, x + barWidth, chartTop + chartHeight, 4f, 4f, paint)
        }

        labelPaint.textSize = h * 0.10f
        for (i in labels.indices) {
            if (i >= data.size) break
            val x = i * step + step / 2f
            canvas.drawText(labels[i], x, h - 4f, labelPaint)
        }
    }

    fun setData(values: List<Float>) {
        data = values
        progressAnimator?.cancel()
        progressAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
