package com.example.swasthyasetu.view

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swasthyasetu.databinding.ActivityMyAppointmentsBinding
import com.example.swasthyasetu.viewmodel.BookingViewModel

class MyAppointmentsActivity : BaseActivity() {

    private lateinit var binding: ActivityMyAppointmentsBinding
    private lateinit var adapter: AppointmentAdapter
    private val viewModel: BookingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Maps back navigation cleanly overriding states
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupObservers()

        // Binds generic swiperefresh array listeners correctly piping bounds to ViewModel requests
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchMyAppointments()
        }

        // Auto trigger pipeline asynchronously securely polling explicit Auth data
        viewModel.fetchMyAppointments()
    }

    private fun setupRecyclerView() {
        adapter = AppointmentAdapter(emptyList())
        binding.rvAppointments.layoutManager = LinearLayoutManager(this)
        binding.rvAppointments.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.isLoadingAppointments.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.appointments.observe(this) { list ->
            if (list.isEmpty()) {
                binding.rvAppointments.visibility = View.GONE
                binding.llEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvAppointments.visibility = View.VISIBLE
                binding.llEmptyState.visibility = View.GONE
                adapter.updateData(list)
            }
        }
    }
}
