package com.example.swasthyasetu.view

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.swasthyasetu.databinding.ItemTimelineBinding
import com.example.swasthyasetu.model.TimelineEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (TimelineEvent) -> Unit
) : ListAdapter<TimelineEvent, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemTimelineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemTimelineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: TimelineEvent) {
            binding.tvEventTitle.text = event.title
            binding.tvEventSnippet.text = event.description
            binding.tvEventDate.text = dateFormat.format(Date(event.date))

            val dotColor = when (event.type) {
                "CHAT" -> Color.parseColor("#2196F3")
                "VACCINATION" -> Color.parseColor("#4CAF50")
                "SYMPTOM" -> Color.parseColor("#FFC107")
                else -> Color.GRAY
            }
            binding.timelineDot.backgroundTintList = ColorStateList.valueOf(dotColor)

            binding.root.setOnClickListener { onItemClick(event) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TimelineEvent>() {
            override fun areItemsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent) = oldItem == newItem
        }
    }
}