package com.keuangan.app.ui

import android.view.View

/** Animasi "halo": denyut skala lembut untuk menandai elemen interaktif. */
object Halo {

    fun pulse(view: View, delay: Long = 0) {
        view.postDelayed({
            view.animate().cancel()
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 1f
            view.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .alpha(0.88f)
                .setDuration(240)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(240)
                        .start()
                }
                .start()
        }, delay)
    }
}
