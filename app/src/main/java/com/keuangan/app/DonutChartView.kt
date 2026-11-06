package com.keuangan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    var slices = emptyList<Slice>()
        private set

    private var currentAngle = 0f

    private var width = 0f

    private val drawPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val rect = RectF()

    init {
        scaleType = ScaleType.FIT_CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = measuredWidth * 0.9f
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (slices.isEmpty()) {
            val placeholder = Paint().apply {
                color = Color.parseColor("#0052CC")
                style = Paint.Style.STROKE
                strokeWidth = 16f
            }
            canvas.drawCircle(0f, 0f, width * 0.45f, placeholder)
            return
        }

        val size = width * 0.7f
        val stroke = size * 0.22f
        rect.set(-size, -size, size, size)

        for (slice in slices) {
            val sweep = slice.percentage * 360f
            drawPaint.color = slice.color
            canvas.drawArc(rect, currentAngle, sweep, false, drawPaint, stroke)
            currentAngle += sweep
        }
    }

    fun setSlices(slices: List<Slice>) {
        this.slices = slices
        invalidate()
    }

    fun setData(values: List<Float>) {
        val total = values.sum().coerceAtLeast(1f)
        setSlices(values.map { it / total })
    }

    data class Slice(val percentage: Float, val color: Int)
}
