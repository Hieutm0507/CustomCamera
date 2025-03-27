package com.allsoft.hieu.customcamera.koin

import com.allsoft.hieu.customcamera.viewmodel.CameraViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        CameraViewModel()
    }
}