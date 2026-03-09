package com.learnlayout.detector_de_comidda

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator

class DetectionOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class AnimatedDetection(
        val result: DetectionResult,
        val index: Int,
        var scale: Float = 0f
    )

    private val detections = mutableListOf<AnimatedDetection>()
    private var imageRect  = RectF()

    // Paints
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E8FF00")
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val cornerPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E8FF00")
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#E8FF00")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.parseColor("#0D0D0D")
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    fun setImageRect(rect: RectF) {
        imageRect = rect
    }

    fun showDetections(results: List<DetectionResult>) {
        detections.clear()
        results.forEachIndexed { i, r ->
            detections.add(AnimatedDetection(r, i))
        }
        animateSequentially(0)
    }

    private fun animateSequentially(index: Int) {
        if (index >= detections.size) return

        val det      = detections[index]
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration    = 350
            interpolator = OvershootInterpolator(3.5f)
            addUpdateListener {
                det.scale = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animateSequentially(index + 1)
                }
            })
        }
        // Delay escalonado entre cada box
        postDelayed({ animator.start() }, index * 120L)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (imageRect.isEmpty) return

        val W = imageRect.width()
        val H = imageRect.height()
        val strokeW = (minOf(W, H) * 0.008f).coerceIn(3f, 10f)
        val textSz  = (minOf(W, H) * 0.038f).coerceIn(20f, 40f)
        val circleR = textSz * 0.42f

        boxPaint.strokeWidth    = strokeW
        cornerPaint.strokeWidth = strokeW * 2.2f
        textPaint.textSize      = textSz

        for (det in detections) {
            if (det.scale <= 0f) continue

            val result = det.result
            val cx = imageRect.left + result.box.centerX() * W
            val cy = imageRect.top  + result.box.centerY() * H
            val bw = result.box.width()  * W * det.scale
            val bh = result.box.height() * H * det.scale

            val left   = cx - bw / 2f
            val top    = cy - bh / 2f
            val right  = cx + bw / 2f
            val bottom = cy + bh / 2f

            // Caja
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Esquinas reforzadas
            val cLen = minOf(bw, bh) * 0.15f
            canvas.drawLine(left,  top,    left + cLen,  top,            cornerPaint)
            canvas.drawLine(left,  top,    left,          top + cLen,    cornerPaint)
            canvas.drawLine(right, top,    right - cLen,  top,           cornerPaint)
            canvas.drawLine(right, top,    right,          top + cLen,   cornerPaint)
            canvas.drawLine(left,  bottom, left + cLen,  bottom,         cornerPaint)
            canvas.drawLine(left,  bottom, left,          bottom - cLen, cornerPaint)
            canvas.drawLine(right, bottom, right - cLen,  bottom,        cornerPaint)
            canvas.drawLine(right, bottom, right,          bottom - cLen, cornerPaint)

            // Círculo con número
            val ncx = left + circleR + strokeW
            val ncy = if (top >= circleR * 2 + strokeW)
                top - circleR else top + circleR + strokeW

            canvas.save()
            canvas.scale(det.scale, det.scale, ncx, ncy)
            canvas.drawCircle(ncx, ncy, circleR, bgPaint)
            val textY = ncy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText("${det.index + 1}", ncx, textY, textPaint)
            canvas.restore()
        }
    }
}