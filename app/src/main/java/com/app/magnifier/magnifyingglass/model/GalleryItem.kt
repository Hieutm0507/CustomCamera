package com.app.magnifier.magnifyingglass.model

import android.net.Uri

sealed class GalleryItem {
    data class DateHeader(val date: String) : GalleryItem()
    data class ImageItem(val uri: Uri, var isSelected: Boolean = false) : GalleryItem()
}