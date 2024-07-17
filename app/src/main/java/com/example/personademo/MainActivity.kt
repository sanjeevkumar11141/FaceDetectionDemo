package com.example.personademo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var instructionText: TextView
    private lateinit var countdownTimer: TextView
    private lateinit var buttonStartCapture: Button
    private var imageCapture: ImageCapture? = null
    private lateinit var faceDetector: FaceDetector
    private lateinit var outputDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        instructionText = findViewById(R.id.instructionText)
        countdownTimer = findViewById(R.id.countdownTimer)
        buttonStartCapture = findViewById(R.id.buttonStartCapture)
        outputDirectory = getOutputDirectory()

        buttonStartCapture.setOnClickListener {
            captureMultiAnglePhotos()
           // dispatchToCustomizedCameraScreen()
        }

        startCamera()
        setupFaceDetector()
    }

     fun dispatchToCustomizedCameraScreen(){
         var intent = Intent(this, CameraCustomizedViewActivity::class.java)
         startActivity(intent)
     }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupFaceDetector() {
        faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }

    private fun takePhoto(onImageCaptured: (File) -> Unit) {
        val imageCapture = imageCapture ?: return

        val photoFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onImageCaptured(photoFile)
                }
            })
    }

    private fun detectFaces(image: InputImage, onFaceDetected: (List<Face>) -> Unit) {
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                onFaceDetected(faces)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun captureMultiAnglePhotos() {
        captureFrontPhoto()
    }

    private fun captureFrontPhoto() {
        instructionText.text = "Look straight ahead"
        startCountdown {
            takePhoto { photoFile ->
                val image = InputImage.fromFilePath(this, Uri.fromFile(photoFile))
                detectFaces(image) { faces ->
                    if (faces.isNotEmpty()) {
                        Log.d(TAG, "Front face detected.")
                        captureLeftPhoto()
                    } else {
                        showToast("Front face not detected, please try again.")
                        captureFrontPhoto() // Retry capturing front face
                    }
                }
            }
        }
    }

    private fun captureLeftPhoto() {
        instructionText.text = "Look slightly left"
        startCountdown {
            takePhoto { photoFile ->
                val image = InputImage.fromFilePath(this, Uri.fromFile(photoFile))
                detectFaces(image) { faces ->
                    val face = faces.firstOrNull()
                    if (face != null) {
                        val headEulerAngleY = face.headEulerAngleY
                        Log.d(TAG, "Left face detected with angle: $headEulerAngleY")
                        if (headEulerAngleY > 30) {
                            captureRightPhoto()
                        } else {
                            showToast("Left face not detected correctly, please try again.")
                            captureLeftPhoto() // Retry capturing left face
                        }
                    } else {
                        showToast("Left face not detected, please try again.")
                        captureLeftPhoto() // Retry capturing left face
                    }
                }
            }
        }
    }

    private fun captureRightPhoto() {
        instructionText.text = "Look slightly right"
        startCountdown {
            takePhoto { photoFile ->
                val image = InputImage.fromFilePath(this, Uri.fromFile(photoFile))
                detectFaces(image) { faces ->
                    val face = faces.firstOrNull()
                    if (face != null) {
                        val headEulerAngleY = face.headEulerAngleY
                        Log.d(TAG, "Right face detected with angle: $headEulerAngleY")
                        if (headEulerAngleY < -30) {
                            showToast("Capture completed.")
                            instructionText.text = "Capture completed."
                        } else {
                            showToast("Right face not detected correctly, please try again.")
                            captureRightPhoto() // Retry capturing right face
                        }
                    } else {
                        showToast("Right face not detected, please try again.")
                        captureRightPhoto() // Retry capturing right face
                    }
                }
            }
        }
    }

    private fun startCountdown(onCountdownFinished: () -> Unit) {
        countdownTimer.visibility = View.VISIBLE
        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownTimer.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                countdownTimer.visibility = View.GONE
                onCountdownFinished()
            }
        }.start()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
