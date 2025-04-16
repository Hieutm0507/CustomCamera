package com.app.magnifier.magnifyingglass.koin

import com.app.magnifier.magnifyingglass.viewmodel.CameraViewModel
import com.app.magnifier.magnifyingglass.viewmodel.ImageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        CameraViewModel()
    }

    viewModel {
        ImageViewModel()
    }
}