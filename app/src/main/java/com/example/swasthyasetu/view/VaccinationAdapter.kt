package com.example.swasthyasetu.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.swasthyasetu.databinding.ItemVaccineBinding
import com.example.swasthyasetu.model.Vaccine
import com.example.swasthyasetu.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaccinationAdapter(
    private val onVaccineCheckChanged: (Vaccine, Boolean) -> Unit
) : ListAdapter<Vaccine, VaccinationAdapter.VaccineViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaccineViewHolder {
        val binding = ItemVaccineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VaccineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VaccineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VaccineViewHolder(private val binding: ItemVaccineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(vaccine: Vaccine) {
            binding.tvVaccineName.text = vaccine.name

            val formattedDate = dateFormat.format(Date(vaccine.dueDate))
            binding.tvDueDate.text = binding.root.context.getString(R.string.vaccine_due_date, formattedDate)

            // Remove listener before setting checked state to avoid unwanted triggers during recycling
            binding.cbVaccineDone.setOnCheckedChangeListener(null)

            binding.cbVaccineDone.isChecked = vaccine.isCompleted

            binding.cbVaccineDone.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != vaccine.isCompleted) {
                    onVaccineCheckChanged(vaccine, isChecked)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Vaccine>() {
            override fun areItemsTheSame(oldItem: Vaccine, newItem: Vaccine): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Vaccine, newItem: Vaccine): Boolean {
                return oldItem == newItem
            }
        }
    }
}
