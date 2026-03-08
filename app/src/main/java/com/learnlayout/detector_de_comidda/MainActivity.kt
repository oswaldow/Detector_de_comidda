package com.learnlayout.detector_de_comidda

import android.Manifest
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

        binding.btnAnalizar.setOnClickListener {
            showBottomSheet()
        }

        // Animación de entrada escalonada
        val elements = listOf(
            binding.decorTop,
            binding.tvTitle,
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
            }, index * 80L)
        }

        // Animación ambient arranca después de la entrada
        binding.root.postDelayed({ breatheIn() }, elements.size * 80L + 500L)

        binding.btnAnalizar.attachSpring()
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

        val optCamera = sheetView.findViewById<android.view.View>(R.id.optCamera)
        val optGallery = sheetView.findViewById<android.view.View>(R.id.optGallery)

        optCamera.setOnClickListener {
            dialog.dismiss()
            checkCameraPermissionAndLaunch()
        }
        optGallery.setOnClickListener {
            dialog.dismiss()
            galleryLauncher.launch("image/*")
        }

        optCamera.attachSpring()
        optGallery.attachSpring()

        dialog.show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val file = File(cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
        currentPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    private fun navigateToResult(uriString: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("image_uri", uriString)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.decorCircle.animate().cancel()
    }
}