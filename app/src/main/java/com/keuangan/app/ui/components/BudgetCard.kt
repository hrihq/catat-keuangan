package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.keuangan.app.R

/**
 * Atomic: budget card (yellow) with progress bar.
 */
class BudgetCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val subtitleView: TextView
    private val progress: LinearProgressIndicator

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.bg_budget_card)
        val padH = (20 * resources.displayMetrics.density).toInt()
        val padV = (18 * resources.displayMetrics.density).toInt()
        setPadding(padH, padV, padH, padV)

        titleView = TextView(context).apply {
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.budget_text))
        }
        subtitleView = TextView(context).apply {
            text = "0% terpakai · sisa Rp 0"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.budget_text))
            setPadding(0, (4 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        progress = LinearProgressIndicator(context).apply {
            max = 100
            progress = 0
            trackCornerRadius = (4 * resources.displayMetrics.density).toInt()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (8 * resources.displayMetrics.density).toInt()).apply {
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }
        addView(titleView)
        addView(subtitleView)
        addView(progress)
    }

    fun bind(title: String, percentUsed: Int, remaining: Double, formatter: java.text.NumberFormat) {
        titleView.text = title
        progress.progress = percentUsed
        subtitleView.text = "$percentUsed% terpakai · sisa ${formatter.format(remaining)}"
    }
}
