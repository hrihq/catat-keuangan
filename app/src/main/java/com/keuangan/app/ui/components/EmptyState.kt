package com.keuangan.app.ui.components

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.keuangan.app.R
import com.keuangan.app.util.Motion

/**
 * Atomic: empty-state placeholder (ikon + pesan + CTA opsional).
 */
class EmptyState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var iconView: IconTile
    private val messageView: TextView
    private val actionView: MaterialButton
    private var floatAnimator: ObjectAnimator? = null

    /** Aksi tombol CTA; diisi oleh layar yang memakai EmptyState. */
    var onActionClick: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val pad = (24 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        iconView = IconTile(context).apply {
            bind(R.drawable.ic_receipt, R.color.brand_blue, R.color.brand_light_blue)
        }
        addView(iconView)

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

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (!::iconView.isInitialized) return
        if (visibility == VISIBLE) startLife() else stopLife()
    }

    override fun onDetachedFromWindow() {
        stopLife()
        super.onDetachedFromWindow()
    }

    /** Ikon masuk (fade+slide) lalu mengambang halus; hormati reduced motion. */
    private fun startLife() {
        stopLife()
        if (!Motion.shouldAnimate(context)) {
            iconView.alpha = 1f
            iconView.translationY = 0f
            return
        }
        iconView.animate().cancel()
        iconView.alpha = 0f
        iconView.translationY = dp(14).toFloat()
        iconView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        floatAnimator = ObjectAnimator.ofFloat(iconView, "translationY", 0f, dp(6).toFloat()).apply {
            duration = 2400
            startDelay = 300
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopLife() {
        if (!::iconView.isInitialized) return
        floatAnimator?.cancel()
        floatAnimator = null
        iconView.animate().cancel()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
