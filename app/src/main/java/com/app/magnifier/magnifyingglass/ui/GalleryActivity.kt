package com.app.magnifier.magnifyingglass.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
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
import com.app.magnifier.magnifyingglass.model.ImageByDate
import com.app.magnifier.magnifyingglass.viewmodel.CameraViewModel
import com.app.magnifier.magnifyingglass.viewmodel.ImageViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel


class GalleryActivity : AppCompatActivity() {
    private val imageViewModel : ImageViewModel by viewModel()
    private val cameraViewModel : CameraViewModel by viewModel()
    private lateinit var binding : ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private lateinit var imageItems: MutableList<GalleryItem>
    private lateinit var imagesTaken : List<ImageByDate>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        initAdapter()
        setupUI()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btSelect.setOnClickListener {
            binding.btSelect.visibility = View.GONE
            binding.cbSelectAll.visibility = View.VISIBLE
            binding.ibDelete.visibility = View.VISIBLE
            binding.ivClose.visibility = View.VISIBLE
            adapter.toggleCheckbox(true)
            binding.tvHeader.text = getString(R.string.selected_items, 0)
        }

        binding.ivClose.setOnClickListener {
            binding.ivClose.visibility = View.GONE
            binding.btSelect.visibility = View.VISIBLE
            binding.cbSelectAll.visibility = View.GONE
            binding.ibDelete.visibility = View.GONE
            adapter.toggleCheckbox(false)
            adapter.selectAllImages(this, false)
            binding.tvHeader.text = getString(R.string.image_saved)
        }
    }


    @SuppressLint("InflateParams")
    private fun displayDeleteDialog(chosenList: List<String>) {
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
            for (item in chosenList) {
                cameraViewModel.deletePicture(this, item)
            }
            reloadImages(this)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun initAdapter() {
        imagesTaken = imageViewModel.getCapturedImages(this)
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
                val bitmap = imageViewModel.getBitmapFromUri(this@GalleryActivity, uri)
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

            override fun onChosenListChangeListener(chosenList: List<String>) {
                binding.tvHeader.text = getString(R.string.selected_items, chosenList.size)

                binding.ibDelete.setOnClickListener {
                    displayDeleteDialog(chosenList)
                }
            }

            override fun onSelectAllStateChange(selectAllImg: Boolean) {
                binding.cbSelectAll.setOnCheckedChangeListener(null)
                binding.cbSelectAll.isChecked = selectAllImg
            }
        })

        supportFragmentManager.setFragmentResultListener("capture_result", this) { _, bundle ->
            val isDelete = bundle.getBoolean("success")
            if (isDelete) {
                reloadImages(this)
            }
        }

        binding.cbSelectAll.setOnCheckedChangeListener { _, selectAll ->
            adapter.selectAllImages(this, selectAll)
        }
    }

    private fun reloadImages(context: Context) {
        imagesTaken = imageViewModel.getCapturedImages(context)
        val updatedImageItems = imageViewModel.flattenImageByDateList(imagesTaken)

        // Update dữ liệu cho adapter
        adapter.setData(updatedImageItems)
    }
}