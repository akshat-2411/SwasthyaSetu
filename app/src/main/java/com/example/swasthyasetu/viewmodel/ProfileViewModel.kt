package com.example.swasthyasetu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthyasetu.model.UserProfile
import com.example.swasthyasetu.repository.ProfileRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val profile = repository.fetchUserProfile()
                _userProfile.value = profile
                if (profile != null) {
                    Log.d(TAG, "Profile loaded: ${profile.name}")
                } else {
                    Log.d(TAG, "No existing profile found — new user")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch profile", e)
                _error.value = "Could not load profile: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveSuccess.value = false
            try {
                repository.saveUserProfile(profile)
                Log.d(TAG, "Profile saved for: ${profile.name}")
                _saveSuccess.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save profile", e)
                _error.value = "Could not save profile: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
