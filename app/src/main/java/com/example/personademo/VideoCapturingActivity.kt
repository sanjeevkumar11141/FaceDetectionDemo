package com.example.personademo
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.personademo.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class VideoCapturingActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var instructionText: TextView
    private lateinit var countdownTimer: TextView
    private lateinit var buttonStartCapture: Button
    private var imageCapture: ImageCapture? = null
    @SuppressLint("RestrictedApi")
    private var videoCapture: VideoCapture? = null
    private lateinit var faceDetector: FaceDetector
    private lateinit var outputDirectory: File
    private var videoFile: File? = null
    private var allAnglesDetected = false

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                checkStoragePermissions()
            } else {
                showToast("Camera permission is required")
            }
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.all { it.value }
            if (allPermissionsGranted) {
                startCamera()
            } else {
                showToast("Storage permissions are required")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_capturing)

        viewFinder = findViewById(R.id.viewFinder)
        instructionText = findViewById(R.id.instructionText)
        countdownTimer = findViewById(R.id.countdownTimer)
        buttonStartCapture = findViewById(R.id.buttonStartCapture)
        outputDirectory = getOutputDirectory()

        buttonStartCapture.setOnClickListener {
            if (checkPermissions()) {
                captureVideoWithFaceDetection()
            }
        }

        checkPermissions()
        setupFaceDetector()
    }

    private fun checkPermissions(): Boolean {
        return when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                checkStoragePermissions()
                true
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                false
            }
        }
    }

    private fun checkStoragePermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            storagePermissionLauncher.launch(permissions)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            videoCapture = VideoCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )
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

    @SuppressLint("MissingPermission", "RestrictedApi")
    private fun captureVideoWithFaceDetection() {
        allAnglesDetected = false
        val videoFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".mp4")

        val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()

        videoCapture?.startRecording(outputOptions, ContextCompat.getMainExecutor(this), object : VideoCapture.OnVideoSavedCallback {
            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                this@VideoCapturingActivity.videoFile = videoFile
                startCountdown({
                    stopVideoCapture()
                    processVideoForFaceDetection(videoFile)
                })
            }

            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                Log.e(TAG, "Video capture failed: $message", cause)
                showToast("Video capture failed")
            }
        })

        startCountdown({
            stopVideoCapture()
            if (!allAnglesDetected) {
                showToast("Video did not capture all angles.")
            }
        }, 20000) // 20 seconds countdown
    }

    @SuppressLint("RestrictedApi")
    private fun stopVideoCapture() {
        videoCapture?.stopRecording()
    }

    private fun processVideoForFaceDetection(videoFile: File) {
        // This function would need to process the video to extract frames and detect faces
        // Implement video frame extraction and face detection here
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

    private fun startCountdown(onCountdownFinished: () -> Unit, duration: Long = 5000) {
        countdownTimer.visibility = View.VISIBLE
        object : CountDownTimer(duration, 1000) {
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
