package com.keuangan.app.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/** Atomic: kartu statistik mini (Pemasukan / Pengeluaran). */
class MiniStatCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val iconTile = IconTile(context)
    private val titleText = TextView(context)
    private val amountText = TextView(context)
    private val column = LinearLayout(context)

    init {
        background = ContextCompat.getDrawable(context, R.drawable.bg_transaction_row)
        val pad = dp(14)
        setPadding(pad, pad, pad, pad)
        isClickable = true
        isFocusable = true

        iconTile.layoutParams = LayoutParams(dp(40), dp(40))

        column.orientation = LinearLayout.VERTICAL
        column.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_VERTICAL
        ).apply { marginStart = dp(12) }

        titleText.textSize = 12f
        titleText.setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))

        amountText.textSize = 16f
        amountText.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
