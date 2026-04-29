package com.example.swasthyasetu.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.swasthyasetu.databinding.ItemHomecareTipBinding

class HomeCareTipAdapter(
    private val tips: List<String>
) : RecyclerView.Adapter<HomeCareTipAdapter.TipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val binding = ItemHomecareTipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.binding.tvTipText.text = tips[position]
    }

    override fun getItemCount(): Int = tips.size

    inner class TipViewHolder(val binding: ItemHomecareTipBinding) :
        RecyclerView.ViewHolder(binding.root)
}
