package com.learnlayout.detector_de_comidda.ui

import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator

fun View.attachSpring() {
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate()
                    .scaleX(0.88f).scaleY(0.88f)
                    .setDuration(100)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(OvershootInterpolator(3f))
                    .start()
                if (event.action == MotionEvent.ACTION_UP) v.performClick()
            }
        }
        true
    }
}