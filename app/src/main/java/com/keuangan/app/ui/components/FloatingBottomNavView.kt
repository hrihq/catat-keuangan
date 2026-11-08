package com.keuangan.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import kotlin.math.abs

/**
 * Atomic: floating pill bottom navigation with a draggable active indicator.
 * Mendukung slot aksi tengah (+) yang menonjol.
 */
class FloatingBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Tab(val iconRes: Int, val label: String)

    var onTabSelected: ((Int) -> Unit)? = null
    var onCenterAction: (() -> Unit)? = null

    var selectedIndex: Int = 0
        private set

    /** Index slot tombol aksi tengah; -1 = tidak ada. */
    var centerActionIndex: Int = -1

    var tabs: List<Tab> = emptyList()
        set(value) {
            field = value
            iconDrawables.clear()
            iconDrawables.addAll(value.map { context.getDrawable(it.iconRes)?.mutate() })
            if (width > 0 && value.isNotEmpty()) {
                indicatorX = tabCenterX(selectedIndex.coerceIn(0, value.size - 1))
            }
            invalidate()
        }

    private val iconDrawables = mutableListOf<Drawable?>()

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(2.5f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(10f)
        typeface = Typeface.DEFAULT_BOLD
    }

    private val indicatorColor: Int by lazy {
        MaterialColors.getColor(this, MaterialR.attr.colorPrimaryContainer)
    }
    private val activeColor: Int by lazy {
        MaterialColors.getColor(this, MaterialR.attr.colorPrimary)
    }
    private val inactiveColor: Int by lazy {
        MaterialColors.getColor(this, MaterialR.attr.colorOnSurfaceVariant)
    }
    private val centerColor: Int by lazy {
        MaterialColors.getColor(this, MaterialR.attr.colorPrimary)
    }
    private val onCenterColor: Int by lazy {
        MaterialColors.getColor(this, MaterialR.attr.colorOnPrimary)
    }

    private val padding = dp(4f)
    private val indicatorSize = dp(40f)
    private val centerSize = dp(50f)
    private val iconSize = dp(24f)
    private val iconCenterY = dp(22f)
    private val labelBaseline = dp(52f)

    private var indicatorX = 0f
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragStartIndicatorX = 0f
    private var dragging = false
    private var snapAnimator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (tabs.isNotEmpty()) {
            indicatorX = tabCenterX(selectedIndex.coerceIn(0, tabs.size - 1))
            invalidate()
        }
    }

    private fun tabWidth(): Float =
        if (tabs.isEmpty()) 0f else (width - padding * 2f) / tabs.size

    private fun tabCenterX(index: Int): Float = padding + tabWidth() * (index + 0.5f)

    private fun selectableIndices(): List<Int> =
        tabs.indices.filter { it != centerActionIndex }

    fun setSelectedIndex(index: Int, animate: Boolean = true) {
        if (tabs.isEmpty()) return
        val targetIndex = index.coerceIn(0, tabs.size - 1)
        val target = tabCenterX(targetIndex)
        selectedIndex = targetIndex
        snapAnimator?.cancel()
        if (animate && width > 0) {
            snapAnimator = ValueAnimator.ofFloat(indicatorX, target).apply {
                duration = 200
                addUpdateListener {
                    indicatorX = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            indicatorX = target
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (tabs.isEmpty()) return

        indicatorPaint.color = indicatorColor
        val half = indicatorSize / 2f
        canvas.drawRoundRect(
            RectF(
                indicatorX - half,
                iconCenterY - half,
                indicatorX + half,
                iconCenterY + half
            ),
            dp(14f), dp(14f), indicatorPaint
        )

        tabs.forEachIndexed { i, tab ->
            val cx = tabCenterX(i)
            val active = i == selectedIndex

            iconDrawables.getOrNull(i)?.let { drawable ->
                val iconHalf = iconSize / 2f
                drawable.setBounds(
                    (cx - iconHalf).toInt(),
                    (iconCenterY - iconHalf).toInt(),
                    (cx + iconHalf).toInt(),
                    (iconCenterY + iconHalf).toInt()
                )
                drawable.setTint(if (active) activeColor else inactiveColor)
                drawable.draw(canvas)
            }

            textPaint.color = if (active) activeColor else inactiveColor
            canvas.drawText(tab.label, cx, labelBaseline, textPaint)
        }

        if (centerActionIndex in tabs.indices) {
            drawCenterButton(canvas, tabCenterX(centerActionIndex))
        }
    }

    private fun drawCenterButton(canvas: Canvas, cx: Float) {
        val cy = iconCenterY + dp(2f)
        val radius = centerSize / 2f
        centerPaint.color = centerColor
        canvas.drawCircle(cx, cy, radius, centerPaint)

        plusPaint.color = onCenterColor
        val arm = radius * 0.42f
        canvas.drawLine(cx - arm, cy, cx + arm, cy, plusPaint)
        canvas.drawLine(cx, cy - arm, cx, cy + arm, plusPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tabs.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.x
                dragStartY = event.y
                dragStartIndicatorX = indicatorX
                dragging = false
                snapAnimator?.cancel()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - dragStartX
                val dy = event.y - dragStartY
                if (!dragging && abs(dx) > dp(8f) && abs(dx) > abs(dy)) dragging = true
                if (dragging) {
                    val selectable = selectableIndices()
                    if (selectable.isNotEmpty()) {
                        val min = tabCenterX(selectable.min())
                        val max = tabCenterX(selectable.max())
                        indicatorX = (dragStartIndicatorX + dx).coerceIn(min, max)
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (centerActionIndex in tabs.indices) {
                    val centerX = tabCenterX(centerActionIndex)
                    if (abs(event.x - centerX) <= centerSize / 2f + dp(8f) &&
                        abs(event.y - (iconCenterY + dp(2f))) <= centerSize / 2f + dp(8f)
                    ) {
                        onCenterAction?.invoke()
                        performClick()
                        return true
                    }
                }
                val previous = selectedIndex
                val index = if (dragging) {
                    dragging = false
                    nearestSelectableIndex(indicatorX)
                } else {
                    indexOfTab(event.x)
                }
                if (index >= 0 && index != centerActionIndex) {
                    setSelectedIndex(index)
                    if (index != previous) onTabSelected?.invoke(index)
                }
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun nearestSelectableIndex(x: Float): Int {
        val selectable = selectableIndices()
        if (selectable.isEmpty()) return selectedIndex
        return selectable.minByOrNull { abs(tabCenterX(it) - x) } ?: selectedIndex
    }

    private fun indexOfTab(x: Float): Int {
        if (tabs.isEmpty() || tabWidth() <= 0f) return -1
        return ((x - padding) / tabWidth()).toInt().coerceIn(0, tabs.size - 1)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
