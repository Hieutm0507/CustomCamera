package com.allsoft.hieu.customcamera.viewmodel

import android.Manifest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PermissionViewModel : ViewModel() {
    private val _permissionGranted = MutableLiveData<Boolean>()
    val permissionGranted: LiveData<Boolean> = _permissionGranted

    // TODO: ASK PERMISSIONS using ActivityResultLauncher
    fun checkAndEnablePermissions() {
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
    }
}