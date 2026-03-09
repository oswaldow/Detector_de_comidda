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
import com.learnlayout.detector_de_comidda.ui.attachSpring
import kotlinx.coroutines.*

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val dotAnimators = mutableListOf<AnimatorSet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, R.anim.slide_down_exit)
        }
        binding.btnBack.attachSpring()

        startDotsAnimation()
        processImage(intent.getStringExtra("image_uri") ?: "")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_down_exit)
    }

    private fun startDotsAnimation() {
        val dots         = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        val riseAmount   = -22f
        val riseDuration = 300L
        val stepDelay    = 150L

        dots.forEachIndexed { index, dot ->
            val delay         = index * stepDelay
            val cycleDuration = dots.size * stepDelay + riseDuration * 2

            val up = ObjectAnimator.ofFloat(dot, "translationY", 0f, riseAmount).apply {
                duration = riseDuration; interpolator = DecelerateInterpolator(); startDelay = delay }
            val down = ObjectAnimator.ofFloat(dot, "translationY", riseAmount, 0f).apply {
                duration = riseDuration; interpolator = DecelerateInterpolator(); startDelay = delay + riseDuration }

            val set = AnimatorSet().apply {
                playTogether(up, down)
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (dotAnimators.isNotEmpty()) {
                            val nUp = ObjectAnimator.ofFloat(dot, "translationY", 0f, riseAmount).apply {
                                duration = riseDuration; interpolator = DecelerateInterpolator() }
                            val nDown = ObjectAnimator.ofFloat(dot, "translationY", riseAmount, 0f).apply {
                                duration = riseDuration; interpolator = DecelerateInterpolator() }
                            val nSet = AnimatorSet()
                            nSet.playSequentially(nUp, nDown)
                            nSet.startDelay = cycleDuration - riseDuration * 2
                            nSet.addListener(this)
                            dotAnimators[index] = nSet
                            nSet.start()
                        }
                    }
                })
            }
            dotAnimators.add(set)
        }
        dotAnimators.forEach { it.start() }

        dots.forEachIndexed { i, dot ->
            dot.postDelayed({
                dot.animate().alpha(0.3f).setDuration(400)
                    .withEndAction { dot.animate().alpha(1f).setDuration(400).start() }.start()
            }, i * 150L)
        }
    }

    private fun stopDotsAnimation() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
            .forEach { it.translationY = 0f }
    }

    private fun showResult(bitmap: Bitmap, results: List<DetectionResult>) {
        stopDotsAnimation()

        // Mostrar imagen limpia (sin anotaciones)
        binding.imgResult.setImageBitmap(bitmap)
        binding.imgResult.visibility = View.VISIBLE
        binding.imgResult.alpha = 0f
        binding.imgResult.animate().alpha(1f).setDuration(400)
            .withEndAction {
                binding.loadingContainer.visibility = View.GONE

                // Calcular el rect exacto donde fitCenter dibuja la imagen
                val imageRect = getImageRect(bitmap)
                binding.detectionOverlay.setImageRect(imageRect)
                binding.detectionOverlay.visibility = View.VISIBLE

                // Disparar animación escalonada de boxes
                if (results.isNotEmpty()) {
                    binding.detectionOverlay.showDetections(results)
                }
            }.start()
    }

    // Calcula el RectF donde fitCenter posiciona la imagen dentro del ImageView
    private fun getImageRect(bitmap: Bitmap): RectF {
        val vw = binding.imgResult.width.toFloat()
        val vh = binding.imgResult.height.toFloat()
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()

        val scale = minOf(vw / bw, vh / bh)
        val scaledW = bw * scale
        val scaledH = bh * scale
        val left = (vw - scaledW) / 2f
        val top  = (vh - scaledH) / 2f

        return RectF(left, top, left + scaledW, top + scaledH)
    }

    private fun processImage(imageUriString: String) {
        scope.launch {
            try {
                val uri    = Uri.parse(imageUriString)
                val bitmap = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
                }
                val results = withContext(Dispatchers.IO) {
                    (application as App).foodDetector.detect(bitmap)
                }

                showResult(bitmap, results)

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

// ── Adapter ──────────────────────────────────────────────────────────────────

class DetectionAdapter(
    private val detections: List<DetectionResult>
) : RecyclerView.Adapter<DetectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIndex:      TextView = view.findViewById(R.id.tvIndex)
        val tvLabel:      TextView = view.findViewById(R.id.tvLabel)
        val tvConfidence: TextView = view.findViewById(R.id.tvConfidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_detection, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val d = detections[position]
        holder.tvIndex.text      = "${position + 1}"
        holder.tvLabel.text      = d.label.replaceFirstChar { it.uppercase() }
        holder.tvConfidence.text = "%.1f%%".format(d.confidence * 100)
    }

    override fun getItemCount() = detections.size
}