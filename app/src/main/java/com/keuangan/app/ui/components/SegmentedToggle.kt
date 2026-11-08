package com.keuangan.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: segmented control (Pengeluaran / Pemasukan) dengan indikator pill yang
 * benar-benar bergeser saat mode diganti. Digambar manual agar pasti tampil.
 */
class SegmentedToggle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var isIncome: Boolean = false
        private set

    var onModeChanged: ((Boolean) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(14f)
        typeface = Typeface.DEFAULT_BOLD
    }

    private val trackColor = ContextCompat.getColor(context, R.color.brand_surface_container)
    private val indicatorColor = ContextCompat.getColor(context, R.color.brand_light_blue)
    private val activeColor = ContextCompat.getColor(context, R.color.brand_blue)
    private val inactiveColor = ContextCompat.getColor(context, R.color.brand_text_secondary)

    private val padding = dp(4f)
    private var indicatorX = 0f
    private var animator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = (h - padding * 2f) / 2f
        val cell = (w - padding * 2f) / 2f
        val baseline = h / 2f + textPaint.textSize * 0.35f

        trackPaint.color = trackColor
        canvas.drawRoundRect(0f, 0f, w, h, radius, radius, trackPaint)

        indicatorPaint.color = indicatorColor
        canvas.drawRoundRect(
            RectF(padding + indicatorX, padding, padding + indicatorX + cell, h - padding),
            radius, radius, indicatorPaint
        )

        textPaint.color = if (isIncome) inactiveColor else activeColor
        canvas.drawText("Pengeluaran", padding + cell / 2f, baseline, textPaint)

        textPaint.color = if (isIncome) activeColor else inactiveColor
        canvas.drawText("Pemasukan", padding + cell + cell / 2f, baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val cell = (width - padding * 2f) / 2f
                setMode(event.x > padding + cell)
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setMode(income: Boolean, animate: Boolean = true) {
        if (isIncome == income) return
        isIncome = income
        val cell = (width - padding * 2f) / 2f
        val target = if (income) cell else 0f
        animator?.cancel()
        if (animate && width > 0) {
            animator = ValueAnimator.ofFloat(indicatorX, target).apply {
                duration = 320
                interpolator = android.view.animation.FastOutSlowInInterpolator()
                addUpdateListener {
                    indicatorX = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            indicatorX = target
            invalidate()
        }
        onModeChanged?.invoke(income)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
