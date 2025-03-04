package com.allsoft.hieu.customcamera.utils

import android.Manifest

object Constants {
    const val TAG = "TAG_CAMERAX"
    const val FILE_NAME_FORMAT = "YY-mm-dd HH-mm"
    const val REQUEST_CODE_PERMISSIONS = 111
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}