package com.example.swasthyasetu.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.swasthyasetu.databinding.ItemFirstAidBinding
import com.example.swasthyasetu.model.FirstAidContent
import java.util.Locale
import kotlin.collections.filter

class FirstAidAdapter(
    private val fullList: List<FirstAidContent>,
    private val onItemClick: (FirstAidContent) -> Unit
) : RecyclerView.Adapter<FirstAidAdapter.ViewHolder>(), Filterable {

    var filteredList: List<FirstAidContent> = fullList
    var onFilterComplete: (() -> Unit)? = null

    inner class ViewHolder(private val binding: ItemFirstAidBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FirstAidContent) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description

            val resId = binding.root.context.resources.getIdentifier(item.iconResName, "drawable", binding.root.context.packageName)
            if (resId != 0) binding.ivIcon.setImageResource(resId)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFirstAidBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount() = filteredList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint?.toString() ?: ""
                val resultsList = if (charSearch.isEmpty()) fullList else {
                    fullList.filter { it.title.lowercase(Locale.ROOT).contains(charSearch.lowercase(Locale.ROOT)) }
                }
                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<FirstAidContent>
                notifyDataSetChanged()
                onFilterComplete?.invoke()
            }
        }
    }
}