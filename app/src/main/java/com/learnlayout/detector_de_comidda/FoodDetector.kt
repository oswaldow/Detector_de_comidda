package com.learnlayout.detector_de_comidda

import ai.onnxruntime.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

class FoodDetector(context: Context) {

    private val session: OrtSession
    private val env: OrtEnvironment
    private val inputSize     = 640
    private val confThreshold = 0.55f
    private val iouThreshold  = 0.35f
    private val containThresh = 0.85f
    private val maxBoxRatio   = 0.92f  // descarta cajas que cubran >92% de la imagen

    private val labels = listOf(
        "Manzana",    // 0 - Apple
        "Aguacate",   // 1 - Avocado
        "Plátano",    // 2 - Banana
        "Pan",        // 3 - Bread
        "Col",        // 4 - Cabbage
        "Zanahoria",  // 5 - Carrot
        "Ajo",        // 6 - Garlic
        "Melón",      // 7 - Melon
        "Calabaza",   // 8 - Pumpkin
        "Sandía"      // 9 - Watermelon
    )
// nc = 10 → output shape [1, 14, 8400] (4 bbox + 10 clases)

    init {
        env = OrtEnvironment.getEnvironment()
        val modelFile = File(context.cacheDir, "best.onnx")
        if (modelFile.exists()) modelFile.delete()
        context.assets.open("best.onnx").use { inp ->
            FileOutputStream(modelFile).use { out ->
                val buf = ByteArray(4 * 1024 * 1024); var n: Int
                while (inp.read(buf).also { n = it } != -1) out.write(buf, 0, n)
            }
        }
        android.util.Log.d("FD", "Modelo: ${modelFile.length() / 1024 / 1024} MB")
        session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val resized     = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputTensor = bitmapToTensor(resized)
        val inputName   = session.inputNames.iterator().next()
        val rawOutput   = session.run(mapOf(inputName to inputTensor))

        val out0  = rawOutput[0].value as Array<*>
        val batch = out0[0] as Array<*>
        val rows  = batch.size
        val cols  = (batch[0] as FloatArray).size
        val mat   = Array(rows) { r -> batch[r] as FloatArray }

        // Nuevo modelo: [10 x 8400] = 4 bbox + 6 clases
        android.util.Log.d("FD", "shape: [$rows x $cols] — clases en modelo: ${rows - 4}")
        val result = parseOutput(mat, rows, cols)
        inputTensor.close(); rawOutput.close()
        return result
    }

    private fun parseOutput(mat: Array<FloatArray>, rows: Int, cols: Int): List<DetectionResult> {
        val nc       = labels.size
        val clsStart = 4

        var globalMax = -Float.MAX_VALUE
        for (r in clsStart until clsStart + nc)
            for (c in 0 until cols)
                if (mat[r][c] > globalMax) globalMax = mat[r][c]

        val needsSigmoid = globalMax > 1.0f
        android.util.Log.d("FD", "globalMax=$globalMax needsSigmoid=$needsSigmoid")

        val dets = mutableListOf<DetectionResult>()

        for (col in 0 until cols) {
            var best = -Float.MAX_VALUE; var bestCls = 0
            for (c in 0 until nc) {
                val raw = mat[clsStart + c][col]
                val s   = if (needsSigmoid) sigmoid(raw) else raw
                if (s > best) { best = s; bestCls = c }
            }
            if (best < confThreshold) continue

            val cx = mat[0][col]; val cy = mat[1][col]
            val w  = mat[2][col]; val h  = mat[3][col]
            val x1 = ((cx - w / 2f) / inputSize).coerceIn(0f, 1f)
            val y1 = ((cy - h / 2f) / inputSize).coerceIn(0f, 1f)
            val x2 = ((cx + w / 2f) / inputSize).coerceIn(0f, 1f)
            val y2 = ((cy + h / 2f) / inputSize).coerceIn(0f, 1f)
            if (x2 <= x1 || y2 <= y1) continue

            // Descartar cajas que cubren casi toda la imagen
            val bw = x2 - x1; val bh = y2 - y1
            if (bw > maxBoxRatio || bh > maxBoxRatio) continue

            dets.add(DetectionResult(labels[bestCls], best, RectF(x1, y1, x2, y2)))
        }

        android.util.Log.d("FD", "Antes NMS: ${dets.size}")
        val afterNMS = applyNMS(dets)
        android.util.Log.d("FD", "Después NMS: ${afterNMS.size}")
        afterNMS.forEach { d ->
            android.util.Log.d("FD",
                "  → ${d.label} ${"%.1f".format(d.confidence * 100)}% ${d.box}")
        }
        return afterNMS
    }

    private fun applyNMS(dets: List<DetectionResult>): List<DetectionResult> {
        val sorted = dets.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<DetectionResult>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)
            sorted.removeAll { other ->
                iou(best.box, other.box) > iouThreshold ||
                        containmentRatio(other.box, best.box) > containThresh
            }
        }
        return result
    }

    private fun containmentRatio(inner: RectF, outer: RectF): Float {
        val iL = maxOf(inner.left, outer.left);   val iT = maxOf(inner.top, outer.top)
        val iR = minOf(inner.right, outer.right); val iB = minOf(inner.bottom, outer.bottom)
        val inter = maxOf(0f, iR - iL) * maxOf(0f, iB - iT)
        val innerArea = inner.width() * inner.height()
        return if (innerArea <= 0f) 0f else inter / innerArea
    }

    private fun iou(a: RectF, b: RectF): Float {
        val iL = maxOf(a.left, b.left);   val iT = maxOf(a.top, b.top)
        val iR = minOf(a.right, b.right); val iB = minOf(a.bottom, b.bottom)
        val inter = maxOf(0f, iR - iL) * maxOf(0f, iB - iT)
        val union = a.width() * a.height() + b.width() * b.height() - inter
        return if (union <= 0f) 0f else inter / union
    }

    private fun bitmapToTensor(bmp: Bitmap): OnnxTensor {
        val w = bmp.width; val h = bmp.height
        val px = IntArray(w * h)
        bmp.copy(Bitmap.Config.ARGB_8888, false).getPixels(px, 0, w, 0, 0, w, h)
        val arr = FloatArray(3 * h * w)
        for (i in px.indices) {
            val p = px[i]
            arr[i]             = ((p shr 16) and 0xFF) / 255f
            arr[h * w + i]     = ((p shr 8)  and 0xFF) / 255f
            arr[2 * h * w + i] = (p           and 0xFF) / 255f
        }
        return OnnxTensor.createTensor(env, FloatBuffer.wrap(arr),
            longArrayOf(1, 3, h.toLong(), w.toLong()))
    }

    private fun sigmoid(x: Float) = 1f / (1f + Math.exp(-x.toDouble()).toFloat())

    fun close() { session.close(); env.close() }
}