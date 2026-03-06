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
    }

    private fun showBottomSheet() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDarkTheme)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_options, null)
        dialog.setContentView(sheetView)

        // Fondo transparente para que se vea el background del layout
        dialog.window?.findViewById<android.view.View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.setBackgroundResource(android.R.color.transparent)

        sheetView.findViewById<android.view.View>(R.id.optCamera).setOnClickListener {
            dialog.dismiss()
            checkCameraPermissionAndLaunch()
        }

        sheetView.findViewById<android.view.View>(R.id.optGallery).setOnClickListener {
            dialog.dismiss()
            galleryLauncher.launch("image/*")
        }

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
}