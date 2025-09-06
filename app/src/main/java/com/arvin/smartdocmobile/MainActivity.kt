package com.arvin.smartdocmobile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arvin.smartdocmobile.model.VerifyRequest
import com.arvin.smartdocmobile.net.ApiClient
import com.arvin.smartdocmobile.util.readBitmapFromUri
import com.arvin.smartdocmobile.util.toBase64Jpeg
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private val apiToken = "474edcd0b36805ae411077228da5c3c0"

    private var ivDefaultIcon: ImageView? = null
    private var ivPreview: ImageView? = null
    private var btnTakePhoto: MaterialButton? = null
    private var btnAttachImage: MaterialButton? = null
    private var progressBar: CircularProgressIndicator? = null

    private var dataDisplayCard: MaterialCardView? = null
    private var tilDocumentType: TextInputLayout? = null
    private var etDocumentType: TextInputEditText? = null
    private var tilNicNo: TextInputLayout? = null
    private var etNicNo: TextInputEditText? = null
    private var tieDrivingLicenceNo: TextInputLayout? = null
    private var etDrivingLicenceNo: TextInputEditText? = null
    private var tiePassportNo: TextInputLayout? = null
    private var etPassportNo: TextInputEditText? = null

    private var capturedUri: Uri? = null
    private var originalUri: Uri? = null
    private var croppedUri: Uri? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCapture()
        Toast.makeText(this@MainActivity, "Camera permission denied.", Toast.LENGTH_LONG).show()
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            originalUri = uri
            startCrop(uri)
        }
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && capturedUri != null) {
            startCrop(capturedUri!!)
        } else {
            Toast.makeText(this@MainActivity, "Capture cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val output = UCrop.getOutput(result.data!!)
            if (output != null) {
                croppedUri = output
                ivDefaultIcon?.visibility = View.GONE
                ivPreview?.visibility = View.VISIBLE
                ivPreview?.setImageURI(croppedUri)
                sendApi()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val err = UCrop.getError(result.data!!)
            Toast.makeText(this, "Crop error: ${err?.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initComponents()
    }

    private fun initComponents() {
        ivDefaultIcon = findViewById(R.id.ivDefaultIcon)
        ivPreview = findViewById(R.id.ivPreview)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnAttachImage = findViewById(R.id.btnAttachImage)
        dataDisplayCard = findViewById(R.id.dataDisplayCard)
        tilDocumentType = findViewById(R.id.tilDocumentType)
        etDocumentType = findViewById(R.id.etDocumentType)
        tilNicNo = findViewById(R.id.tilNicNo)
        etNicNo = findViewById(R.id.etNicNo)
        tieDrivingLicenceNo = findViewById(R.id.tieDrivingLicenceNo)
        etDrivingLicenceNo = findViewById(R.id.etDrivingLicenceNo)
        tiePassportNo = findViewById(R.id.tiePassportNo)
        etPassportNo = findViewById(R.id.etPassportNo)
        progressBar = findViewById(R.id.progressBar)

        btnTakePhoto?.setOnClickListener {
            onTakePhotoClicked()
        }

        btnAttachImage?.setOnClickListener {
            onAttachImageClicked()
        }
    }


    private fun sendApi(){
        progressBar?.visibility = View.VISIBLE
        resetDataUI()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bmp = readBitmapFromUri(this@MainActivity, croppedUri!!, maxSide = 1000)
                    val b64 = bmp.toBase64Jpeg(quality = 90)
                    val req = VerifyRequest(img_data = b64, api_token = apiToken)
                    ApiClient.api.verifyImage(req)
                }
            }

            result.onSuccess { res ->
                progressBar?.visibility = View.GONE

                dataDisplayCard?.visibility = View.VISIBLE

                if (res.is_new_nic){
                    tilDocumentType?.visibility = View.VISIBLE
                    etDocumentType?.setText("New NIC")
                } else if (res.is_old_nic) {
                    tilDocumentType?.visibility = View.VISIBLE
                    etDocumentType?.setText("Old NIC")
                } else if (res.is_driving_licence){
                    tilDocumentType?.visibility = View.VISIBLE
                    etDocumentType?.setText("Driving Licence")
                } else if (res.is_passport) {
                    tilDocumentType?.visibility = View.VISIBLE
                    etDocumentType?.setText("Passport")
                } else {
                    tilDocumentType?.visibility = View.VISIBLE
                    etDocumentType?.setText("Invalid Document Type")
                }

                if (res.nic_no?.isNotEmpty() == true){
                    tilNicNo?.visibility = View.VISIBLE
                    etNicNo?.setText(res.nic_no)
                }

                if (res.driving_licence_no?.isNotEmpty() == true){
                    tieDrivingLicenceNo?.visibility = View.VISIBLE
                    etDrivingLicenceNo?.setText(res.driving_licence_no)
                }

                if (res.passport_no?.isNotEmpty() == true){
                    tiePassportNo?.visibility = View.VISIBLE
                    etPassportNo?.setText(res.passport_no)
                }
            }.onFailure { e ->
                progressBar?.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error: ${e.message ?: "Unexpected error"}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetDataUI(){
        tilDocumentType?.visibility = View.GONE
        tilNicNo?.visibility = View.GONE
        tieDrivingLicenceNo?.visibility = View.GONE
        tiePassportNo?.visibility = View.GONE
        dataDisplayCard?.visibility = View.GONE
    }

    private fun startCrop(source: Uri) {
        val destUri = Uri.fromFile(File(cacheDir, "crop_${System.currentTimeMillis()}.jpg"))

        val options = UCrop.Options().apply {
            setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
        }

        val intent = UCrop.of(source, destUri)
            .withOptions(options)
            .getIntent(this)

        cropLauncher.launch(intent)
    }

    private fun onAttachImageClicked() {
        resetDataUI()
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun startCapture() {
        capturedUri = newImageUri()
        takePicture.launch(capturedUri!!)
    }
    private fun newImageUri(): Uri {
        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("capture_", ".jpg", dir)
        val authority = "${packageName}.fileprovider"
        return FileProvider.getUriForFile(this, authority, file)
    }


    private fun onTakePhotoClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCapture()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }


}