package com.keuangan.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: segmented control (Pengeluaran / Pemasukan) with a sliding indicator.
 */
class SegmentedToggle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var isIncome: Boolean = false
        private set

    var onModeChanged: ((Boolean) -> Unit)? = null

    private val indicator: View
    private val expenseLabel: TextView
    private val incomeLabel: TextView
    private var slideAnimator: ValueAnimator? = null

    init {
        setBackgroundResource(R.drawable.bg_segment_track)
        val pad = dp(4f)
        setPadding(pad, pad, pad, pad)
        isClickable = true

        indicator = View(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_segment_indicator)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT)
        }
        addView(indicator)

        val labels = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        expenseLabel = TextView(context).apply {
            text = "Pengeluaran"
            gravity = Gravity.CENTER
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { setMode(false) }
        }
        incomeLabel = TextView(context).apply {
            text = "Pemasukan"
            gravity = Gravity.CENTER
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { setMode(true) }
        }
        labels.addView(expenseLabel)
        labels.addView(incomeLabel)
        addView(labels)

        isIncome = false
        updateColors()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cell = (w - paddingLeft - paddingRight) / 2f
        (indicator.layoutParams as LayoutParams).width = cell.toInt()
        indicator.layoutParams.height = h - paddingTop - paddingBottom
        indicator.requestLayout()
        updateIndicator(false)
    }

    fun setMode(income: Boolean, animate: Boolean = true) {
        if (isIncome == income) return
        isIncome = income
        updateIndicator(animate)
        updateColors()
        onModeChanged?.invoke(income)
    }

    private fun updateIndicator(animate: Boolean) {
        val cell = (width - paddingLeft - paddingRight) / 2f
        val target = if (isIncome) cell else 0f
        slideAnimator?.cancel()
        if (animate && width > 0) {
            slideAnimator = ValueAnimator.ofFloat(indicator.translationX, target).apply {
                duration = 200
                addUpdateListener {
                    indicator.translationX = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            indicator.translationX = target
        }
    }

    private fun updateColors() {
        val active = ContextCompat.getColor(context, R.color.brand_blue)
        val inactive = ContextCompat.getColor(context, R.color.brand_text_secondary)
        expenseLabel.setTextColor(if (isIncome) inactive else active)
        incomeLabel.setTextColor(if (isIncome) active else inactive)
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}
