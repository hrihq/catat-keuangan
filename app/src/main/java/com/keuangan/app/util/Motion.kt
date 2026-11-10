package com.keuangan.app.util

import android.content.Context
import android.provider.Settings
import android.view.View

/**
 * Helper animasi: hormati reduced motion (Animator duration scale = 0).
 * Semua animasi baru di app ini lewat sini.
 */
object Motion {

    fun shouldAnimate(context: Context): Boolean {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return scale > 0f
    }

    /** Masuk berjenjang: slide-up + fade, delay index * 40ms (maks 200ms). */
    fun staggerIn(view: View, index: Int, distanceDp: Float = 12f) {
        if (!shouldAnimate(view.context)) {
            view.alpha = 1f
            view.translationY = 0f
            return
        }
        view.animate().cancel()
        view.alpha = 0f
        view.translationY = distanceDp * view.resources.displayMetrics.density
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay((index * 40L).coerceAtMost(200L))
            .setDuration(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
}
