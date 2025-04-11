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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


@Suppress("DEPRECATION")
class CaptureFragment : Fragment() {
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
}