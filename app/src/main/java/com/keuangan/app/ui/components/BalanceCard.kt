package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

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
        amountView.text = formatter.format(balance)
        incomePill.bind("Pemasukan", "+ ${formatter.format(income)}", R.color.white_text)
        expensePill.bind("Pengeluaran", "- ${formatter.format(expense)}", R.color.white_text)
    }
}
