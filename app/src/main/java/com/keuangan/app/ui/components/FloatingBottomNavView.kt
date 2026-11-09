package com.keuangan.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.keuangan.app.R
import kotlin.math.abs

/**
 * Floating pill bottom navigation, liquid-glass style:
 * - latar belakang = snapshot konten halaman yang di-blur halus (box blur 3 pass),
 *   bukan sekadar bitmap kecil yang di-upscale;
 * - tint + kilau atas + border tipis + inner shadow mengikuti tema light/dark;
 * - bayangan digambar sendiri (tanpa elevation) supaya tidak muncul kotak keras;
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
    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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

    /** Ruang kosong di sekeliling pill untuk bayangan lembut. */
    private val inset = dp(12f)
    private val padding = dp(4f)
    private val centerSize = dp(46f)
    private val iconSize = dp(21f)

    private val pillLeft: Float get() = inset
    private val pillTop: Float get() = inset
    private val pillWidth: Float get() = width - inset * 2f
    private val pillHeight: Float get() = height - inset * 2f
    private val pillRadius: Float get() = pillHeight / 2f

    /** Posisi ikon/label relatif terhadap pill, bukan seluruh view. */
    private val iconCenterY: Float get() = pillTop + pillHeight * 0.38f
    private val labelBaseline: Float get() = pillTop + pillHeight * 0.76f

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

    /**
     * Rekam hanya area nav (bukan seluruh layar) di resolusi 50%, lalu blur
     * dengan box blur 3 pass. Hasilnya halus, tanpa garis/streak seperti
     * trik downscale ekstrem sebelumnya.
     */
    private fun captureBackdrop() {
        val source = backdropSource ?: return
        if (source.width <= 0 || source.height <= 0) return
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        val srcLoc = IntArray(2)
        val myLoc = IntArray(2)
        source.getLocationInWindow(srcLoc)
        getLocationInWindow(myLoc)
        val left = myLoc[0] - srcLoc[0]
        val top = myLoc[1] - srcLoc[1]

        val scale = 0.5f
        val bw = (w * scale).toInt().coerceAtLeast(1)
        val bh = (h * scale).toInt().coerceAtLeast(1)
        var bmp = glassBitmap
        if (bmp == null || bmp.width != bw || bmp.height != bh) {
            bmp?.recycle()
            bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            glassBitmap = bmp
        }
        val target = bmp ?: return

        target.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(target)
        canvas.scale(scale, scale)
        canvas.translate(-left.toFloat(), -top.toFloat())
        canvas.clipRect(left.toFloat(), top.toFloat(), (left + w).toFloat(), (top + h).toFloat())
        try {
            source.draw(canvas)
        } catch (_: Throwable) {
            // Snapshot gagal (mis. view sedang di-recycle): pakai tint saja.
        }

        val radiusPx = (dp(6f) * scale).toInt().coerceAtLeast(1)
        boxBlur(target, radiusPx)
    }

    /** Box blur 3 pass ≈ Gaussian, jalan di CPU tapi hanya untuk area nav. */
    private fun boxBlur(bitmap: Bitmap, radius: Int) {
        if (radius <= 0) return
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(2) {
            blurPass(pixels, tmp, w, h, radius, horizontal = true)
            blurPass(tmp, pixels, w, h, radius, horizontal = false)
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun blurPass(
        src: IntArray,
        dst: IntArray,
        w: Int,
        h: Int,
        radius: Int,
        horizontal: Boolean
    ) {
        val length = if (horizontal) w else h
        val lines = if (horizontal) h else w
        val step = if (horizontal) 1 else w
        val div = radius * 2 + 1
        for (line in 0 until lines) {
            val base = if (horizontal) line * w else line
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (k in -radius..radius) {
                val c = src[base + k.coerceIn(0, length - 1) * step]
                sumA += (c ushr 24) and 0xFF
                sumR += (c ushr 16) and 0xFF
                sumG += (c ushr 8) and 0xFF
                sumB += c and 0xFF
            }
            for (i in 0 until length) {
                dst[base + i * step] =
                    ((sumA / div) shl 24) or
                        ((sumR / div) shl 16) or
                        ((sumG / div) shl 8) or
                        (sumB / div)
                val add = src[base + (i + radius + 1).coerceAtMost(length - 1) * step]
                val rem = src[base + (i - radius).coerceAtLeast(0) * step]
                sumA += ((add ushr 24) and 0xFF) - ((rem ushr 24) and 0xFF)
                sumR += ((add ushr 16) and 0xFF) - ((rem ushr 16) and 0xFF)
                sumG += ((add ushr 8) and 0xFF) - ((rem ushr 8) and 0xFF)
                sumB += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (tabs.isNotEmpty()) {
            indicatorX = tabCenterX(selectedIndex.coerceIn(0, tabs.size - 1))
            invalidate()
        }
        scheduleCapture()
    }

    private fun tabWidth(): Float =
        if (tabs.isEmpty()) 0f else (pillWidth - padding * 2f) / tabs.size

    private fun tabCenterX(index: Int): Float =
        pillLeft + padding + tabWidth() * (index + 0.5f)

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
        drawShadow(canvas)
        drawGlassBackground(canvas)
        if (tabs.isEmpty()) return

        val h = height.toFloat()
        // Indikator: selebar sel tab, setinggi isi pill (gaya FloatingBottomBar KoWSU).
        val cell = tabWidth()
        val indicatorWidth = (cell - dp(6f)).coerceAtLeast(dp(24f))
        val indicatorHeight = pillHeight - padding * 2f
        val indicatorRect = RectF(
            indicatorX - indicatorWidth / 2f,
            pillTop + padding,
            indicatorX + indicatorWidth / 2f,
            pillTop + padding + indicatorHeight
        )
        val indicatorRadius = minOf(indicatorWidth, indicatorHeight) / 2f
        val indicatorScale = if (dragging || pressedIndex == selectedIndex) pressScale else 1f
        canvas.save()
        canvas.scale(indicatorScale, indicatorScale, indicatorX, h / 2f)
        indicatorPaint.color = indicatorColor
        canvas.drawRoundRect(indicatorRect, indicatorRadius, indicatorRadius, indicatorPaint)
        canvas.restore()

        tabs.forEachIndexed { i, tab ->
            val cx = tabCenterX(i)
            val active = i == selectedIndex
            canvas.save()
            if (i == pressedIndex) {
                canvas.scale(pressScale, pressScale, cx, h / 2f)
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

    /**
     * Bayangan ambient berlapis. Bukan elevation, jadi tidak ada kotak keras:
     * tiap lapis membesar dengan alpha makin kecil dan mengikuti bentuk pill.
     */
    private fun drawShadow(canvas: Canvas) {
        if (pillWidth <= 0f || pillHeight <= 0f) return
        val layers = 10
        val maxSpread = dp(10f)
        val yOffset = dp(2f)
        for (i in layers downTo 1) {
            val t = i / layers.toFloat()
            val spread = maxSpread * t
            val alpha = (2.6f * (1f - t) + 0.9f).toInt()
            if (alpha <= 0) continue
            shadowPaint.color = Color.argb(alpha, 34, 27, 64)
            val r = RectF(
                pillLeft - spread,
                pillTop - spread + yOffset,
                pillLeft + pillWidth + spread,
                pillTop + pillHeight + spread + yOffset
            )
            canvas.drawRoundRect(r, pillRadius + spread, pillRadius + spread, shadowPaint)
        }
    }

    /** Liquid glass: snapshot konten ter-blur + tint tema + kilau + border. */
    private fun drawGlassBackground(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || pillWidth <= 0f || pillHeight <= 0f) return
        val radius = pillRadius

        glassRect.set(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)
        glassClipPath.reset()
        glassClipPath.addRoundRect(glassRect, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(glassClipPath)

        // 1) Konten di belakang nav, sudah di-blur halus.
        val bmp = glassBitmap
        val source = backdropSource
        if (bmp != null && source != null && source.width > 0 && source.height > 0) {
            val srcLoc = IntArray(2)
            val myLoc = IntArray(2)
            source.getLocationInWindow(srcLoc)
            getLocationInWindow(myLoc)
            val scaleX = bmp.width / w
            val scaleY = bmp.height / h
            val left = (myLoc[0] - srcLoc[0]).coerceAtLeast(0)
            val top = (myLoc[1] - srcLoc[1]).coerceAtLeast(0)
            paintRect.set(
                left * scaleX,
                top * scaleY,
                (left + w) * scaleX,
                (top + h) * scaleY
            )
            glassPaint.alpha = 255
            val srcRect = android.graphics.Rect(
                paintRect.left.toInt().coerceIn(0, bmp.width - 1),
                paintRect.top.toInt().coerceIn(0, bmp.height - 1),
                paintRect.right.toInt().coerceIn(1, bmp.width),
                paintRect.bottom.toInt().coerceIn(1, bmp.height)
            )
            val dstRect = android.graphics.Rect(0, 0, w.toInt(), h.toInt())
            canvas.drawBitmap(bmp, srcRect, dstRect, glassPaint)
        }

        // 2) Tint tema supaya teks tetap kontras di atas konten apa pun.
        tintPaint.color = glassTintColor
        canvas.drawRoundRect(glassRect, radius, radius, tintPaint)

        // 3) Kilau kaca di bagian atas.
        glossPaint.shader = LinearGradient(
            pillLeft, pillTop, pillLeft, pillTop + pillHeight * 0.62f,
            intArrayOf(0x40FFFFFF, 0x10FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(glassRect, radius, radius, glossPaint)
        glossPaint.shader = null

        // 4) Inner shadow tipis di bawah, memberi kesan lensa/lensa cekung.
        innerShadowPaint.shader = LinearGradient(
            pillLeft, pillTop + pillHeight * 0.55f, pillLeft, pillTop + pillHeight,
            intArrayOf(0x00201A33, 0x14201A33),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(glassRect, radius, radius, innerShadowPaint)
        innerShadowPaint.shader = null

        canvas.restore()

        // 5) Border tipis (rim cahaya).
        borderPaint.color = glassBorderColor
        canvas.drawRoundRect(
            RectF(
                pillLeft + dp(0.5f),
                pillTop + dp(0.5f),
                pillLeft + pillWidth - dp(0.5f),
                pillTop + pillHeight - dp(0.5f)
            ),
            radius, radius, borderPaint
        )
    }

    private fun drawCenterButton(canvas: Canvas, cx: Float) {
        val cy = height / 2f
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
                    animatePress(1.16f, 180)
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
                        abs(event.y - height / 2f) <= centerSize / 2f + dp(8f)
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
        return ((x - pillLeft - padding) / tabWidth()).toInt().coerceIn(0, tabs.size - 1)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        glassBitmap?.recycle()
        glassBitmap = null
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
