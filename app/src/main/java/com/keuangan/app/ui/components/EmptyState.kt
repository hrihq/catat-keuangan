package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: empty-state placeholder.
 */
class EmptyState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val messageView: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val pad = (24 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        messageView = TextView(context).apply {
            text = "Belum ada transaksi. Tambah yang pertama!"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
            gravity = Gravity.CENTER
        }
        addView(messageView)
    }

    fun bind(message: String) {
        messageView.text = message
    }
}
