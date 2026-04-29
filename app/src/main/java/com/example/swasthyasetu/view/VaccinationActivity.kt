package com.example.swasthyasetu.view

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import com.example.swasthyasetu.R
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swasthyasetu.databinding.ActivityVaccinationBinding
import com.example.swasthyasetu.viewmodel.VaccinationViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar

class VaccinationActivity : BaseActivity() {

    private lateinit var binding: ActivityVaccinationBinding
    private val viewModel: VaccinationViewModel by viewModels()
    private lateinit var adapter: VaccinationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaccinationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupDemoFab()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = VaccinationAdapter { vaccine, isChecked ->
            binding.progressBar.visibility = View.VISIBLE
            viewModel.updateVaccineStatus(vaccine, isChecked) { success ->
                binding.progressBar.visibility = View.GONE
                if (success) {
                    Snackbar.make(binding.root, R.string.vaccine_status_updated, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, getString(R.string.alert_error), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvVaccines.layoutManager = LinearLayoutManager(this)
        binding.rvVaccines.adapter = adapter
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(R.id.chipCompleted)) {
                viewModel.setFilter("completed")
            } else {
                viewModel.setFilter("upcoming")
            }
        }
    }

    private fun setupDemoFab() {
        binding.fabAddDemo.setOnClickListener {
            // Adds a simulated demo vaccine dynamically expiring in 7 days
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
            }
            viewModel.addDummyVaccine(getString(R.string.vaccine_add_demo), cal.timeInMillis)
            Snackbar.make(binding.root, "Demo entry pushed to Firestore", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            // Use repeatOnLifecycle so updates stop collecting when App is in background
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredVaccines.collect { vaccines ->
                    adapter.submitList(vaccines)
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
        binding.progressBar.visibility = View.VISIBLE
    }
}
