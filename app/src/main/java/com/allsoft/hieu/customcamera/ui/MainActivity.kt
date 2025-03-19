package com.allsoft.hieu.customcamera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import com.allsoft.hieu.customcamera.R
import com.allsoft.hieu.customcamera.databinding.ActivityMainBinding
import com.allsoft.hieu.customcamera.utils.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: androidx.camera.core.Camera
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var flashState: Boolean = false
    private lateinit var cameraProvider: ProcessCameraProvider
    private var isPaused : Boolean = false


    // TODO: ASK PERMISSIONS using ActivityResultLauncher
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        }
        else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            finish()        // Close app when permission is not granted
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(Manifest.permission.CAMERA)

        // Thêm quyền Storage dựa trên phiên bản Android
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 and previous
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            // Android 11 and 12
            requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 13 and later
            requiredPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            startCamera()
        } else {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        checkAndRequestPermissions()

        if (allPermissionGranted()) {
            startCamera()
        }
        else {
            launcher.launch(Constants.REQUIRED_PERMISSIONS)
        }

//        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.ibCamera.setOnClickListener {
//            takePhoto()
            takePicture()
        }

        zoomCameraPinch()

        binding.ibZoomIn.setOnClickListener {
            adjustZoom(true)
        }

        binding.ibZoomOut.setOnClickListener {
            adjustZoom(false)
        }

        openFlashLight()
        changeExposure()

        binding.ibSetting.setOnClickListener {
            val ft : FragmentTransaction = supportFragmentManager.beginTransaction()
            ft.replace(R.id.fl_display_fragment, SettingFragment())
            ft.addToBackStack(null)
            ft.commit()
        }

//        binding.ibFreezeImg.setOnClickListener {
//            togglePause()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        // Close cameraProvider
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    override fun onPause() {
        super.onPause()
        cameraProvider.unbindAll()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionGranted()) {
            startCamera()
        }
    }

    // TODO: Freeze the image
//    private fun togglePause() {
//        isPaused = !isPaused
//
//        if (isPaused) {
//            binding.pvView.bitmap?.let { bitmap ->
//                binding.ivFreezingImg.setImageBitmap(bitmap)
//                binding.ivFreezingImg.visibility = View.VISIBLE
//            }
//        }
//        else {
//            binding.ivFreezingImg.visibility = View.INVISIBLE
//        }
//
//        binding.ibFreezeImg.setImageResource(
//            if (isPaused) R.drawable.ic_resume
//            else R.drawable.ic_pause)
//    }

    // TODO: Open Flashlight and change icon
    private fun openFlashLight() {
        binding.ibFlash.setOnClickListener {
            flashState = !flashState
            camera.cameraControl.enableTorch(flashState)

            val background = ContextCompat.getDrawable(this, R.drawable.bg_buttons)
            val iconColor = ContextCompat.getDrawable(this, R.drawable.ic_flash)

            if (flashState) {
                background?.setTint(Color.parseColor("#129CFF"))
                iconColor?.setTint(Color.parseColor("#FFFFFF"))
            }
            else {
                background?.setTint(Color.parseColor("#FFFFFF"))
                iconColor?.setTint(Color.parseColor("#000000"))
            }

            binding.ibFlash.background = background
            binding.ibFlash.setImageDrawable(iconColor)
        }
    }


    // TODO: Change the Camera Exposure
    private fun changeExposure() {
        binding.sbBrightness.progress = 50

        binding.sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val exposure = (progress - 50) / 25.0f
                    camera.cameraControl.setExposureCompensationIndex(exposure.toInt())
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) { }

            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })
    }


    // TODO: Zooming Camera
    @SuppressLint("ClickableViewAccessibility")
    private fun zoomCameraPinch() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                camera.cameraControl.setZoomRatio(scale * delta)
                return true
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(this, listener)

        binding.pvView.setOnTouchListener { _, motionEvent ->
            scaleGestureDetector.onTouchEvent(motionEvent)
            return@setOnTouchListener true
        }
    }

    private fun adjustZoom (isZoomIn : Boolean) {
        val cameraInfo = camera.cameraInfo
        val currentZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 1f

        val newZoomRatio =
            if (isZoomIn) {
                currentZoomRatio + 1f
            }
            else {
                currentZoomRatio - 1f
            }

        camera.cameraControl.setZoomRatio(newZoomRatio)
    }

    @SuppressLint("DefaultLocale")
    private fun observeZoom() {
        if (!::camera.isInitialized) return     //Do nothing when camera is not initialized

        camera.cameraInfo.zoomState.observe(this) { zoomState ->
            val zoomRatio = zoomState.zoomRatio

            // 1. Displaying TextView Zoom Ratio
            binding.tvZoomRatio.text = String.format(Locale.US, "%.1fx", zoomRatio)

            binding.tvZoomRatio.visibility = View.VISIBLE

            // Hide the TextView after 23
            Handler(Looper.getMainLooper()).postDelayed({
                binding.tvZoomRatio.visibility = View.GONE
            }, 3000)


            // 2. Change color of buttons' icon
            val iconZIColor = ContextCompat.getDrawable(this, R.drawable.ic_plus)
            val iconZOColor = ContextCompat.getDrawable(this, R.drawable.ic_minus)

            when (zoomRatio) {
                camera.cameraInfo.zoomState.value?.maxZoomRatio -> {
                    iconZIColor?.setTint(Color.parseColor("#979797"))
                }
                1.0f -> {
                    iconZOColor?.setTint(Color.parseColor("#979797"))
                }
                else -> {
                    iconZIColor?.setTint(Color.parseColor("#000000"))
                    iconZOColor?.setTint(Color.parseColor("#000000"))
                }
            }

            binding.ibZoomIn.setImageDrawable(iconZIColor)
            binding.ibZoomOut.setImageDrawable(iconZOColor)
        }
    }
    

    // TODO: Take picture and save it
    /* Method: If Android 10 and later: Using MediaStore API
     *         If Android 9 or earlier: Using Access API
     */

    @SuppressLint("WeekBasedYear")
    private fun takePicture() {
        val imageCapture = imageCapture
        val fileName = SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Magnifier")
            }
        }

        val outputOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = applicationContext.contentResolver
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri == null) {
                Log.e(Constants.TAG, "Failed to create new MediaStore record.")
                return
            }
            ImageCapture.OutputFileOptions.Builder(contentResolver, uri, contentValues).build()
        } else {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val subDir = File(directory, "Magnifier")
            if (!subDir.exists()) {
                subDir.mkdirs()
            }
            val file = File(subDir, fileName)
            ImageCapture.OutputFileOptions.Builder(file).build()
        }

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    val msg = "Photo Saved"
                    Toast.makeText(this@MainActivity, "$msg $savedUri", Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }
            }
        )
    }



    private fun getOutputDirectory() : File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    @SuppressLint("WeekBasedYear")
    private fun takePhoto() {
        val photoFile = File(outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis()) + ".jpg")

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()


        // takePicture(outputFileOption, executor, object : ImageCapture.OnImageSavedCallback)
        // Ảnh lưu trong thư mục Phone/Android/media/
        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo Saved"

                    Toast.makeText(this@MainActivity, "$msg $savedUri", Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }
            }
        )
    }


    // TODO: Start Camera
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Using "also" => To ensure camera preview is modified before assigning to preview xml
            val preview = Preview.Builder().build().also { mPreview ->
                mPreview.surfaceProvider = binding.pvView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()

            setUpCamera(cameraProvider, preview)

            observeZoom()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setUpCamera(cameraProvider: ProcessCameraProvider, preview: Preview) {
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()      // Hủy các camera previous
            camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageCapture)
            camera.cameraControl.setZoomRatio(3.0f)     // Set initial zoom ratio
        } catch (e: Exception) {
            Log.d(Constants.TAG, "startCamera failed: ${e.message}")
        }
    }


    // TODO: Verify Permissions
    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
}