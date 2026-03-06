package com.learnlayout.detector_de_comidda

import android.graphics.RectF

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val box: RectF
)