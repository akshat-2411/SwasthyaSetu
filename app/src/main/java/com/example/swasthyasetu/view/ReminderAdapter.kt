package com.example.swasthyasetu.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.swasthyasetu.database.ReminderEntity
import com.example.swasthyasetu.databinding.ItemReminderBinding

class ReminderAdapter(
    private val onToggleInteraction: (ReminderEntity, Boolean) -> Unit,
    private val onDeleteInteraction: (ReminderEntity) -> Unit
) : ListAdapter<ReminderEntity, ReminderAdapter.ReminderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReminderViewHolder(private val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reminder: ReminderEntity) {
            binding.tvMedicineName.text = "${reminder.medicineName} (${reminder.dosage})"
            binding.tvReminderTime.text = reminder.timeString
            binding.tvReminderFreq.text = "(${reminder.frequency})"

            // Temporarily suppress listener to avoid reactive loop updates natively triggering updates while simply binding
            binding.switchActive.setOnCheckedChangeListener(null)

            // Map OS Switch layout
            binding.switchActive.isChecked = reminder.isActive

            // Bind explicitly unmasking exact triggers
            binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
                onToggleInteraction(reminder, isChecked)
            }

            // Map standard delete mapping trigger
            binding.btnDelete.setOnClickListener {
                onDeleteInteraction(reminder)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ReminderEntity>() {
            override fun areItemsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity) = oldItem == newItem
        }
    }
}
