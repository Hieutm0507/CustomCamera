package com.app.magnifier.magnifyingglass.ui

import android.annotation.SuppressLint
import android.content.Intent
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
    }


    @SuppressLint("SetTextI18n")
    private fun initUI() {
        binding.tvAppVersion.text = getString(R.string.version) + ": " + Constants.APP_VERSION

        binding.ivBack.setOnClickListener {
            val ft = requireActivity().supportFragmentManager.beginTransaction()
            ft.setCustomAnimations(0, R.anim.slide_out)
            ft.remove(this)
            ft.commit()
        }

        binding.clImageSaved.setOnClickListener {
            val intent = Intent(requireContext(), GalleryActivity::class.java)
            startActivity(intent)
        }
    }
}