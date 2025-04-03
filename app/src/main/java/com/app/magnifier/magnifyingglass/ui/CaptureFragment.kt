package com.app.magnifier.magnifyingglass.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.magnifier.magnifyingglass.databinding.FragmentCaptureBinding


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