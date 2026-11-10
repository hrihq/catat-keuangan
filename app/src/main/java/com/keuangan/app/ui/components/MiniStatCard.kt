package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R
import com.keuangan.app.util.Motion
import java.text.NumberFormat

/** Atomic: kartu statistik mini (Pemasukan / Pengeluaran). */
class MiniStatCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val iconTile = IconTile(context)
    private val titleText = TextView(context)
    private val amountText = TextView(context)
    private val column = LinearLayout(context)
    private var countAnimator: android.animation.ValueAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.bg_transaction_row)
        val pad = dp(14)
        setPadding(pad, pad, pad, pad)
        isClickable = true
        isFocusable = true

        iconTile.layoutParams = LayoutParams(dp(40), dp(40))

        column.orientation = VERTICAL
        column.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(12)
        }
        titleText.textSize = 12f
        titleText.setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))

        amountText.textSize = 16f
        amountText.maxLines = 1
        amountText.setAutoSizeTextTypeUniformWithConfiguration(
            11, 16, 1, android.util.TypedValue.COMPLEX_UNIT_SP
        )
        amountText.setTypeface(null, android.graphics.Typeface.BOLD)
        amountText.setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))

        column.addView(titleText)
        column.addView(amountText)
        addView(iconTile)
        addView(column)
    }

    fun bind(title: String, amount: String, iconRes: Int, accentRes: Int, iconBackgroundRes: Int) {
        titleText.text = title
        amountText.text = amount
        amountText.setTextColor(ContextCompat.getColor(context, accentRes))
        iconTile.bind(iconRes, accentRes, iconBackgroundRes)
    }

    /** Sama seperti bind(), tapi nominal dianimasikan count-up. */
    fun bindAmount(
        title: String,
        value: Double,
        isIncome: Boolean,
        formatter: NumberFormat,
        iconRes: Int,
        accentRes: Int,
        iconBackgroundRes: Int
    ) {
        countAnimator?.cancel()
        titleText.text = title
        amountText.setTextColor(ContextCompat.getColor(context, accentRes))
        iconTile.bind(iconRes, accentRes, iconBackgroundRes)

        val sign = if (isIncome) "+" else "-"
        if (!Motion.shouldAnimate(context)) {
            amountText.text = "$sign${formatter.format(value)}"
            return
        }
        countAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 550
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                amountText.text = "$sign${formatter.format(value * (it.animatedValue as Float))}"
            }
            start()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
