package com.keuangan.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Atomic: floating pill bottom navigation with a draggable active indicator.
 * Drag horizontally (Miuix-style): the indicator follows the finger and snaps
 * to the nearest tab on release; tapping a tab also works.
 */
class FloatingBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Tab(val iconRes: Int, val label: String)

    var onTabSelected: ((Int) -> Unit)? = null

    var selectedIndex: Int = 0
        private set

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

    private val padding = dp(4f)
    private val indicatorSize = dp(40f)
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
        val indicatorRect = android.graphics.RectF(
            indicatorX - half,
            iconCenterY - half,
            indicatorX + half,
            iconCenterY + half
        )
        canvas.drawRoundRect(indicatorRect, dp(14f), dp(14f), indicatorPaint)

        tabs.forEachIndexed { i, tab ->
            val cx = tabCenterX(i)
            val active = i == selectedIndex

            iconDrawables.getOrNull(i)?.let { drawable ->
                val half = iconSize / 2f
                drawable.setBounds(
                    (cx - half).toInt(),
                    (iconCenterY - half).toInt(),
                    (cx + half).toInt(),
                    (iconCenterY + half).toInt()
                )
                drawable.setTint(if (active) activeColor else inactiveColor)
                drawable.draw(canvas)
            }

            textPaint.color = if (active) activeColor else inactiveColor
            canvas.drawText(tab.label, cx, labelBaseline, textPaint)
        }
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
                    val min = tabCenterX(0)
                    val max = tabCenterX(tabs.size - 1)
                    indicatorX = (dragStartIndicatorX + dx).coerceIn(min, max)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val previous = selectedIndex
                val index = if (dragging) {
                    dragging = false
                    indexOfPosition(indicatorX)
                } else {
                    indexOfTab(event.x)
                }
                if (index >= 0) {
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

    private fun indexOfPosition(x: Float): Int {
        if (tabs.isEmpty() || tabWidth() <= 0f) return selectedIndex
        return (((x - padding) / tabWidth()) - 0.5f).roundToInt().coerceIn(0, tabs.size - 1)
    }

    private fun indexOfTab(x: Float): Int {
        if (tabs.isEmpty() || tabWidth() <= 0f) return -1
        return ((x - padding) / tabWidth()).toInt().coerceIn(0, tabs.size - 1)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
