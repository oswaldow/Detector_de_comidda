package com.learnlayout.detector_de_comidda

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.learnlayout.detector_de_comidda.databinding.ActivityResultBinding
import kotlinx.coroutines.*

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val dotAnimators = mutableListOf<AnimatorSet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        startDotsAnimation()

        val imageUriString = intent.getStringExtra("image_uri") ?: ""
        processImage(imageUriString)
    }

    private fun startDotsAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        val riseAmount = -22f   // cuantos dp sube cada circulo
        val riseDuration = 300L // duracion de subida en ms
        val stepDelay = 150L    // delay entre cada circulo

        // Cada circulo tiene su propio ciclo: sube y baja, con delay escalonado
        dots.forEachIndexed { index, dot ->
            val delay = index * stepDelay
            val cycleDuration = dots.size * stepDelay + riseDuration * 2

            val up = ObjectAnimator.ofFloat(dot, "translationY", 0f, riseAmount).apply {
                duration = riseDuration
                interpolator = DecelerateInterpolator()
                startDelay = delay
            }
            val down = ObjectAnimator.ofFloat(dot, "translationY", riseAmount, 0f).apply {
                duration = riseDuration
                interpolator = DecelerateInterpolator()
                startDelay = delay + riseDuration
            }

            val set = AnimatorSet().apply {
                playTogether(up, down)
                // Repetir el ciclo completo indefinidamente
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (dotAnimators.isNotEmpty()) {
                            // Reiniciar con el mismo delay para mantener la ola
                            val newUp = ObjectAnimator.ofFloat(dot, "translationY", 0f, riseAmount).apply {
                                duration = riseDuration
                                interpolator = DecelerateInterpolator()
                            }
                            val newDown = ObjectAnimator.ofFloat(dot, "translationY", riseAmount, 0f).apply {
                                duration = riseDuration
                                interpolator = DecelerateInterpolator()
                            }
                            val newSet = AnimatorSet()
                            newSet.playSequentially(newUp, newDown)
                            newSet.startDelay = cycleDuration - riseDuration * 2
                            newSet.addListener(this)
                            dotAnimators[index] = newSet
                            newSet.start()
                        }
                    }
                })
            }
            dotAnimators.add(set)
        }

        // Arrancar todos con sus delays ya incorporados
        dotAnimators.forEach { it.start() }
    }

    private fun stopDotsAnimation() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()
        // Resetear posicion de los circulos
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4).forEach {
            it.translationY = 0f
        }
    }

    private fun showResult(annotated: Bitmap) {
        stopDotsAnimation()
        binding.imgResult.setImageBitmap(annotated)
        binding.imgResult.visibility = View.VISIBLE
        binding.imgResult.alpha = 0f
        binding.imgResult.animate()
            .alpha(1f)
            .setDuration(400)
            .withEndAction {
                binding.loadingContainer.visibility = View.GONE
            }
            .start()
    }

    private fun processImage(imageUriString: String) {
        scope.launch {
            try {
                val uri = Uri.parse(imageUriString)

                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream = contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
                }

                val results = withContext(Dispatchers.IO) {
                    val detector = (application as App).foodDetector
                    detector.detect(bitmap)
                }

                val annotated = withContext(Dispatchers.IO) {
                    drawDetections(bitmap, results)
                }

                showResult(annotated)

                if (results.isEmpty()) {
                    binding.tvDetectionsTitle.text = "Sin detecciones"
                    binding.rvDetections.visibility = View.GONE
                } else {
                    binding.tvDetectionsTitle.text = "Detecciones (${results.size})"
                    binding.rvDetections.visibility = View.VISIBLE
                    binding.rvDetections.layoutManager = LinearLayoutManager(this@ResultActivity)
                    binding.rvDetections.adapter = DetectionAdapter(results)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                stopDotsAnimation()
                binding.loadingContainer.visibility = View.GONE
                binding.tvDetectionsTitle.text = "Error al procesar la imagen"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDotsAnimation()
        scope.cancel()
    }
}

class DetectionAdapter(
    private val detections: List<DetectionResult>
) : RecyclerView.Adapter<DetectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tvLabel)
        val tvConfidence: TextView = view.findViewById(R.id.tvConfidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val detection = detections[position]
        holder.tvLabel.text = detection.label.replaceFirstChar { it.uppercase() }
        holder.tvConfidence.text = "%.1f%%".format(detection.confidence * 100)
    }

    override fun getItemCount() = detections.size
}

fun drawDetections(bitmap: Bitmap, detections: List<DetectionResult>): Bitmap {
    val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutable)

    val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#E8FF00")
    }
    val textPaint = Paint().apply {
        color = Color.parseColor("#0D0D0D")
        textSize = 42f
        typeface = Typeface.DEFAULT_BOLD
    }
    val bgPaint = Paint().apply {
        color = Color.parseColor("#E8FF00")
        style = Paint.Style.FILL
    }

    for (det in detections) {
        val left   = det.box.left   * mutable.width
        val top    = det.box.top    * mutable.height
        val right  = det.box.right  * mutable.width
        val bottom = det.box.bottom * mutable.height

        canvas.drawRect(left, top, right, bottom, boxPaint)

        val label = "${det.label.replaceFirstChar { it.uppercase() }} ${"%.1f".format(det.confidence * 100)}%"
        val textWidth = textPaint.measureText(label)
        canvas.drawRect(left, top - 52f, left + textWidth + 12f, top, bgPaint)
        canvas.drawText(label, left + 6f, top - 12f, textPaint)
    }

    return mutable
}