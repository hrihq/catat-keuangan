package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: icon tile with a soft tinted background (transaction rows, header, bell).
 * Background colour is set in code so it can be driven by category.
 */
class IconTile @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val backgroundView: ImageView
    private val iconView: ImageView

    init {
        val size = (44 * resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size)

        backgroundView = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setImageResource(R.drawable.bg_icon_tile)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(backgroundView)

        iconView = ImageView(context).apply {
            layoutParams = LayoutParams(
                (22 * resources.displayMetrics.density).toInt(),
                (22 * resources.displayMetrics.density).toInt(),
                Gravity.CENTER
            )
            setImageResource(R.drawable.ic_receipt)
        }
        addView(iconView)
    }

    fun bind(iconRes: Int, tintColorRes: Int, backgroundTint: Int) {
        iconView.setImageResource(iconRes)
        iconView.setColorFilter(ContextCompat.getColor(context, tintColorRes))
        backgroundView.setColorFilter(ContextCompat.getColor(context, backgroundTint))
    }
}
