package com.allsoft.hieu.customcamera.ui

import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import com.allsoft.hieu.customcamera.R
import com.allsoft.hieu.customcamera.databinding.FragmentGuidelineBinding


class GuidelineFragment : Fragment() {
    private lateinit var binding: FragmentGuidelineBinding
    private var step = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGuidelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btNext.setOnClickListener {
            step += 1
            updateUI()
        }
    }

    private fun updateUI() {
        when (step) {
            1 -> {
                binding.tvStep.text = "1/5"
                binding.tvInstruction.text = getString(R.string.instruct_1)
            }
            2 -> {
                binding.tvStep.text = "2/5"
                binding.tvInstruction.text = getString(R.string.instruct_2)
                binding.llBrightness.visibility = View.VISIBLE
                binding.ivArrow2.visibility = View.VISIBLE
                binding.ibFlash.visibility = View.INVISIBLE
                binding.ivArrow1.visibility = View.INVISIBLE
            }
            3 -> {
                binding.tvStep.text = "3/5"
                binding.tvInstruction.text = getString(R.string.instruct_3)
                binding.ibZoomOut.visibility = View.VISIBLE
                binding.ivArrow3.visibility = View.VISIBLE
                binding.llBrightness.visibility = View.INVISIBLE
                binding.ivArrow2.visibility = View.INVISIBLE

                // Change the position of clInstruct
                val marginInDp = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    10f,
                    resources.displayMetrics
                ).toInt()

                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.clContent)

                constraintSet.clear(binding.clInstruct.id, ConstraintSet.TOP)
                constraintSet.connect(binding.clInstruct.id, ConstraintSet.BOTTOM, binding.ivArrow4.id, ConstraintSet.TOP, marginInDp)
                constraintSet.connect(binding.clInstruct.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                constraintSet.connect(binding.clInstruct.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

                constraintSet.applyTo(binding.clContent)
                binding.clContent.requestLayout()
            }
            4 -> {
                binding.tvStep.text = "4/5"
                binding.tvInstruction.text = getString(R.string.instruct_4)
                binding.ibCamera.visibility = View.VISIBLE
                binding.ivArrow4.visibility = View.VISIBLE
                binding.ibZoomOut.visibility = View.INVISIBLE
                binding.ivArrow3.visibility = View.INVISIBLE
            }
            5 -> {
                binding.tvStep.text = "5/5"
                binding.tvInstruction.text = getString(R.string.instruct_5)
                binding.ibZoomIn.visibility = View.VISIBLE
                binding.ivArrow5.visibility = View.VISIBLE
                binding.ibCamera.visibility = View.INVISIBLE
                binding.ivArrow4.visibility = View.INVISIBLE
            }
            else -> {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }
}