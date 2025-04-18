package com.app.magnifier.magnifyingglass.model

import android.net.Uri

sealed class GalleryItem {
    data class DateHeader(val date: String, var isSelected: Boolean = false) : GalleryItem()
    data class ImageItem(val uri: Uri, val date: String, var isSelected: Boolean = false) : GalleryItem()
}