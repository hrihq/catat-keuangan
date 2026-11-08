package com.keuangan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    var slices = emptyList<Slice>()
        private set

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = MaterialColors.getColor(this@DonutChartView, MaterialR.attr.colorOutline)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = MaterialColors.getColor(this@DonutChartView, MaterialR.attr.colorOnSurfaceVariant)
    }
    private val rect = RectF()

    private var progress = 1f
    private var progressAnimator: android.animation.ValueAnimator? = null

    init {
        scaleType = ScaleType.FIT_CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) * 0.78f
        val stroke = radius * 0.30f

        if (slices.isEmpty()) {
            placeholderPaint.strokeWidth = stroke
            canvas.drawCircle(cx, cy, radius, placeholderPaint)
            textPaint.textSize = radius * 0.30f
            canvas.drawText("Belum ada data", cx, cy + textPaint.textSize * 0.35f, textPaint)
            return
        }

        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        drawPaint.strokeWidth = stroke
        var angle = -90f
        for (slice in slices) {
            val sweep = slice.percentage * 3.6f * progress
            drawPaint.color = slice.color
            canvas.drawArc(rect, angle, sweep, false, drawPaint)
            angle += slice.percentage * 3.6f
        }
    }

    fun setSlices(slices: List<Slice>) {
        this.slices = slices
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

    fun setData(values: List<Float>) {
        val total = values.sum().coerceAtLeast(1f)
        val color = MaterialColors.getColor(this, MaterialR.attr.colorPrimary)
        setSlices(values.map { Slice((it / total) * 100f, color) })
    }

    data class Slice(val percentage: Float, val color: Int)
}
