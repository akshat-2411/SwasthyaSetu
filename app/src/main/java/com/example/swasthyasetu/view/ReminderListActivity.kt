package com.example.swasthyasetu.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swasthyasetu.databinding.ActivityReminderListBinding
import com.example.swasthyasetu.viewmodel.ReminderViewModel

class ReminderListActivity : BaseActivity() {

    private lateinit var binding: ActivityReminderListBinding

    // Extrapolate view interactions actively straight to Android VM schemas dynamically handling database bindings.
    private val viewModel: ReminderViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupObservers()

        // Execute Action Trigger to Scaffold AddReminder Form dynamically
        binding.fabAddReminder.setOnClickListener {
            startActivity(Intent(this, AddReminderActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = ReminderAdapter(
            onToggleInteraction = { reminder, isChecked ->
                // Map the active visual user state over the persistent offline memory state
                reminder.isActive = isChecked
                viewModel.update(reminder)
            },
            onDeleteInteraction = { reminder ->
                viewModel.delete(reminder)
            }
        )
        binding.rvReminders.layoutManager = LinearLayoutManager(this)
        binding.rvReminders.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allReminders.observe(this) { reminders ->
            // Enforce hard null-check preventing crashes during asynchronous SQLite Room load frames
            if (reminders != null) {
                adapter.submitList(reminders)
                if (reminders.isEmpty()) {
                    binding.tvNoReminders.visibility = View.VISIBLE
                    binding.rvReminders.visibility = View.GONE
                } else {
                    binding.tvNoReminders.visibility = View.GONE
                    binding.rvReminders.visibility = View.VISIBLE
                }
            } else {
                adapter.submitList(emptyList())
            }
        }
    }
}
