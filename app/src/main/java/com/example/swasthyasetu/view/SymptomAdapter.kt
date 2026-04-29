package com.example.swasthyasetu.view

import kotlin.collections.filter
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.swasthyasetu.R
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.swasthyasetu.model.Symptom
import com.example.swasthyasetu.databinding.ItemSymptomBinding


class SymptomAdapter(
    private val symptoms: List<Symptom>,
    private val onSelectionChanged: (List<Symptom>) -> Unit
) : RecyclerView.Adapter<SymptomAdapter.SymptomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomViewHolder {
        val binding = ItemSymptomBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SymptomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SymptomViewHolder, position: Int) {
        holder.bind(symptoms[position])
    }

    override fun getItemCount(): Int = symptoms.size

    fun getSelectedSymptoms(): List<Symptom> = symptoms.filter { it.isSelected }

    inner class SymptomViewHolder(
        private val binding: ItemSymptomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(symptom: Symptom) {
            val context = binding.root.context

            binding.tvSymptomName.text = symptom.name
            binding.ivSymptomIcon.setImageResource(symptom.iconRes)

            updateSelectionUI(symptom, context)

            binding.cardSymptom.setOnClickListener {
                symptom.isSelected = !symptom.isSelected
                updateSelectionUI(symptom, context)
                onSelectionChanged(getSelectedSymptoms())
            }
        }

        private fun updateSelectionUI(symptom: Symptom, context: android.content.Context) {
            if (symptom.isSelected) {
                // Selected: teal border + teal tint on icon bg
                binding.cardSymptom.strokeColor =
                    ContextCompat.getColor(context, R.color.primary)
                binding.cardSymptom.strokeWidth =
                    context.resources.getDimensionPixelSize(R.dimen.symptom_selected_stroke)
                binding.cardSymptom.cardElevation =
                    context.resources.getDimension(R.dimen.symptom_selected_elevation)
                binding.iconContainer.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_light))
            } else {
                // Unselected: light border
                binding.cardSymptom.strokeColor =
                    ContextCompat.getColor(context, R.color.primary_light)
                binding.cardSymptom.strokeWidth =
                    context.resources.getDimensionPixelSize(R.dimen.symptom_default_stroke)
                binding.cardSymptom.cardElevation =
                    context.resources.getDimension(R.dimen.card_elevation)
                binding.iconContainer.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_surface))
            }
        }
    }
}
