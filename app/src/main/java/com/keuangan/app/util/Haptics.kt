package com.keuangan.app.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Umpan balik haptik ringan (tanpa dependency baru). */
object Haptics {

    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun confirm(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
