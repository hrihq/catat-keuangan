package com.keuangan.app.ui.components

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.keuangan.app.R
import com.keuangan.app.util.Motion

/**
 * Atomic: loading skeleton (3 bar shimmer 1.2s) menggantikan spinner.
 */
class LoadingState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val bars = mutableListOf<View>()
    private val animators = mutableListOf<ObjectAnimator>()
    private var ready = false

    init {
        orientation = VERTICAL
        val pad = (24 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        val widths = intArrayOf(220, 180, 140)
        widths.forEachIndexed { index, w ->
            val bar = View(context).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(ContextCompat.getColor(context, R.color.brand_light_blue))
                }
                layoutParams = LayoutParams(dp(w), dp(14)).apply {
                    if (index > 0) topMargin = dp(12)
                }
                alpha = 0.35f
            }
            bars.add(bar)
            addView(bar)
        }
        ready = true
        if (visibility == VISIBLE) startShimmer()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (!ready) return
        if (visibility == VISIBLE) startShimmer() else stopShimmer()
    }

    override fun onDetachedFromWindow() {
        stopShimmer()
        super.onDetachedFromWindow()
    }

    private fun startShimmer() {
        stopShimmer()
        if (!Motion.shouldAnimate(context)) {
            bars.forEach { it.alpha = 0.6f }
            return
        }
        bars.forEachIndexed { index, bar ->
            animators.add(
                ObjectAnimator.ofFloat(bar, "alpha", 0.35f, 0.9f).apply {
                    duration = 1200
                    startDelay = index * 120L
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                    start()
                }
            )
        }
    }

    private fun stopShimmer() {
        animators.forEach { it.cancel() }
        animators.clear()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
