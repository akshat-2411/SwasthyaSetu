package com.example.swasthyasetu.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicineName: String,
    val dosage: String,
    val timeInMillis: Long,
    val timeString: String,
    val frequency: String,
    var isActive: Boolean = true,
    var alarmRequestCode: Int = 0
)