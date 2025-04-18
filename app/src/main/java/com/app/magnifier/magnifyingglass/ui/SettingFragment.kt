package com.app.magnifier.magnifyingglass.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.app.magnifier.magnifyingglass.R
import com.app.magnifier.magnifyingglass.databinding.FragmentSettingBinding
import com.app.magnifier.magnifyingglass.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetDialog

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
        
        binding.clFeedback.setOnClickListener { 
            displayFeedbackDialog()
        }

        binding.clRate.setOnClickListener {
            displayRatingDialog()
        }
    }

    private fun displayFeedbackDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.CenteredBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.btm_dialog_feedback, null)
        
        val etFeedback = view.findViewById<EditText>(R.id.et_feedback)
        val btCancel = view.findViewById<AppCompatButton>(R.id.bt_cancel)
        val btSubmit = view.findViewById<AppCompatButton>(R.id.bt_submit)


        etFeedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                btSubmit.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_button)
                btSubmit.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                btSubmit.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
                btSubmit.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                btSubmit.setOnClickListener {
                    //send feedback
                }
            }

            override fun afterTextChanged(text: Editable?) {}

        })

        btCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }
    private fun displayRatingDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.CenteredBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.btm_dialog_rate, null)


        val btCancel = view.findViewById<AppCompatButton>(R.id.bt_cancel)
        val btSend = view.findViewById<AppCompatButton>(R.id.bt_send)


        btCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

}