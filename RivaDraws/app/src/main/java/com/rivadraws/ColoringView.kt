package com.rivadraws

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import java.util.ArrayDeque
import kotlin.math.min

class ColoringView(context: Context) : View(context) {
    var selectedColor: Int = Color.rgb(216, 65, 82)
    var brushMode: Boolean = false
    private var artwork: Bitmap? = null
    private var template = 0
    private var lastX = 0f
    private var lastY = 0f
    private var drawing = false
    private val undoStack = ArrayDeque<Bitmap>()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(42, 43, 48); style = Paint.Style.STROKE
        strokeWidth = 3.2f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && (artwork == null || oldw != w || oldh != h)) {
            val old = artwork
            artwork = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(artwork!!)
            canvas.drawColor(Color.WHITE)
            if (old != null) canvas.drawBitmap(old, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
            else drawTemplate(canvas, w, h, template)
            old?.recycle()
            undoStack.clear()
        }
    }

    fun setTemplate(index: Int) {
        template = index
        val bitmap = artwork ?: return
        pushUndo()
        Canvas(bitmap).apply { drawColor(Color.WHITE); drawTemplate(this, bitmap.width, bitmap.height, template) }
        invalidate()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            artwork?.recycle()
            artwork = undoStack.removeLast()
            invalidate()
        }
    }

    fun exportBitmap(): Bitmap? = artwork?.copy(Bitmap.Config.ARGB_8888, false)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(248, 245, 239))
        val b = artwork ?: return
        val margin = 5f * resources.displayMetrics.density
        canvas.drawRoundRect(margin, margin, width - margin, height - margin, 18f, 18f, paperPaint)
        canvas.save()
        canvas.translate(margin, margin)
        canvas.clipRect(0f, 0f, width - 2 * margin, height - 2 * margin)
        canvas.drawBitmap(b, null, RectF(0f, 0f, width - 2 * margin, height - 2 * margin), null)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val bitmap = artwork ?: return false
        val margin = 5f * resources.displayMetrics.density
        val drawW = width - 2 * margin
        val drawH = height - 2 * margin
        if (drawW <= 0 || drawH <= 0) return false
        val x = ((event.x - margin) / drawW * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = ((event.y - margin) / drawH * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < margin || event.y < margin || event.x > width - margin || event.y > height - margin) return false
                pushUndo()
                lastX = x.toFloat(); lastY = y.toFloat(); drawing = true
                if (brushMode) {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = selectedColor; style = Paint.Style.FILL }
                    Canvas(bitmap).drawCircle(lastX, lastY, brushPaint.strokeWidth / 2, p)
                } else floodFill(bitmap, x, y, selectedColor)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (drawing && brushMode) {
                    brushPaint.color = selectedColor
                    Canvas(bitmap).drawLine(lastX, lastY, x.toFloat(), y.toFloat(), brushPaint)
                    lastX = x.toFloat(); lastY = y.toFloat()
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawing = false; performClick(); return true
            }
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    private fun pushUndo() {
        val b = artwork ?: return
        undoStack.addLast(b.copy(Bitmap.Config.ARGB_8888, true))
        while (undoStack.size > 12) undoStack.removeFirst().recycle()
    }

    private fun floodFill(bitmap: Bitmap, startX: Int, startY: Int, replacement: Int) {
        val w = bitmap.width; val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val start = startY * w + startX
        val target = pixels[start]
        if (target == replacement || (Color.red(target) < 105 && Color.green(target) < 105 && Color.blue(target) < 105)) return
        fun eligible(c: Int): Boolean = Color.red(c) > 170 && Color.green(c) > 170 && Color.blue(c) > 170
        if (!eligible(target)) return
        val queue = IntArray(w * h)
        var head = 0; var tail = 0
        queue[tail++] = start
        pixels[start] = replacement
        while (head < tail) {
            val index = queue[head++]
            val x = index % w; val y = index / w
            if (x > 0) tail = visit(index - 1, replacement, pixels, queue, tail, ::eligible)
            if (x < w - 1) tail = visit(index + 1, replacement, pixels, queue, tail, ::eligible)
            if (y > 0) tail = visit(index - w, replacement, pixels, queue, tail, ::eligible)
            if (y < h - 1) tail = visit(index + w, replacement, pixels, queue, tail, ::eligible)
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun visit(index: Int, replacement: Int, pixels: IntArray, queue: IntArray, tail: Int, eligible: (Int) -> Boolean): Int {
        if (pixels[index] != replacement && eligible(pixels[index])) {
            pixels[index] = replacement; queue[tail] = index; return tail + 1
        }
        return tail
    }

    private fun drawTemplate(canvas: Canvas, w: Int, h: Int, which: Int) {
        canvas.drawColor(Color.WHITE)
        val scale = min(w, h) * 0.82f / 400f
        canvas.save()
        canvas.translate(w / 2f - 200f * scale, h / 2f - 200f * scale)
        canvas.scale(scale, scale)
        when (which) { 0 -> drawMandala(canvas); 1 -> drawPetalRosette(canvas); else -> drawGeometric(canvas) }
        canvas.restore()
    }

    private fun drawMandala(c: Canvas) {
        val m = 200f
        for (r in listOf(35f, 70f, 105f, 140f, 175f)) c.drawCircle(m, m, r, linePaint)
        for (i in 0 until 12) {
            c.save(); c.rotate(i * 30f, m, m)
            val p = Path().apply {
                moveTo(m, m - 35f)
                cubicTo(m - 18f, m - 62f, m - 23f, m - 87f, m, m - 105f)
                cubicTo(m + 23f, m - 87f, m + 18f, m - 62f, m, m - 35f)
            }
            c.drawPath(p, linePaint); c.drawCircle(m, m - 145f, 17f, linePaint); c.drawCircle(m, m - 175f, 8f, linePaint)
            c.restore()
        }
        for (i in 0 until 8) {
            c.save(); c.rotate(i * 45f + 22.5f, m, m)
            val p = Path().apply {
                moveTo(m, m - 70f)
                cubicTo(m - 28f, m - 93f, m - 27f, m - 125f, m, m - 140f)
                cubicTo(m + 27f, m - 125f, m + 28f, m - 93f, m, m - 70f)
            }
            c.drawPath(p, linePaint); c.restore()
        }
    }

    private fun drawPetalRosette(c: Canvas) {
        val m = 200f
        c.drawCircle(m, m, 28f, linePaint); c.drawCircle(m, m, 55f, linePaint)
        for (i in 0 until 16) {
            c.save(); c.rotate(i * 22.5f, m, m)
            val p = Path().apply {
                moveTo(m, m - 25f)
                cubicTo(m - 20f, m - 65f, m - 18f, m - 105f, m, m - 135f)
                cubicTo(m + 18f, m - 105f, m + 20f, m - 65f, m, m - 25f)
            }
            c.drawPath(p, linePaint); c.restore()
        }
        c.drawCircle(m, m, 165f, linePaint); c.drawCircle(m, m, 185f, linePaint)
        for (i in 0 until 24) {
            c.save(); c.rotate(i * 15f, m, m)
            c.drawOval(RectF(m - 8f, m - 182f, m + 8f, m - 168f), linePaint); c.restore()
        }
    }

    private fun drawGeometric(c: Canvas) {
        val m = 200f
        for (i in 0 until 6) {
            c.save(); c.rotate(i * 60f, m, m)
            val p = Path().apply {
                moveTo(m, m - 180f); lineTo(m + 45f, m - 105f); lineTo(m + 35f, m - 35f)
                lineTo(m, m); lineTo(m - 35f, m - 35f); lineTo(m - 45f, m - 105f); close()
            }
            c.drawPath(p, linePaint); c.restore()
        }
        c.drawCircle(m, m, 45f, linePaint); c.drawCircle(m, m, 100f, linePaint); c.drawCircle(m, m, 180f, linePaint)
        for (i in 0 until 12) {
            c.save(); c.rotate(i * 30f, m, m); c.drawLine(m, m - 100f, m, m - 180f, linePaint); c.restore()
        }
    }
}
