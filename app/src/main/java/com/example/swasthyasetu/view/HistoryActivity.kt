package com.example.swasthyasetu.view

import android.os.Bundle
import android.view.View
import android.content.Intent
import androidx.activity.viewModels
import com.example.swasthyasetu.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swasthyasetu.databinding.ActivityHistoryBinding
import com.example.swasthyasetu.model.TimelineEvent
import com.example.swasthyasetu.viewmodel.HistoryViewModel

class HistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private val viewModel: HistoryViewModel by viewModels()
    private var fullHistory = listOf<TimelineEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupFilters()
        setupObservers()
        setupAI()

        viewModel.loadHistory()
    }

    private fun setupAI() {
        binding.btnAiSummary.setOnClickListener {
            binding.btnAiSummary.visibility = View.GONE
            binding.layoutAiBubble.visibility = View.VISIBLE
            viewModel.generateHealthSummary(fullHistory)
        }
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter { event -> handleTimelineClick(event) }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun handleTimelineClick(event: TimelineEvent) {
        when (event.type) {
            "CHAT" -> {
                val intent = Intent(this, ChatActivity::class.java).apply { putExtra("CHAT_ID", event.id) }
                startActivity(intent)
            }
            "SYMPTOM" -> {
                val stubResult = com.example.swasthyasetu.model.DiagnosisResult(
                    illnessName = "Historical Record",
                    urgencyLevel = "Yellow",
                    homeCareTips = listOf(event.description),
                    isAiGenerated = true
                )
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_DIAGNOSIS_RESULT, stubResult)
                }
                startActivity(intent)
            }
            "VACCINATION" -> {
                VaccineDetailsBottomSheet.newInstance(event.title, event.description, event.date)
                    .show(supportFragmentManager, "VaccineSheet")
            }
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.tvNoEvents.text = "Loading timeline..."
                binding.tvNoEvents.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
            }
        }

        viewModel.timelineEvents.observe(this) { events ->
            fullHistory = events
            applyCurrentFilter()

            if (events.isNotEmpty() && binding.layoutAiBubble.visibility == View.GONE) {
                binding.btnAiSummary.visibility = View.VISIBLE
            } else {
                binding.btnAiSummary.visibility = View.GONE
            }
        }

        viewModel.isAiLoading.observe(this) { isLoading ->
            binding.pbAiLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.aiSummary.observe(this) { summaryText ->
            if (!summaryText.isNullOrEmpty()) {
                binding.tvAiSummaryText.text = summaryText
            }
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ -> applyCurrentFilter() }
    }

    private fun applyCurrentFilter() {
        val checkedIds = binding.chipGroupFilter.checkedChipIds
        if (checkedIds.isEmpty()) return

        val filteredList = when (checkedIds.first()) {
            R.id.chipChat -> fullHistory.filter { it.type == "CHAT" }
            R.id.chipVaccine -> fullHistory.filter { it.type == "VACCINATION" }
            R.id.chipSymptom -> fullHistory.filter { it.type == "SYMPTOM" }
            else -> fullHistory
        }

        adapter.submitList(filteredList)
        updateEmptyState(filteredList.size)
        if (filteredList.isNotEmpty()) binding.rvHistory.scrollToPosition(0)
    }

    private fun updateEmptyState(itemCount: Int) {
        if (itemCount == 0 && viewModel.isLoading.value == false) {
            binding.tvNoEvents.text = getString(R.string.history_no_events)
            binding.tvNoEvents.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else if (itemCount > 0) {
            binding.tvNoEvents.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
        }
    }
}