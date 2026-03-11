package com.learnlayout.detector_de_comidda.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class AnimatedTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        orientation = VERTICAL
    }

    fun animateText(
        label: String,
        textSizeSp: Float,
        textColorRes: Int,
        baseDelayMs: Long = 0L,
        charDelayMs: Long = 60L
    ) {
        removeAllViews()

        val lines = label.split("\n")
        var charIndex = 0

        lines.forEach { line ->
            val lineLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.START
                layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
            }

            line.forEach { char ->
                val tv = TextView(context).apply {
                    text = char.toString()
                    textSize = textSizeSp
                    setTextColor(textColorRes)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                    )
                    alpha = 0f
                    translationY = -60f
                }

                val delay = baseDelayMs + charIndex * charDelayMs
                tv.postDelayed({
                    tv.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }, delay)

                lineLayout.addView(tv)
                charIndex++
            }

            addView(lineLayout)
        }
    }
}