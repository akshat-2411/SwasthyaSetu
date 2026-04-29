package com.example.swasthyasetu.viewmodel

import android.app.Application
import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.swasthyasetu.model.EmergencyContact
import com.example.swasthyasetu.repository.EmergencyRepository
import kotlinx.coroutines.launch
class EmergencyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmergencyRepository()
    private val _emergencyContact = MutableLiveData<EmergencyContact?>()
    val emergencyContact: LiveData<EmergencyContact?> = _emergencyContact
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _sosResult = MutableLiveData<SosResult?>()
    val sosResult: LiveData<SosResult?> = _sosResult
    sealed class SosResult {
        data class Success(val contactName: String) : SosResult()
        data class Error(val message: String) : SosResult()
        object NoContactFound : SosResult()
        object PermissionRequired : SosResult()
    }
    fun fetchEmergencyContact() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val contact = repository.getPrimaryEmergencyContact()
                _emergencyContact.value = contact
                if (contact == null) {
                    Log.w(TAG, "No emergency contact found in Firestore")
                } else {
                    Log.d(TAG, "Emergency contact loaded: ${contact.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch emergency contact", e)
                _emergencyContact.value = null
                _sosResult.value = SosResult.Error(
                    "Could not load emergency contact: ${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun triggerSos(latitude: Double, longitude: Double) {
        val contact = _emergencyContact.value
        if (contact == null || contact.phone.isBlank()) {
            _sosResult.value = SosResult.NoContactFound
            return
        }
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _sosResult.value = SosResult.PermissionRequired
            return
        }
        try {
            val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
            val messageBody = "🚨 SOS EMERGENCY! 🚨\n\n" +
                    "I need immediate help!\n" +
                    "My current location:\n" +
                    "$mapsLink\n\n" +
                    "— Sent via SwasthyaSetu"
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(messageBody)
            smsManager.sendMultipartTextMessage(
                contact.phone, null, parts, null, null
            )
            Log.d(TAG, "SOS SMS sent to ${contact.name} (${contact.phone})")
            _sosResult.value = SosResult.Success(contact.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SOS SMS", e)
            _sosResult.value = SosResult.Error(
                "Failed to send SOS: ${e.localizedMessage}"
            )
        }
    }
    fun loadEmergencyContact() {
        _emergencyContact.value = EmergencyContact(
            name = "Emergency Contact",
            phone = "9999999999"
        )
    }
    fun clearSosResult() {
        _sosResult.value = null
    }
    companion object {
        private const val TAG = "EmergencyViewModel"
    }
}