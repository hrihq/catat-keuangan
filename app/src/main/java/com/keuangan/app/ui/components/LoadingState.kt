package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: loading placeholder (simple centred spinner + label).
 */
class LoadingState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val pad = (24 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        val spinner = com.google.android.material.progressindicator.CircularProgressIndicator(context).apply {
            isIndeterminate = true
            layoutParams = LayoutParams(
                (32 * resources.displayMetrics.density).toInt(),
                (32 * resources.displayMetrics.density).toInt()
            )
        }
        val label = TextView(context).apply {
            text = "Memuat data…"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
            setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        addView(spinner)
        addView(label)
    }
}
