package com.app.magnifier.magnifyingglass.viewmodel

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.ScaleGestureDetector
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.ViewModel
import com.app.magnifier.magnifyingglass.utils.Constants
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class CameraViewModel : ViewModel() {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    lateinit var camera: androidx.camera.core.Camera
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val _zoomState = MutableLiveData<ZoomState>()
    val zoomState: LiveData<ZoomState> get() = _zoomState
    private val _flashState = MutableLiveData<Boolean>(false)
    val flashState: LiveData<Boolean> get() = _flashState
    private val _exposureIndex = MutableLiveData<Int>()
    val exposureIndex: LiveData<Int> get() = _exposureIndex
    private val _photoSavedUri = MutableLiveData<Uri?>()
    val photoSavedUri: LiveData<Uri?> get() = _photoSavedUri

    // TODO: Start Camera
    fun startCamera(context: Context, previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Using "also" => To ensure camera preview is modified before assigning to preview xml
            val preview = Preview.Builder().build().also { mPreview ->
                mPreview.surfaceProvider = previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()

            setUpCamera(context, cameraProvider, preview, lifecycleOwner)

            observeCameraZoom()

        }, ContextCompat.getMainExecutor(context))
    }

    private fun setUpCamera(context: Context, cameraProvider: ProcessCameraProvider, preview: Preview, lifecycleOwner: LifecycleOwner) {
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()      // Hủy các camera previous
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            camera.cameraControl.setZoomRatio(3.0f).addListener({
                Log.d(Constants.TAG, camera.cameraInfo.zoomState.value.toString())
            }, ContextCompat.getMainExecutor(context))     // Set initial zoom ratio
        } catch (e: Exception) {
            Log.d(Constants.TAG, "startCamera failed: ${e.message}")
        }
    }
    // ------------------------------------------------------------------------


    // TODO: Zooming Camera
    // 2.1 Zooming by buttons
    fun adjustZoom (isZoomIn : Boolean) {
        val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f

        val newZoomRatio =
            if (isZoomIn) {
                currentZoomRatio + 1f
            }
            else {
                currentZoomRatio - 1f
            }

        camera.cameraControl.setZoomRatio(newZoomRatio)
    }

    //2.2 Zooming by pinch
    @SuppressLint("ClickableViewAccessibility")
    fun zoomCameraPinch(context: Context, previewView: PreviewView) {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                camera.cameraControl.setZoomRatio(scale * delta)
                return true
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(context, listener)

        previewView.setOnTouchListener { _, motionEvent ->
            scaleGestureDetector.onTouchEvent(motionEvent)
            return@setOnTouchListener true
        }
    }

    private fun observeCameraZoom() {
        camera.cameraInfo.zoomState.observeForever { zoomState ->
            _zoomState.value = zoomState
        }
    }
    // ------------------------------------------------------------------------


    // TODO: Flashlight
    fun toggleFlash() {
        val newState = !(_flashState.value ?: false)
        _flashState.value = newState
        camera.cameraControl.enableTorch(newState)
    }


    // TODO: Change the Camera Exposure
    fun changeExposure(progress : Int) {
        val exposureState = camera.cameraInfo.exposureState
        val min = exposureState.exposureCompensationRange.lower
        val max = exposureState.exposureCompensationRange.upper

        val exposure = (((progress - 50) / 100.0f) * (max - min)).coerceIn(min.toFloat(), max.toFloat())

        _exposureIndex.value = exposure.toInt()
        camera.cameraControl.setExposureCompensationIndex(exposure.toInt())
    }


    // TODO: Release camera
    fun releaseCamera() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
        }
    }

    // TODO: Take picture
    /* Method: If Android 10 and later: Using MediaStore API
     *         If Android 9 or earlier: Using Access API
     */
    @SuppressLint("WeekBasedYear")
    fun savePicture(bitmap: Bitmap, context: Context) : Boolean {
        val fileName = SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Magnifier")
            }
        }

        val outputStream: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri == null) {
                Log.e(Constants.TAG, "Failed to create new MediaStore record.")
                return false
            }

            outputStream = contentResolver.openOutputStream(uri)
        } else {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val subDir = File(directory, "Magnifier")
            if (!subDir.exists()) {
                subDir.mkdirs()
            }
            val file = File(subDir, fileName)
            outputStream = FileOutputStream(file)

            // Update MediaStore for Android 9 and below
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg")
            ) { path, uri ->
                Log.d("Gallery Update", "Scanned $path -> URI = $uri")
            }
        }

        outputStream?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            it.flush()
        }

        return true
    }
}