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

    private val accent: Int by lazy {
        ContextCompat.getColor(context, R.color.brand_blue)
    }

    init {
        orientation = VERTICAL
        addView(grid)
    }

    fun setOptions(options: List<Option>) {
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
            cell.setOnClickListener { setSelected(option.name) }

            cells[option.name] = cell
            labelViews[option.name] = label
            grid.addView(cell)
        }
    }

    fun setSelected(name: String) {
        selectedName = name
        cells.forEach { (key, cell) ->
            val selected = key == name
            cell.isSelected = selected
            cell.setBackgroundResource(if (selected) R.drawable.bg_category_cell else 0)
            labelViews[key]?.apply {
                setTextColor(if (selected) accent else ContextCompat.getColor(context, R.color.brand_text_secondary))
                setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            }
        }
        if (name.isNotEmpty()) onSelectionChanged?.invoke(name)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
