package com.keuangan.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R

/**
 * Atomic: a single transaction row (icon tile + title/subtitle + amount).
 */
class TransactionRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val iconTile: IconTile
    private val titleView: TextView
    private val subtitleView: TextView
    private val amountView: TextView
    private val deleteButton: ImageView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_transaction_row)
        foreground = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x1F000000),
            null,
            null
        )
        val padH = (16 * resources.displayMetrics.density).toInt()
        val padV = (12 * resources.displayMetrics.density).toInt()
        setPadding(padH, padV, padH, padV)

        iconTile = IconTile(context).apply {
            bind(R.drawable.ic_receipt, R.color.brand_text_primary, R.color.brand_light_blue)
        }
        addView(iconTile)

        val textColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (12 * resources.displayMetrics.density).toInt()
                marginEnd = (12 * resources.displayMetrics.density).toInt()
            }
        }
        titleView = TextView(context).apply {
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
        }
        subtitleView = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
        }
        textColumn.addView(titleView)
        textColumn.addView(subtitleView)
        addView(textColumn)

        amountView = TextView(context).apply {
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.expense_red))
        }
        addView(amountView)

        deleteButton = ImageView(context).apply {
            layoutParams = LayoutParams(dp(36), dp(36))
            setImageResource(R.drawable.ic_close)
            setColorFilter(ContextCompat.getColor(context, R.color.brand_text_secondary))
            setPadding(dp(9), dp(9), dp(9), dp(9))
            background = ContextCompat.getDrawable(context, R.drawable.bg_delete_circle)
            visibility = GONE
        }
        addView(deleteButton)
    }

    fun bind(
        title: String,
        subtitle: String,
        amountText: String,
        isIncome: Boolean,
        iconRes: Int,
        iconTint: Int,
        iconBackground: Int,
        onDelete: (() -> Unit)? = null
    ) {
        titleView.text = title
        subtitleView.text = subtitle
        amountView.text = amountText
        amountView.setTextColor(
            ContextCompat.getColor(context, if (isIncome) R.color.income_green else R.color.expense_red)
        )
        iconTile.bind(iconRes, iconTint, iconBackground)

        deleteButton.visibility = if (onDelete != null) VISIBLE else GONE
        deleteButton.setOnClickListener { onDelete?.invoke() }
        if (onDelete != null) {
            isClickable = true
            setOnClickListener { onDelete() }
            setOnLongClickListener {
                onDelete()
                true
            }
        }
    }

    fun removeWithAnimation(onDone: () -> Unit) {
        if (parent == null) {
            onDone()
            return
        }
        animate().cancel()
        alpha = 1f
        animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction {
                val params = layoutParams
                val startHeight = height
                android.animation.ValueAnimator.ofInt(startHeight, 0).apply {
                    duration = 220
                    interpolator = android.view.animation.DecelerateInterpolator()
                    addUpdateListener {
                        params.height = it.animatedValue as Int
                        layoutParams = params
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            onDone()
                        }
                    })
                    start()
                }
            }.start()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
