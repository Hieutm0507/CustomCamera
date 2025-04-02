package com.allsoft.hieu.customcamera.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.allsoft.hieu.customcamera.databinding.FragmentGuidelineCaptureBinding

class GuidelineCaptureFragment : Fragment() {
    private lateinit var binding: FragmentGuidelineCaptureBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGuidelineCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.setOnClickListener {
            val ft = requireActivity().supportFragmentManager.beginTransaction()
            ft.remove(this)
            ft.commit()
        }
    }
}