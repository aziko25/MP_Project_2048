package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceDetector(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onDirectionDetected: (Direction) -> Unit
) {
    private var cameraExecutor: ExecutorService? = null
    private val movementThreshold = 10f
    private var lastDetectionTime = 0L
    private val detectionCooldown = 500L // milliseconds
    private lateinit var detector: com.google.mlkit.vision.face.FaceDetector

    fun start(previewView: PreviewView) {
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize ML Kit face detector once
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(options)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(previewView.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("FaceDetector", "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private var lastHeadPositionX = 0f
    private var lastHeadPositionY = 0f

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image != null) {
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    faces.firstOrNull()?.getLandmark(FaceLandmark.NOSE_BASE)?.let { nose ->
                        val currentX = nose.position.x
                        val currentY = nose.position.y
                        val movementX = currentX - lastHeadPositionX
                        val movementY = currentY - lastHeadPositionY
                        val currentTime = System.currentTimeMillis()

                        if (kotlin.math.abs(movementX) > movementThreshold &&
                            currentTime - lastDetectionTime > detectionCooldown
                        ) {
                            val direction = if (movementX > 0) Direction.LEFT else Direction.RIGHT
                            onDirectionDetected(direction)
                            lastDetectionTime = currentTime
                        } else if (kotlin.math.abs(movementY) > movementThreshold &&
                            currentTime - lastDetectionTime > detectionCooldown
                        ) {
                            val direction = if (movementY > 0) Direction.DOWN else Direction.UP
                            onDirectionDetected(direction)
                            lastDetectionTime = currentTime
                        }

                        lastHeadPositionX = currentX
                        lastHeadPositionY = currentY
                    }
                }
                .addOnFailureListener {
                    Log.e("FaceDetector", "Face detection failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun stop() {
        cameraExecutor?.shutdown()
        cameraExecutor = null
        detector.close()
    }
}