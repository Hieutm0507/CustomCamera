package com.allsoft.hieu.customcamera.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
    private var frontCamState : Boolean = false
    private var flashState: Boolean = false
    private lateinit var cameraProvider: ProcessCameraProvider
    private var isPaused : Boolean = false


    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        }
        else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            finish()        // Đóng ứng dụng khi quyền ko được cấp
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        if (allPermissionGranted()) {
            startCamera()
        }
        else {
            launcher.launch(Constants.REQUIRED_PERMISSIONS)
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.ivTakePic.setOnClickListener {
            takePhoto()
        }

        zoomCameraPinch()
        openFlashLight()
        changeExposure()

        binding.ibFreezeImg.setOnClickListener {
            togglePause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        // Đóng cameraProvider
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
    private fun togglePause() {
        isPaused = !isPaused

        if (isPaused) {
            binding.pvView.bitmap?.let { bitmap ->
                binding.ivFreezingImg.setImageBitmap(bitmap)
                binding.ivFreezingImg.visibility = View.VISIBLE
            }
        }
        else {
            binding.ivFreezingImg.visibility = View.INVISIBLE
        }

        binding.ibFreezeImg.setImageResource(
            if (isPaused) R.drawable.ic_resume
            else R.drawable.ic_pause)
    }

    // TODO: Open Flashlight and change icon
    private fun openFlashLight() {
        binding.ibFlash.setOnClickListener {
            flashState = !flashState
            camera.cameraControl.enableTorch(flashState)

            if (flashState) {
                binding.ibFlash.setImageResource(R.drawable.ic_flash_on)
            }
            else binding.ibFlash.setImageResource(R.drawable.ic_flash_off)
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


    // TODO: Zoom Camera by Seekbar
//    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("DefaultLocale")
    private fun zoomCameraSeekbar() {
        camera.cameraInfo.zoomState.observe(this) { state ->
            binding.sbZooming.max = (state.maxZoomRatio * 10).toInt()
            // binding.sbZooming.min = (state.minZoomRatio * 10).toInt()
            binding.tvZoomLevel.text = String.format("%.1fx", state.zoomRatio)
            binding.sbZooming.progress = (state.zoomRatio * 10).toInt()
        }

        binding.sbZooming.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    camera.cameraControl.setZoomRatio(progress / 10f)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
    }


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


    // TODO: Take picture and save it
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

            zoomCameraSeekbar()

            binding.ibChangeCamera.setOnClickListener {
                frontCamState = !frontCamState
                setUpCamera(cameraProvider, preview)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setUpCamera(cameraProvider: ProcessCameraProvider, preview: Preview) {
        cameraSelector = if (frontCamState) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            cameraProvider.unbindAll()      // Hủy các camera previous
            camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageCapture)
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