package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.keuangan.app.R

/**
 * Atomic: empty-state placeholder (ikon + pesan + CTA opsional).
 */
class EmptyState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val messageView: TextView
    private val actionView: MaterialButton

    /** Aksi tombol CTA; diisi oleh layar yang memakai EmptyState. */
    var onActionClick: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val pad = (24 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        val icon = IconTile(context).apply {
            bind(R.drawable.ic_receipt, R.color.brand_blue, R.color.brand_light_blue)
        }
        addView(icon)

        messageView = TextView(context).apply {
            text = "Belum ada transaksi. Tambah yang pertama!"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
            setPadding(0, dp(14), 0, 0)
        }
        addView(messageView)

        actionView = MaterialButton(context).apply {
            text = "Tambah transaksi"
            textSize = 14f
            isAllCaps = false
            setTextColor(ContextCompat.getColor(context, R.color.white_text))
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.brand_blue)
            )
            setPadding(dp(20), dp(10), dp(20), dp(10))
            visibility = GONE
            setOnClickListener { onActionClick?.invoke() }
        }
        addView(actionView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(16)
        })
    }

    fun bind(message: String, actionLabel: String? = null) {
        messageView.text = message
        if (actionLabel.isNullOrEmpty()) {
            actionView.visibility = GONE
        } else {
            actionView.text = actionLabel
            actionView.visibility = VISIBLE
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
