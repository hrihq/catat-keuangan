package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: greeting header (icon + "Selamat pagi" + app name) and notification bell.
 */
class HeaderGreeting @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val greetingView: TextView
    private val titleView: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val iconTile = IconTile(context).apply {
            bind(R.drawable.ic_wallet, R.color.white_text, R.color.brand_blue)
        }
        addView(iconTile)

        val textColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (12 * resources.displayMetrics.density).toInt()
            }
        }
        greetingView = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
        }
        titleView = TextView(context).apply {
            text = "Catat Keuangan"
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
        }
        textColumn.addView(greetingView)
        textColumn.addView(titleView)
        addView(textColumn)

        val bell = IconTile(context).apply {
            bind(R.drawable.ic_notifications, R.color.brand_text_primary, R.color.brand_teal)
            val tileSize = (44 * resources.displayMetrics.density).toInt()
            layoutParams = LayoutParams(tileSize, tileSize).apply {
                marginStart = (12 * resources.displayMetrics.density).toInt()
            }
        }
        addView(bell)
    }

    fun bind(greeting: String, title: String) {
        greetingView.text = greeting
        titleView.text = title
    }
}
