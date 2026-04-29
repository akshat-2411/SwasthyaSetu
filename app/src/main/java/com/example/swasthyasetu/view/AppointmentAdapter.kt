package com.example.swasthyasetu.view

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.swasthyasetu.R
import androidx.recyclerview.widget.RecyclerView
import com.example.swasthyasetu.databinding.ItemAppointmentBinding
import com.example.swasthyasetu.model.Appointment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentAdapter(private var appointments: List<Appointment>) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    fun updateData(newAppointments: List<Appointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int = appointments.size

    inner class ViewHolder(private val binding: ItemAppointmentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appointment: Appointment) {
            binding.tvHospitalName.text = appointment.hospitalName

            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateString = sdf.format(Date(appointment.appointmentDate))

            binding.tvDateTime.text = "$dateString • ${appointment.timeSlot}"

            binding.chipStatus.text = appointment.status

            when (appointment.status.lowercase()) {
                "confirmed" -> binding.chipStatus.setChipBackgroundColorResource(R.color.success_green)
                "completed" -> binding.chipStatus.setChipBackgroundColorResource(R.color.primary)
                else -> binding.chipStatus.setChipBackgroundColorResource(R.color.warning_amber) // Default Pending state
            }
        }
    }
}
