package com.app.magnifier.magnifyingglass.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.app.magnifier.magnifyingglass.R
import com.app.magnifier.magnifyingglass.databinding.FragmentCaptureBinding
import com.app.magnifier.magnifyingglass.viewmodel.CameraViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.koin.androidx.viewmodel.ext.android.viewModel


@Suppress("DEPRECATION")
class CaptureFragment : Fragment() {
    private val cameraViewModel : CameraViewModel by viewModel()
    private lateinit var binding: FragmentCaptureBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments
        val bitmap = bundle?.getParcelable<Bitmap>("bitmap")
        val fileName = bundle?.getString("fileName")
        binding.ivCaptureImg.setImageBitmap(bitmap)

        zoomImage()

        binding.ivBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btConvert.setOnClickListener {
            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val resultText = visionText.text
                        displayTextConvertDialog(resultText)
                    }
                    .addOnFailureListener { e ->
                        Log.e("OCR", "Text recognition failed", e)
                    }
            }
        }

        binding.ivDelete.setOnClickListener {
            if (fileName != null) {
                displayDeleteDialog(fileName)
            }
        }
    }


    @SuppressLint("InflateParams")
    private fun displayTextConvertDialog(resultText : String) : Boolean {
        val dialog = BottomSheetDialog(requireContext(), R.style.CenteredBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.btm_dialog_text_convert, null)

        val tvTextConverted = view.findViewById<TextView>(R.id.tv_text_convert)
        tvTextConverted.text = resultText

        val btClose = view.findViewById<AppCompatButton>(R.id.bt_close)
        btClose.setOnClickListener {
            dialog.dismiss()
        }

        val btCopy = view.findViewById<AppCompatButton>(R.id.bt_copy)
        btCopy.setOnClickListener {
            val textToCopy = tvTextConverted.text.toString()
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", textToCopy)
            clipboard.setPrimaryClip(clip)
        }

        dialog.setContentView(view)
        dialog.show()

        val behavior = BottomSheetBehavior.from(view.parent as View)
        behavior.isDraggable = true
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        return true
    }

    private fun zoomImage() {
        binding.ivCaptureImg.controller.settings.setRotationEnabled(false)
            .setZoomEnabled(true)
            .setMaxZoom(10f)
            .setDoubleTapEnabled(true)
            .setDoubleTapZoom(5f).setFillViewport(true)
            .setOverscrollDistance(20f, 20f)
    }

    @SuppressLint("InflateParams")
    private fun displayDeleteDialog(fileName : String) {
        val dialog = BottomSheetDialog(requireContext(), R.style.CenteredBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.btm_dialog_delete, null)

        val btCancel = view.findViewById<AppCompatButton>(R.id.bt_cancel)
        val btDelete = view.findViewById<AppCompatButton>(R.id.bt_delete)

        btCancel.setOnClickListener {
            dialog.dismiss()
        }

        btDelete.setOnClickListener {
            dialog.dismiss()
            cameraViewModel.deletePicture(requireContext(), fileName)
            val result = Bundle().apply { putBoolean("success", true) }
            parentFragmentManager.setFragmentResult("capture_result", result)
            parentFragmentManager.popBackStack()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}