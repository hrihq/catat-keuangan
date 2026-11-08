package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: small pill label (Pemasukan / Pengeluaran inside the balance card).
 */
class PillLabel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val valueView: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.bg_pill)
        val padH = (14 * resources.displayMetrics.density).toInt()
        val padV = (10 * resources.displayMetrics.density).toInt()
        setPadding(padH, padV, padH, padV)

        titleView = TextView(context).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
        }
        valueView = TextView(context).apply {
            textSize = 15f
            maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(
                11, 15, 1, android.util.TypedValue.COMPLEX_UNIT_SP
            )
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        addView(titleView)
        addView(valueView)
    }

    fun bind(
        title: String,
        value: String,
        valueColorRes: Int,
        titleColorRes: Int = R.color.brand_text_secondary
    ) {
        titleView.text = title
        valueView.text = value
        valueView.setTextColor(ContextCompat.getColor(context, valueColorRes))
        titleView.setTextColor(ContextCompat.getColor(context, titleColorRes))
    }
}
