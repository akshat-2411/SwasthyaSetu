package com.example.swasthyasetu.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.swasthyasetu.R
import com.example.swasthyasetu.database.ReminderEntity
import com.example.swasthyasetu.databinding.ActivityAddReminderBinding
import com.example.swasthyasetu.repository.ReminderRepository
import com.example.swasthyasetu.viewmodel.ReminderViewModel
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddReminderActivity : BaseActivity() {

    private lateinit var binding: ActivityAddReminderBinding

    // Wire native Room abstraction pipeline
    private val viewModel: ReminderViewModel by viewModels()

    // Core calendar instance safely tracking hour/min modifications
    private var reminderCalendar: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupTimePicker()
        setupSaveButton()
    }

    private fun setupTimePicker() {
        binding.btnSelectTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Medication Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                // Initialize clean calendar mapped specifically to the picker dials
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, picker.hour)
                cal.set(Calendar.MINUTE, picker.minute)
                cal.set(Calendar.SECOND, 0)

                reminderCalendar = cal

                // Format calendar to beautiful AM/PM UI String
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                binding.tvSelectedTime.text = sdf.format(cal.time)
            }

            picker.show(supportFragmentManager, "REMINDER_TIME_PICKER")
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveReminder.setOnClickListener {
            val medicineName = binding.etMedicineName.text.toString().trim()
            val dosage = binding.etDosage.text.toString().trim()

            // Extract the Checked Chip explicitly
            val frequencyText = when (binding.chipGroupFrequency.checkedChipId) {
                R.id.chipOnce -> getString(R.string.reminder_freq_once)
                R.id.chipTwice -> getString(R.string.reminder_freq_twice)
                R.id.chipThrice -> getString(R.string.reminder_freq_thrice)
                else -> "Unknown Frequency"
            }

            // Validation Parameters
            if (medicineName.isEmpty() || dosage.isEmpty() || reminderCalendar == null) {
                Toast.makeText(this, getString(R.string.reminder_error_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timeString = binding.tvSelectedTime.text.toString()
            val uniqueRequestCode = (System.currentTimeMillis() % 100000).toInt()

            // Scaffold internal Entity Map cleanly matching parameters
            val entity = ReminderEntity(
                medicineName = medicineName,
                dosage = dosage,
                timeInMillis = reminderCalendar!!.timeInMillis,
                timeString = timeString,
                frequency = frequencyText,
                isActive = true,
                alarmRequestCode = uniqueRequestCode
            )

            // Submit native database save implicitly via coroutine
            viewModel.insert(entity)

            // Map and Deploy the Hardware level background job trigger natively bypassing standard UI maps
            val repository = ReminderRepository()
            repository.scheduleAlarm(this, reminderCalendar!!.timeInMillis, medicineName, uniqueRequestCode)

            Toast.makeText(this, "Reminder correctly armed for $timeString.", Toast.LENGTH_SHORT).show()

            // Close module since mapping completed
            finish()
        }
    }
}
