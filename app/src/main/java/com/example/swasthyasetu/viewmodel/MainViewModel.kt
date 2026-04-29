package com.example.swasthyasetu.viewmodel

import com.example.swasthyasetu.model.HealthTip
import com.example.swasthyasetu.model.UserProfile
import com.example.swasthyasetu.repository.HealthTipRepository
import com.example.swasthyasetu.repository.ProfileRepository
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val healthTipRepository = HealthTipRepository()
    private val profileRepository = ProfileRepository()

    // ── Health Tip LiveData ──
    private val _healthTip = MutableLiveData<HealthTip?>()
    val healthTip: LiveData<HealthTip?> = _healthTip

    private val _isLoadingTip = MutableLiveData(true)
    val isLoadingTip: LiveData<Boolean> = _isLoadingTip

    private val _tipError = MutableLiveData<String?>()
    val tipError: LiveData<String?> = _tipError

    // ── User Profile LiveData (for Medical Mini-Card) ──
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _isLoadingProfile = MutableLiveData(true)
    val isLoadingProfile: LiveData<Boolean> = _isLoadingProfile

    fun fetchDailyTip() {
        viewModelScope.launch {
            _isLoadingTip.value = true
            _tipError.value = null
            try {
                val tip = healthTipRepository.fetchLatestTip()
                _healthTip.value = tip
                if (tip != null) Log.d(TAG, "Health tip loaded: ${tip.title}")
                else Log.w(TAG, "No health tip found in Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch health tip", e)
                _healthTip.value = null
                _tipError.value = "Could not load health tip: ${e.localizedMessage}"
            } finally {
                _isLoadingTip.value = false
            }
        }
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            try {
                val profile = profileRepository.fetchUserProfile()
                _userProfile.value = profile
                Log.d(TAG, if (profile != null) "Profile loaded: ${profile.name}" else "No profile found")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch profile for mini-card", e)
                _userProfile.value = null
            } finally {
                _isLoadingProfile.value = false
            }
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
