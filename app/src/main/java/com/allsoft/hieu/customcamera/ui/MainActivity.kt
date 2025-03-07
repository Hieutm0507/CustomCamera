package com.allsoft.hieu.customcamera.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.allsoft.hieu.customcamera.R
import com.allsoft.hieu.customcamera.databinding.ActivityMainBinding
import com.allsoft.hieu.customcamera.utils.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: androidx.camera.core.Camera
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var frontCamState : Boolean = false
    private var flashState: Boolean = false
    private lateinit var cameraManager : CameraManager
    private lateinit var cameraId : String


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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        zoomCamera()

        openFlashLight()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        // Đóng cameraProvider
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }



    private fun openFlashLight() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]
        val isFlashAvailable = applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        if (isFlashAvailable) {
            binding.ibFlash.setOnClickListener {
                flashState = !flashState
                try {
                    cameraManager.setTorchMode(cameraId, flashState)
                }
                catch (e : CameraAccessException) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.d(Constants.TAG_FLASH, "Error" + e.message)
                }
            }
        }
        else {
            Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show()
        }

//        binding.ibFlash.setOnClickListener {
//
//            cameraManager.setTorchMode(cameraId, flashState)
//            if (!flashState) {
//                binding.ibFlash.setImageResource(R.drawable.ic_flash_on)
//                flashState = true
//            }
//            else {
//                binding.ibFlash.setImageResource(R.drawable.ic_flash_off)
//                flashState = false
//            }
//        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun zoomCamera() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = camera.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                camera.cameraControl.setZoomRatio(scale)
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
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { mPreview ->
                mPreview.surfaceProvider = binding.pvView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            setUpCamera(cameraProvider, preview)

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
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
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