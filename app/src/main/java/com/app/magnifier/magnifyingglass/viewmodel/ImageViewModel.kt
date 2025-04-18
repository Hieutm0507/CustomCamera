package com.app.magnifier.magnifyingglass.viewmodel

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import com.app.magnifier.magnifyingglass.model.GalleryItem
import com.app.magnifier.magnifyingglass.model.ImageByDate
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale

class ImageViewModel : ViewModel() {
    fun flattenImageByDateList(data: List<ImageByDate>): MutableList<GalleryItem> {
        val result = mutableListOf<GalleryItem>()
        for (group in data) {
            result.add(GalleryItem.DateHeader(group.date))
            group.uris.forEach { result.add(GalleryItem.ImageItem(it, group.date)) }
        }
        return result
    }

    fun getCapturedImages(context: Context): List<ImageByDate> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Pictures/Magnifier%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val map = mutableMapOf<String, MutableList<Uri>>()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateTakenMillis = cursor.getLong(dateColumn)
                val fallbackDateMillis = cursor.getLong(dateAddedColumn) * 1000
                val finalDateMillis = if (dateTakenMillis > 0) dateTakenMillis else fallbackDateMillis
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(finalDateMillis))
                val uri = ContentUris.withAppendedId(collection, id)
                map.getOrPut(date) { mutableListOf() }.add(uri)
            }
        }

        return map.entries.sortedByDescending { it.key }
            .map { entry ->
                val sortedUris = entry.value.reversed()         // Reverse to ensure newer upper
                ImageByDate(entry.key, sortedUris)
            }
    }

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}