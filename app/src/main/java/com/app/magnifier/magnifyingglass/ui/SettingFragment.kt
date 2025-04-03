package com.app.magnifier.magnifyingglass.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.magnifier.magnifyingglass.R
import com.app.magnifier.magnifyingglass.databinding.FragmentSettingBinding
import com.app.magnifier.magnifyingglass.utils.Constants

class SettingFragment : Fragment() {
    private lateinit var binding: FragmentSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvAppVersion.text = getString(R.string.version) + ": " + Constants.APP_VERSION

        binding.ivBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
}