package com.allsoft.hieu.customcamera.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import com.allsoft.hieu.customcamera.R
import com.allsoft.hieu.customcamera.databinding.ActivityMainBinding
import com.allsoft.hieu.customcamera.utils.Constants
import com.allsoft.hieu.customcamera.viewmodel.CameraViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private val cameraViewModel : CameraViewModel by viewModel()
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var isPaused : Boolean = false
    private lateinit var sharedPreferences: SharedPreferences
    private var isFirstTime : Boolean = true
    private var isFirstCap : Boolean = true


    // TODO: ASK PERMISSIONS using ActivityResultLauncher
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            cameraViewModel.startCamera(this, binding.pvView, this as LifecycleOwner)
        }
        else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(Manifest.permission.CAMERA)

        requiredPermissions.add(Manifest.permission.CAMERA)

        // Add permission for Storage based on Android's version
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

        if (permissionsToRequest.isNotEmpty()) {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // TODO: Splash Screen
        Handler(Looper.getMainLooper()).postDelayed(
            {
                binding.clSplashScreen.visibility = View.GONE

                if (allPermissionGranted()) {
                    cameraViewModel.startCamera(this, binding.pvView, this as LifecycleOwner)
                }
                else {
                    displayPermissionDialog()
                }
            },
            Constants.LOADING_TIME)

        val animator = ObjectAnimator.ofInt(binding.pbLoading, "progress", 0, 100)
        animator.duration = Constants.LOADING_TIME
        animator.start()
        //-------------------------------


        // TODO: Direct to Instruction Fragment
        callData()
        if (isFirstTime) {
            goToInstruction()
            saveData()
        }
        //-------------------------------


        cameraExecutor = Executors.newSingleThreadExecutor()

        observeZoom()

        cameraViewModel.zoomCameraPinch(this, binding.pvView)

        binding.ibZoomIn.setOnClickListener {
            cameraViewModel.adjustZoom(true)
        }

        binding.ibZoomOut.setOnClickListener {
            cameraViewModel.adjustZoom(false)
        }

        openFlashLight()
        changeExposure()

        binding.ibSetting.setOnClickListener {
            val ft : FragmentTransaction = supportFragmentManager.beginTransaction()
            ft.replace(R.id.fl_display_fragment, SettingFragment())
            ft.addToBackStack(null)
            ft.commit()
        }

        binding.ibCamera.setOnClickListener {
            togglePause()

            if (isFirstCap) {
                goToCapInstruct()
                saveData()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        // Close cameraProvider
        cameraViewModel.releaseCamera()
    }

    override fun onPause() {
        super.onPause()
        cameraViewModel.releaseCamera()
        saveData()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionGranted()) {
            cameraViewModel.startCamera(this, binding.pvView, this as LifecycleOwner)
        }
        callData()
    }

    private fun saveData() {
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        Log.d("TAG_P_SAVE", isFirstTime.toString())
        editor.putBoolean("isFirstTime", isFirstTime)
        Log.d("TAG_P_SAVE", isFirstCap.toString())
        editor.putBoolean("isFirstCap", isFirstCap)
        editor.apply()
    }

    private fun callData() {
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE, MODE_PRIVATE)
        isFirstTime = sharedPreferences.getBoolean("isFirstTime", true)
        Log.d("TAG_P_CALL", isFirstTime.toString())
        isFirstCap = sharedPreferences.getBoolean("isFirstCap", true)
        Log.d("TAG_P_CALL", isFirstCap.toString())
    }

    private fun goToInstruction() {
        val ft : FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fl_display_fragment, GuidelineFragment())
        ft.addToBackStack(null)
        ft.commit()

        isFirstTime = false
    }

    private fun goToCapInstruct() {
        val ft : FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.add(R.id.fl_display_fragment, GuidelineCaptureFragment())
        ft.addToBackStack(null)
        ft.commit()

        isFirstCap = false
    }

    // TODO: Freeze the image
    private fun togglePause() {
        isPaused = !isPaused

        if (isPaused) {
            binding.pvView.bitmap?.let { bitmap ->
                val saveImg = cameraViewModel.savePicture(bitmap, this)

                if (saveImg) {
                    showSuccessToast(this)
                }

                // Display Image
                val ft : FragmentTransaction = supportFragmentManager.beginTransaction()
                val fragment = CaptureFragment()

                val bundle = Bundle()
                bundle.putParcelable("bitmap", bitmap)
                fragment.arguments = bundle
                ft.add(R.id.fl_display_fragment, fragment)
                ft.addToBackStack(null)
                ft.commit()
            }
        }
    }


    // TODO: Open Flashlight and change icon
    private fun openFlashLight() {
        binding.ibFlash.setOnClickListener {
            cameraViewModel.toggleFlash()
        }

        cameraViewModel.flashState.observe(this) { flashState ->
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
                    cameraViewModel.changeExposure(progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) { }

            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })
    }


    // TODO: Zooming Camera
    @SuppressLint("DefaultLocale")
    private fun observeZoom() {
        cameraViewModel.zoomState.observe(this) { zoomState ->
            val zoomRatio = zoomState.zoomRatio

            // 1. Displaying TextView Zoom Ratio
            binding.tvZoomRatio.text = String.format(Locale.US, "%.1fx", zoomRatio)

            binding.tvZoomRatio.visibility = View.VISIBLE

            // Hide the TextView after 3 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                binding.tvZoomRatio.visibility = View.GONE
            }, 3000)


            // 2. Change color of buttons' icon
            val iconZIColor = ContextCompat.getDrawable(this, R.drawable.ic_plus)
            val iconZOColor = ContextCompat.getDrawable(this, R.drawable.ic_minus)

            when (zoomRatio) {
                cameraViewModel.camera.cameraInfo.zoomState.value?.maxZoomRatio -> {
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


    @Suppress("DEPRECATION")
    @SuppressLint("InflateParams", "MissingInflatedId", "SetTextI18n")
    private fun showSuccessToast(context : Context) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_success_toast, null)

        val textView = layout.findViewById<TextView>(R.id.tv_success_message)
        textView.text = "Your image is saved successfully"

        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }


    @SuppressLint("InflateParams")
    private fun displayPermissionDialog() : Boolean {
        val dialog = BottomSheetDialog(this, R.style.CenteredBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.btm_dialog_permission, null)

        val btEnable = view.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.bt_enable)

        btEnable.setOnClickListener {
            checkAndRequestPermissions()
            dialog.dismiss()
        }

        dialog.setCancelable(false)
        dialog.setContentView(view)
        dialog.show()

        return true
    }


    // TODO: Verify Permissions
    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
}