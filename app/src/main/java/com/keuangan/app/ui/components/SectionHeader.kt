package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: section header with optional trailing action ("Lihat semua").
 */
class SectionHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val actionView: TextView

    var onActionClick: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        titleView = TextView(context).apply {
            textSize = 17f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        actionView = TextView(context).apply {
            textSize = 13f
            maxLines = 1
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_blue))
            setPadding((8 * resources.displayMetrics.density).toInt(), 0, 0, 0)
        }
        addView(titleView)
        addView(actionView)
    }

    fun bind(title: String, action: String? = null) {
        titleView.text = title
        actionView.text = action
        actionView.visibility = if (action.isNullOrEmpty()) GONE else VISIBLE
        actionView.setOnClickListener { if (!action.isNullOrEmpty()) onActionClick?.invoke() }
    }
}
