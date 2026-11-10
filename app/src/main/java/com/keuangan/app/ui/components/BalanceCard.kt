package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R
import com.keuangan.app.util.Motion

/**
 * Atomic: balance card (blue) with total + Pemasukan/Pengeluaran pills.
 */
class BalanceCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val subtitleView: TextView
    private val amountView: TextView
    private val incomePill: PillLabel
    private val expensePill: PillLabel

    private var countAnimator: android.animation.ValueAnimator? = null

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.bg_balance_card)
        val pad = (24 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        subtitleView = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.white_text))
        }
        amountView = TextView(context).apply {
            text = "Rp 0"
            textSize = 34f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.white_text))
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        addView(subtitleView)
        addView(amountView)

        val pillRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, (18 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        incomePill = PillLabel(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (6 * resources.displayMetrics.density).toInt()
            }
        }
        expensePill = PillLabel(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (6 * resources.displayMetrics.density).toInt()
            }
        }
        pillRow.addView(incomePill)
        pillRow.addView(expensePill)
        addView(pillRow)
    }

    fun bind(
        subtitle: String,
        balance: Double,
        income: Double,
        expense: Double,
        formatter: java.text.NumberFormat
    ) {
        subtitleView.text = subtitle
        countAnimator?.cancel()
        if (!Motion.shouldAnimate(context)) {
            amountView.text = formatter.format(balance)
        } else {
            countAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 700
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener {
                    amountView.text = formatter.format(balance * (it.animatedValue as Float))
                }
                start()
            }
        }
        incomePill.bind("Pemasukan", "+${formatter.format(income)}", R.color.white_text, R.color.white_text)
        expensePill.bind("Pengeluaran", "-${formatter.format(expense)}", R.color.white_text, R.color.white_text)
    }

    /** Dipanggil saat tab Beranda ditampilkan. */
    fun playEntrance() {
        animate().cancel()
        alpha = 0f
        translationY = (16 * resources.displayMetrics.density)
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
}
