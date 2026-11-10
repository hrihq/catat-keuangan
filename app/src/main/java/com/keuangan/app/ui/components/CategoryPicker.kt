package com.keuangan.app.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keuangan.app.R
import com.keuangan.app.util.Haptics
import com.keuangan.app.util.Motion

/** Atomic: pemilih kategori berbentuk grid ikon (dipakai di form catat transaksi). */
class CategoryPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    data class Option(
        val name: String,
        val iconRes: Int,
        val tint: Int,
        val background: Int
    )

    var onSelectionChanged: ((String) -> Unit)? = null

    var selectedName: String = ""
        private set

    private val grid = GridLayout(context).apply {
        columnCount = 4
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private val cells = mutableMapOf<String, LinearLayout>()
    private val labelViews = mutableMapOf<String, TextView>()
    private val tileViews = mutableMapOf<String, IconTile>()
    private val optionMap = mutableMapOf<String, Option>()

    private val accent: Int by lazy {
        ContextCompat.getColor(context, R.color.brand_blue)
    }

    init {
        orientation = VERTICAL
        addView(grid)
    }

    fun setOptions(options: List<Option>) {
        val crossfade = grid.childCount > 0 && Motion.shouldAnimate(context)
        grid.animate().cancel()
        grid.removeAllViews()
        cells.clear()
        labelViews.clear()

        options.forEach { option ->
            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                isClickable = true
                isFocusable = true
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(4), dp(8), dp(4), dp(8))
                }
                setPadding(dp(4), dp(8), dp(4), dp(8))
            }

            val tile = IconTile(context).apply {
                layoutParams = LayoutParams(dp(52), dp(52))
                bind(option.iconRes, option.tint, option.background)
            }

            val label = TextView(context).apply {
                text = option.name
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
            }

            cell.addView(tile)
            cell.addView(label)
            cell.setOnClickListener {
                Haptics.tap(cell)
                setSelected(option.name)
            }

            cells[option.name] = cell
            labelViews[option.name] = label
            tileViews[option.name] = tile
            optionMap[option.name] = option
            grid.addView(cell)
        }

        if (crossfade) {
            grid.alpha = 0f
            grid.animate()
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else {
            grid.alpha = 1f
        }
    }

    fun setSelected(name: String, animate: Boolean = true) {
        selectedName = name
        cells.forEach { (key, cell) ->
            val selected = key == name
            cell.isSelected = selected
            cell.setBackgroundResource(if (selected) R.drawable.bg_category_cell else 0)
            // Kuning mengikuti kategori yang dipilih.
            tileViews[key]?.bind(
                optionMap[key]?.iconRes ?: R.drawable.ic_add,
                if (selected) R.color.on_yellow else R.color.brand_blue,
                if (selected) R.color.brand_yellow else R.color.brand_light_blue
            )
            if (selected && animate && Motion.shouldAnimate(context)) {
                // Animasi pop saat kategori dipilih.
                cell.animate().cancel()
                cell.scaleX = 0.88f
                cell.scaleY = 0.88f
                cell.alpha = 0.5f
                cell.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.4f))
                    .start()
            }
            labelViews[key]?.apply {
                setTextColor(if (selected) accent else ContextCompat.getColor(context, R.color.brand_text_secondary))
                setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            }
        }
        if (name.isNotEmpty()) onSelectionChanged?.invoke(name)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
