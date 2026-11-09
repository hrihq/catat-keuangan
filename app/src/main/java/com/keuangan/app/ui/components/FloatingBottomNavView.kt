package com.keuangan.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.keuangan.app.R
import kotlin.math.abs

/**
 * Floating pill bottom navigation, liquid-glass style:
 * - latar belakang = snapshot konten halaman yang di-blur (backdrop blur asli),
 *   jadi teks halaman tidak lagi menembus nav tanpa terbaca;
 * - tint + kilau atas + border tipis mengikuti tema light/dark;
 * - indikator tab bisa di-drag, ikon menekan dengan animasi pegas.
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
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
    private val glassTintColor: Int by lazy {
        ContextCompat.getColor(context, R.color.nav_glass_bg)
    }
    private val glassBorderColor: Int by lazy {
        ContextCompat.getColor(context, R.color.nav_glass_stroke)
    }

    private val padding = dp(6f)
    private val indicatorSize = dp(42f)
    private val centerSize = dp(56f)
    private val iconSize = dp(24f)
    private val iconCenterY = dp(32f)
    private val labelBaseline = dp(62f)

    private var indicatorX = 0f
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragStartIndicatorX = 0f
    private var dragging = false
    private var snapAnimator: ValueAnimator? = null

    private var pressedIndex = -1
    private var pressScale = 1f
    private var pressAnimator: ValueAnimator? = null

    // ---- Liquid glass backdrop ----
    private var backdropSource: View? = null
    private var glassBitmap: Bitmap? = null
    private var capturePending = false
    private val glassClipPath = Path()
    private val glassRect = RectF()
    private val paintRect = RectF()

    init {
        isClickable = true
        isFocusable = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
            }
        }
        clipToOutline = false
    }

    /**
     * Hubungkan ke view konten halaman. Snapshot konten direkam ulang saat
     * halaman di-scroll / berubah layout, lalu dipakai sebagai latar glass.
     */
    fun bindBackdrop(source: View) {
        backdropSource = source
        source.viewTreeObserver.addOnScrollChangedListener { scheduleCapture() }
        source.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> scheduleCapture() }
        scheduleCapture()
        postDelayed({ scheduleCapture() }, 350)
    }

    /** Panggil setelah ganti tab/konten agar snapshot ikut terbarui. */
    fun refreshGlass() {
        scheduleCapture()
        postDelayed({ scheduleCapture() }, 250)
    }

    private fun scheduleCapture() {
        if (capturePending) return
        capturePending = true
        postOnAnimation {
            capturePending = false
            captureBackdrop()
            invalidate()
        }
    }

    private fun captureBackdrop() {
        val source = backdropSource ?: return
        if (source.width <= 0 || source.height <= 0) return
        // Downscale kuat + upscale bilinear = blur halus dan murah (tanpa RenderScript).
        val scale = 0.09f
        val bw = (source.width * scale).toInt().coerceAtLeast(1)
        val bh = (source.height * scale).toInt().coerceAtLeast(1)
        var bmp = glassBitmap
        if (bmp == null || bmp.width != bw || bmp.height != bh) {
            bmp?.recycle()
            bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            glassBitmap = bmp
        }
        val target = bmp ?: return
        try {
            val canvas = Canvas(target)
            canvas.drawColor(0x00000000)
            canvas.scale(bw / source.width.toFloat(), bh / source.height.toFloat())
            source.draw(canvas)
        } catch (_: Throwable) {
            // Snapshot gagal (mis. view sedang di-recycle): pakai tint saja.
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            elevation = dp(10f)
        }
        if (tabs.isNotEmpty()) {
            indicatorX = tabCenterX(selectedIndex.coerceIn(0, tabs.size - 1))
            invalidate()
        }
        scheduleCapture()
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
                duration = 260
                interpolator = OvershootInterpolator(1.1f)
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
        drawGlassBackground(canvas)
        if (tabs.isEmpty()) return

        val half = indicatorSize / 2f
        val indicatorRect = RectF(
            indicatorX - half,
            iconCenterY - half,
            indicatorX + half,
            iconCenterY + half
        )
        canvas.save()
        canvas.scale(pressScale, pressScale, indicatorRect.centerX(), indicatorRect.centerY())
        indicatorPaint.color = indicatorColor
        canvas.drawRoundRect(indicatorRect, dp(14f), dp(14f), indicatorPaint)
        canvas.restore()

        tabs.forEachIndexed { i, tab ->
            val cx = tabCenterX(i)
            val active = i == selectedIndex
            canvas.save()
            if (i == pressedIndex) {
                canvas.scale(pressScale, pressScale, cx, iconCenterY + dp(14f))
            }

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
            canvas.restore()
        }

        if (centerActionIndex in tabs.indices) {
            drawCenterButton(canvas, tabCenterX(centerActionIndex))
        }
    }

    /** Liquid glass: snapshot konten ter-blur + tint tema + kilau + border. */
    private fun drawGlassBackground(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val radius = h / 2f

        glassRect.set(0f, 0f, w, h)
        glassClipPath.reset()
        glassClipPath.addRoundRect(glassRect, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(glassClipPath)

        // 1) Konten di belakang nav, sudah diblur lewat downscale + bilinear upsample.
        val bmp = glassBitmap
        val source = backdropSource
        if (bmp != null && source != null && source.width > 0 && source.height > 0) {
            val srcLoc = IntArray(2)
            val myLoc = IntArray(2)
            source.getLocationInWindow(srcLoc)
            getLocationInWindow(myLoc)
            val scaleX = bmp.width / source.width.toFloat()
            val scaleY = bmp.height / source.height.toFloat()
            val left = (myLoc[0] - srcLoc[0]).coerceAtLeast(0)
            val top = (myLoc[1] - srcLoc[1]).coerceAtLeast(0)
            paintRect.set(
                left * scaleX,
                top * scaleY,
                (left + w) * scaleX,
                (top + h) * scaleY
            )
            glassPaint.alpha = 255
            canvas.drawBitmap(bmp, paintRect, glassRect, glassPaint)
        }

        // 2) Tint tema supaya teks tetap kontras di atas konten apa pun.
        tintPaint.color = glassTintColor
        canvas.drawRoundRect(glassRect, radius, radius, tintPaint)

        // 3) Kilau kaca di bagian atas.
        glossPaint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.62f,
            intArrayOf(0x4DFFFFFF, 0x14FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(glassRect, radius, radius, glossPaint)
        glossPaint.shader = null

        canvas.restore()

        // 4) Border tipis + garis dalam (memberi kesan lensa kaca).
        borderPaint.color = glassBorderColor
        canvas.drawRoundRect(
            RectF(dp(0.5f), dp(0.5f), w - dp(0.5f), h - dp(0.5f)),
            radius, radius, borderPaint
        )
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
                val touched = indexOfTab(event.x)
                if (touched >= 0 && touched != centerActionIndex) {
                    pressedIndex = touched
                    animatePress(1.14f, 140)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - dragStartX
                val dy = event.y - dragStartY
                if (!dragging && abs(dx) > dp(8f) && abs(dx) > abs(dy)) {
                    dragging = true
                    pressedIndex = -1
                    animatePress(1f, 140)
                }
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
                if (event.actionMasked == MotionEvent.ACTION_UP && centerActionIndex in tabs.indices) {
                    val centerX = tabCenterX(centerActionIndex)
                    if (abs(event.x - centerX) <= centerSize / 2f + dp(8f) &&
                        abs(event.y - (iconCenterY + dp(2f))) <= centerSize / 2f + dp(8f)
                    ) {
                        pressedIndex = -1
                        animatePress(1f, 160)
                        onCenterAction?.invoke()
                        performClick()
                        return true
                    }
                }
                pressedIndex = -1
                animatePress(1f, 220)
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

    private fun animatePress(target: Float, duration: Long) {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(pressScale, target).apply {
            this.duration = duration
            interpolator = OvershootInterpolator(1.4f)
            addUpdateListener {
                pressScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        glassBitmap?.recycle()
        glassBitmap = null
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
