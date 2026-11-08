package com.keuangan.app.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Tema: sistem / terang / gelap, tersimpan di SharedPreferences. */
object ThemeManager {

    private const val PREFS = "settings"
    private const val KEY_THEME = "theme_mode"

    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2

    fun mode(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_THEME, SYSTEM)

    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode(context)))
    }

    fun setMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME, mode).apply()
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    private fun toNightMode(mode: Int) = when (mode) {
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
