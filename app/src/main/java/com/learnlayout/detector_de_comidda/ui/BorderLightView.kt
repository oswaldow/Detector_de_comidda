package com.learnlayout.detector_de_comidda.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BorderLightView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }

    var progress: Float = 0f
        set(value) { field = value; invalidate() }

    private val padding = 6f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = (h - padding * 2) / 2f

        val path = Path().apply {
            addRoundRect(
                RectF(padding, padding, w - padding, h - padding),
                r, r,
                Path.Direction.CW
            )
        }

        val pm = PathMeasure(path, true)
        val total = pm.length

        val topCenter = total * 0.75f

        drawLight(canvas, pm, total, topCenter, progress, true)
        drawLight(canvas, pm, total, topCenter, progress, false)
    }

    private fun drawLight(
        canvas: Canvas,
        pm: PathMeasure,
        total: Float,
        topCenter: Float,
        progress: Float,
        forward: Boolean
    ) {
        val segmentLength = total * 0.16f
        val dotRadius = 3f
        val gap = 9f

        val tipPos = if (forward) {
            (topCenter + travel + total) % total
        } else {
            (topCenter - travel + total) % total
        }

        var remaining = segmentLength
        var currentPos = tipPos

        while (remaining > 0f) {
            val ratio = 1f - (remaining / segmentLength)
            val alpha = when {
                ratio < 0.15f -> ratio / 0.15f
                ratio > 0.6f  -> (1f - ratio) / 0.4f
                else          -> 1f
            }

            val dst = FloatArray(2)
            pm.getPosTan(((currentPos % total) + total) % total, dst, null)

            paint.color = Color.argb(
                (alpha * 230).toInt().coerceIn(0, 255),
            )
            canvas.drawCircle(dst[0], dst[1], dotRadius * alpha.coerceAtLeast(0.4f), paint)

            currentPos = if (forward) {
                currentPos - (dotRadius * 2f + gap)
            } else {
                currentPos + (dotRadius * 2f + gap)
            }
            remaining -= (dotRadius * 2f + gap)
        }
    }
}