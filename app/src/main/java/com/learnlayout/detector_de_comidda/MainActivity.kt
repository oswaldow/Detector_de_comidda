package com.learnlayout.detector_de_comidda

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.learnlayout.detector_de_comidda.databinding.ActivityMainBinding
import com.learnlayout.detector_de_comidda.ui.attachSpring
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPhotoUri: Uri? = null
    private var borderAnimator: ValueAnimator? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            navigateToResult(currentPhotoUri.toString())
        } else {
            Toast.makeText(this, "No se tomo la foto", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            navigateToResult(uri.toString())
        } else {
            Toast.makeText(this, "No se selecciono ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Permiso de camara requerido", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAnalizar.setOnClickListener { showBottomSheet() }

        // Letras del título caen una por una
        binding.tvTitle.animateText(
            label = "Detector\nde Comida",
            textSizeSp = 52f,
            textColorRes = android.graphics.Color.WHITE,
            baseDelayMs = 0L,
            charDelayMs = 60L
        )

        // Animación de entrada escalonada del resto de elementos
        val elements = listOf(
            binding.decorTop,
            binding.titleUnderline,
            binding.btnAnalizar,
            binding.tvHint
        )
        val anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_up)
        elements.forEachIndexed { index, view ->
            view.alpha = 0f
            view.postDelayed({
                view.alpha = 1f
                view.startAnimation(anim)
            }, index * 80L + 600L)
        }

        // Círculo respira
        binding.root.postDelayed({ breatheIn() }, 2000L)

        // Borde animado arranca cuando el botón ya está en su lugar
        binding.root.postDelayed({ startBorderAnimation() }, 1400L)

        binding.btnAnalizar.attachSpring()
    }

    private fun startBorderAnimation() {
        binding.borderLight.animate()
            .alpha(1f)
            .setDuration(400)
            .withEndAction {
                borderAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 2500
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener {
                        binding.borderLight.progress = it.animatedValue as Float
                    }
                    start()
                }
            }
            .start()
    }

    private fun breatheIn() {
        binding.decorCircle.animate()
            .scaleX(1.12f).scaleY(1.12f)
            .alpha(0.12f)
            .setDuration(2200)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction { breatheOut() }
            .start()
    }

    private fun breatheOut() {
        binding.decorCircle.animate()
            .scaleX(1f).scaleY(1f)
            .alpha(0.06f)
            .setDuration(2200)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction { breatheIn() }
            .start()
    }

    private fun showBottomSheet() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDarkTheme)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_options, null)
        dialog.setContentView(sheetView)

        dialog.window?.findViewById<android.view.View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.setBackgroundResource(android.R.color.transparent)

        val optCamera  = sheetView.findViewById<android.view.View>(R.id.optCamera)
        val optGallery = sheetView.findViewById<android.view.View>(R.id.optGallery)

        optCamera.setOnClickListener  { dialog.dismiss(); checkCameraPermissionAndLaunch() }
        optGallery.setOnClickListener { dialog.dismiss(); galleryLauncher.launch("image/*") }

        optCamera.attachSpring()
        optGallery.attachSpring()

        dialog.show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val file = File(cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        val uri  = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        currentPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    private fun navigateToResult(uriString: String) {
        startActivity(Intent(this, ResultActivity::class.java).apply {
            putExtra("image_uri", uriString)
        })
        overridePendingTransition(R.anim.slide_up_enter, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.decorCircle.animate().cancel()
        borderAnimator?.cancel()
    }
}