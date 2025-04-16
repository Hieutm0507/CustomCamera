package com.app.magnifier.magnifyingglass.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.magnifier.magnifyingglass.databinding.ItemDateHeaderBinding
import com.app.magnifier.magnifyingglass.databinding.ItemImageBinding
import com.app.magnifier.magnifyingglass.model.GalleryItem


@SuppressLint("NotifyDataSetChanged")
class GalleryAdapter(private var items: MutableList<GalleryItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var mListener: OnItemClickListener? = null
    private var showCheckbox = false

    fun setOnClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    companion object {
        const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PHOTO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is GalleryItem.DateHeader -> VIEW_TYPE_HEADER
            is GalleryItem.ImageItem -> VIEW_TYPE_PHOTO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_PHOTO -> {
                val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PhotoViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GalleryItem.DateHeader -> (holder as HeaderViewHolder).bind(item)
            is GalleryItem.ImageItem -> (holder as PhotoViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: GalleryItem.DateHeader) {
            binding.tvDate.text = header.date
            binding.cbCheckBox.visibility =
                if(showCheckbox) View.VISIBLE
                else View.GONE
        }
    }

    inner class PhotoViewHolder(private val binding : ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(photo: GalleryItem.ImageItem) {
            binding.ivImageItem.setImageURI(photo.uri)
            binding.cbCheckBox.visibility =
                if(showCheckbox) View.VISIBLE
                else View.GONE
            binding.cbCheckBox.isChecked = photo.isSelected

            binding.cbCheckBox.setOnCheckedChangeListener { _, isChecked ->
                photo.isSelected = isChecked
            }
        }

        init {
            binding.root.setOnClickListener {
                val item = items[adapterPosition] as GalleryItem.ImageItem
                val fileName = getFileNameFromUri(binding.root.context, item.uri)
                mListener?.onItemClick(item.uri, fileName)
            }
        }
    }

    fun toggleCheckbox(isShown : Boolean) {
        showCheckbox = isShown
        notifyDataSetChanged()
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            fileName = if (nameIndex >= 0 && it.moveToFirst()) {
                it.getString(nameIndex)
            } else {
                uri.lastPathSegment ?: "unknown_file"
            }
        }
        return fileName
    }

    fun getSelectedImages(): List<Uri> {
        return items.filterIsInstance<GalleryItem.ImageItem>()
            .filter { it.isSelected }
            .map { it.uri }
    }

    fun setData(imageItems: MutableList<GalleryItem>) {
        items = imageItems
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick(uri: Uri, fileName: String)
    }
}