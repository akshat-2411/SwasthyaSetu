package com.example.swasthyasetu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.swasthyasetu.database.AppDatabase
import com.example.swasthyasetu.database.ReminderEntity
import com.example.swasthyasetu.repository.ReminderRepository
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).reminderDao()
    private val repository = ReminderRepository()
    val allReminders: LiveData<List<ReminderEntity>> = dao.getAllReminders().asLiveData()

    fun insert(reminder: ReminderEntity) {
        viewModelScope.launch { dao.insert(reminder) }
    }

    fun update(reminder: ReminderEntity) {
        viewModelScope.launch {
            dao.update(reminder)
            if (!reminder.isActive) repository.cancelAlarm(getApplication(), reminder.alarmRequestCode)
            else repository.scheduleAlarm(getApplication(), reminder.timeInMillis, reminder.medicineName, reminder.alarmRequestCode)
        }
    }

    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.cancelAlarm(getApplication(), reminder.alarmRequestCode)
            dao.delete(reminder)
        }
    }
}