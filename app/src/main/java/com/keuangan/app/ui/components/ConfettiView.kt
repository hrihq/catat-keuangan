package com.keuangan.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.keuangan.app.R
import com.keuangan.app.util.Motion
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Atomic: konfeti ringan untuk perayaan target tercapai.
 * Partikel sederhana (rect berputar) selama ~700ms, tanpa dependency baru.
 */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Particle(
        val angle: Float,
        val speed: Float,
        val size: Float,
        val color: Int,
        val spin: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val palette by lazy {
        intArrayOf(
            ContextCompat.getColor(context, R.color.brand_blue),
            ContextCompat.getColor(context, R.color.brand_yellow),
            ContextCompat.getColor(context, R.color.income_green),
            ContextCompat.getColor(context, R.color.expense_red),
            ContextCompat.getColor(context, R.color.brand_teal)
        )
    }

    /** Luncurkan konfeti dari tengah atas view. Hormati reduced motion. */
    fun burst() {
        if (!Motion.shouldAnimate(context)) return
        particles.clear()
        val count = 26
        for (i in 0 until count) {
            val angle = (-Math.PI / 2 + (Random.nextFloat() - 0.5f) * 2.2f).toFloat()
            val paletteIndex = Random.nextInt(palette.size)
            particles.add(
                Particle(
                    angle = angle,
                    speed = 0.55f + Random.nextFloat() * 0.7f,
                    size = dp(5f) + Random.nextFloat() * dp(4f),
                    color = palette[paletteIndex],
                    spin = (Random.nextFloat() - 0.5f) * 720f
                )
            )
        }
        animator?.cancel()
        progress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (particles.isEmpty() || progress <= 0f || progress >= 1f) return
        val originX = width / 2f
        val ground = height * 0.86f
        for (particle in particles) {
            val traveled = particle.speed * progress
            val x = originX + cos(particle.angle) * traveled * width * 0.55f
            val y = height * 0.05f + sin(particle.angle) * traveled * height * 0.35f +
                ground * progress * progress * 0.75f
            val alpha = (255 * (1f - progress)).toInt().coerceIn(0, 255)
            paint.color = particle.color
            paint.alpha = alpha
            canvas.save()
            canvas.rotate(particle.spin * progress, x, y)
            rect.set(x - particle.size / 2f, y - particle.size / 2f, x + particle.size / 2f, y + particle.size / 2f)
            canvas.drawRoundRect(rect, dp(1f), dp(1f), paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
