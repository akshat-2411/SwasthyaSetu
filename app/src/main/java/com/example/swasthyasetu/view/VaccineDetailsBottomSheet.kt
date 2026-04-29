package com.example.swasthyasetu.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.swasthyasetu.R
import com.example.swasthyasetu.databinding.DialogVaccineDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaccineDetailsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: DialogVaccineDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogVaccineDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: "Vaccine Tracker"
        val timestamp = arguments?.getLong(ARG_DATE) ?: System.currentTimeMillis()
        val description = arguments?.getString(ARG_DESC) ?: "Unknown Status"

        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        binding.tvSheetVaccineName.text = title
        binding.tvSheetVaccineDate.text = sdf.format(Date(timestamp))
        binding.tvSheetVaccineDescription.text = description

        binding.btnDownloadCert.setOnClickListener {
            Toast.makeText(requireContext(), R.string.vaccine_downloading, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_DATE = "arg_date"

        fun newInstance(title: String, description: String, date: Long): VaccineDetailsBottomSheet {
            return VaccineDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_DESC, description)
                    putLong(ARG_DATE, date)
                }
            }
        }
    }
}