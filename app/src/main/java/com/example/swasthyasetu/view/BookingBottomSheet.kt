package com.example.swasthyasetu.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.swasthyasetu.databinding.LayoutBookingFormBinding
import com.example.swasthyasetu.viewmodel.BookingViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class BookingBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBookingFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingViewModel by viewModels()

    private var selectedDateMills: Long = System.currentTimeMillis()
    private var hospitalName: String = ""

    companion object {
        private const val ARG_HOSPITAL_NAME = "hospital_name"

        fun newInstance(hospitalName: String): BookingBottomSheet {
            val fragment = BookingBottomSheet()
            val args = Bundle()
            args.putString(ARG_HOSPITAL_NAME, hospitalName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutBookingFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hospitalName = arguments?.getString(ARG_HOSPITAL_NAME) ?: "Hospital"
        binding.tvHospitalNameHeader.text = "Book at $hospitalName"

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDateMills = calendar.timeInMillis
        }

        binding.btnConfirmRequest.setOnClickListener {
            val slot = if (binding.rbMorning.isChecked) "Morning" else "Evening"
            viewModel.submitBooking(hospitalName, selectedDateMills, slot)
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.isSubmitting.observe(viewLifecycleOwner) { isSubmitting ->
            binding.progressBar.visibility = if (isSubmitting) View.VISIBLE else View.GONE
            binding.btnConfirmRequest.isEnabled = !isSubmitting
            binding.calendarView.isEnabled = !isSubmitting
            binding.rgTimeSlot.isEnabled = !isSubmitting
        }

        viewModel.isSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                playSuccessAnimation()
            }
        }
    }

    private fun playSuccessAnimation() {
        // Swap UI instantly
        binding.llBookingForm.visibility = View.GONE
        binding.llSuccessState.visibility = View.VISIBLE

        // Fire native ViewPropertyAnimator leveraging Overshoot bounce effect (Lottie alternative natively mapped)
        binding.ivSuccessCheck.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                // Auto dismiss after a pause
                binding.root.postDelayed({ dismiss() }, 2000)
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
