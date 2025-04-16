package com.app.magnifier.magnifyingglass.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import com.app.magnifier.magnifyingglass.R
import com.app.magnifier.magnifyingglass.adapter.GalleryAdapter
import com.app.magnifier.magnifyingglass.databinding.ActivityGalleryBinding
import com.app.magnifier.magnifyingglass.model.GalleryItem
import com.app.magnifier.magnifyingglass.viewmodel.ImageViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel


class GalleryActivity : AppCompatActivity() {
    private val imageViewModel : ImageViewModel by viewModel()
    private lateinit var binding : ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private lateinit var imageItems: MutableList<GalleryItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        binding.ivBack.setOnClickListener {
            finish()
        }

        initAdapter(this)

        binding.btSelect.setOnClickListener {
            binding.btSelect.visibility = View.GONE
            binding.cbSelectAll.visibility = View.VISIBLE
            binding.ibDelete.visibility = View.VISIBLE
            binding.ivClose.visibility = View.VISIBLE
            adapter.toggleCheckbox(true)
        }

        binding.ivClose.setOnClickListener {
            binding.ivClose.visibility = View.GONE
            binding.btSelect.visibility = View.VISIBLE
            binding.cbSelectAll.visibility = View.GONE
            binding.ibDelete.visibility = View.GONE
            adapter.toggleCheckbox(false)
        }

        binding.ibDelete.setOnClickListener {
            displayDeleteDialog()
        }

        val list = adapter.getSelectedImages()
        Log.d("TAG_LIST", list.toString())
    }


    @SuppressLint("InflateParams")
    private fun displayDeleteDialog() {
        val dialog = BottomSheetDialog(this, R.style.CenteredBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.btm_dialog_delete, null)

        val tvDeleteDescription = view.findViewById<TextView>(R.id.tv_description)
        val btCancel = view.findViewById<AppCompatButton>(R.id.bt_cancel)
        val btDelete = view.findViewById<AppCompatButton>(R.id.bt_delete)

        tvDeleteDescription.setText(R.string.multi_delete_description)
        btCancel.setOnClickListener {
            dialog.dismiss()
        }

        btDelete.setOnClickListener {
            dialog.dismiss()
            binding.ivClose.performClick()
//            cameraViewModel.deletePicture(requireContext(), fileName)
//            deletePhoto()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun initAdapter(context: Context) {
        var imagesTaken = imageViewModel.getCapturedImages(this)
        imageItems = imageViewModel.flattenImageByDateList(imagesTaken)

        adapter = GalleryAdapter(imageItems)

        val layoutManager = GridLayoutManager(this, 4)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    GalleryAdapter.VIEW_TYPE_HEADER -> 4  // full width
                    else -> 1
                }
            }
        }
        binding.rvImages.layoutManager = layoutManager
        binding.rvImages.adapter = adapter

        adapter.setOnClickListener(object : GalleryAdapter.OnItemClickListener {
            override fun onItemClick(uri: Uri, fileName: String) {
                val bitmap = getBitmapFromUri(this@GalleryActivity, uri)
                // Display Image
                val ft : FragmentTransaction = supportFragmentManager.beginTransaction()
                val fragment = CaptureFragment()

                val bundle = Bundle()
                bundle.putParcelable("bitmap", bitmap)
                bundle.putString("fileName", fileName)
                fragment.arguments = bundle
                ft.add(R.id.fl_display_fragment, fragment)
                ft.addToBackStack(null)
                ft.commit()
            }
        })

        supportFragmentManager.setFragmentResultListener("capture_result", this) { _, bundle ->
            val isDelete = bundle.getBoolean("success")
            if (isDelete) {
                imagesTaken = imageViewModel.getCapturedImages(context)
                val updatedImageItems = imageViewModel.flattenImageByDateList(imagesTaken)

                // Update dữ liệu cho adapter
                adapter.setData(updatedImageItems)
            }
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