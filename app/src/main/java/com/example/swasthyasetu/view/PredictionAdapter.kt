package com.example.swasthyasetu.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.swasthyasetu.databinding.ItemDiseaseRiskBinding
import com.example.swasthyasetu.util.DiseaseRisk
import com.example.swasthyasetu.util.RiskLevel

class PredictionAdapter(private val risks: List<DiseaseRisk>) : RecyclerView.Adapter<PredictionAdapter.RiskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiskViewHolder {
        val binding = ItemDiseaseRiskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RiskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiskViewHolder, position: Int) {
        holder.bind(risks[position])
    }

    override fun getItemCount(): Int = risks.size

    inner class RiskViewHolder(private val binding: ItemDiseaseRiskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(risk: DiseaseRisk) {
            binding.tvDiseaseName.text = risk.name
            binding.tvDiseaseScore.text = "${risk.probability}%"
            binding.progressSecondary.progress = risk.probability

            val colorStr = when (risk.riskLevel) {
                RiskLevel.HIGH -> "#F44336"
                RiskLevel.MEDIUM -> "#FF9800"
                RiskLevel.LOW -> "#4CAF50"
            }
            binding.progressSecondary.setIndicatorColor(Color.parseColor(colorStr))
        }
    }
}
