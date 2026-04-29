package com.example.swasthyasetu.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import com.example.swasthyasetu.R
import androidx.appcompat.app.AppCompatActivity
import com.example.swasthyasetu.databinding.ActivityProfileBinding
import com.example.swasthyasetu.model.UserProfile
import com.example.swasthyasetu.viewmodel.ProfileViewModel

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()

    // ──────────────────────────────────────────────
    //  Dropdown Lists
    // ──────────────────────────────────────────────

    private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

    private val bloodGroupOptions = listOf(
        "A+", "A−", "B+", "B−", "AB+", "AB−", "O+", "O−"
    )

    // ──────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdowns()
        setupSaveButton()
        observeViewModel()

        // Fetch existing profile from Firestore to pre-fill form
        profileViewModel.fetchUserProfile()
    }

    // ──────────────────────────────────────────────
    //  Setup — Dropdown Adapters
    // ──────────────────────────────────────────────

    private fun setupDropdowns() {
        // Gender dropdown
        val genderAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        )
        binding.dropdownGender.setAdapter(genderAdapter)

        // Blood Group dropdown
        val bloodAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            bloodGroupOptions
        )
        binding.dropdownBloodGroup.setAdapter(bloodAdapter)
    }

    // ──────────────────────────────────────────────
    //  Setup — Save Button + Validation
    // ──────────────────────────────────────────────

    private fun setupSaveButton() {
        binding.btnSaveProfile.setOnClickListener {
            if (validateForm()) {
                saveProfile()
            }
        }

        binding.btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    // ──────────────────────────────────────────────
    //  Observe ViewModel
    // ──────────────────────────────────────────────

    private fun observeViewModel() {
        // Loading state — disable button while saving/loading
        profileViewModel.isLoading.observe(this) { isLoading ->
            binding.btnSaveProfile.isEnabled = !isLoading
            binding.btnSaveProfile.text = if (isLoading) {
                getString(R.string.profile_btn_saving)
            } else {
                getString(R.string.profile_btn_save)
            }
        }

        // Pre-fill form when profile is fetched
        profileViewModel.userProfile.observe(this) { profile ->
            profile?.let { prefillForm(it) }
        }

        // Save success → toast + finish
        profileViewModel.saveSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(
                    this,
                    getString(R.string.profile_saved_toast),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

        // Error handling
        profileViewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Pre-fill Form from Firestore Data
    // ──────────────────────────────────────────────

    /**
     * Populates all form fields with the fetched [UserProfile] data.
     * Dropdowns use [setText] with filter=false so the value is set
     * without triggering the autocomplete filter.
     */
    private fun prefillForm(profile: UserProfile) {
        binding.etFullName.setText(profile.name)

        if (profile.age > 0) {
            binding.etUserAge.setText(profile.age.toString())
        }

        if (profile.gender.isNotBlank()) {
            binding.dropdownGender.setText(profile.gender, false)
        }

        if (profile.bloodGroup.isNotBlank()) {
            binding.dropdownBloodGroup.setText(profile.bloodGroup, false)
        }

        if (profile.allergies.isNotBlank()) {
            binding.etAllergies.setText(profile.allergies)
        }

        if (profile.emergencyContact.isNotBlank()) {
            binding.etEmergencyContact.setText(profile.emergencyContact)
        }
    }

    // ──────────────────────────────────────────────
    //  Validation
    // ──────────────────────────────────────────────

    /**
     * Validates all required fields and sets inline errors on
     * the TextInputLayout containers.
     *
     * @return true if all validations pass
     */
    private fun validateForm(): Boolean {
        var isValid = true

        // Full Name — required
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        if (fullName.isEmpty()) {
            binding.tilFullName.error = getString(R.string.profile_error_name_required)
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        // Age — required, 1–120
        val ageText = binding.etUserAge.text?.toString()?.trim().orEmpty()
        val age = ageText.toIntOrNull()
        if (age == null || age < 1 || age > 120) {
            binding.tilUserAge.error = getString(R.string.profile_error_age_invalid)
            isValid = false
        } else {
            binding.tilUserAge.error = null
        }

        // Gender — required
        val gender = binding.dropdownGender.text?.toString()?.trim().orEmpty()
        if (gender.isEmpty()) {
            binding.tilGender.error = getString(R.string.profile_error_gender_required)
            isValid = false
        } else {
            binding.tilGender.error = null
        }

        // Blood Group — required
        val bloodGroup = binding.dropdownBloodGroup.text?.toString()?.trim().orEmpty()
        if (bloodGroup.isEmpty()) {
            binding.tilBloodGroup.error = getString(R.string.profile_error_blood_required)
            isValid = false
        } else {
            binding.tilBloodGroup.error = null
        }

        // Emergency Contact — required, at least 10 digits
        val emergencyContact = binding.etEmergencyContact.text?.toString()?.trim().orEmpty()
        if (emergencyContact.length < 10) {
            binding.tilEmergencyContact.error = getString(R.string.profile_error_phone_invalid)
            isValid = false
        } else {
            binding.tilEmergencyContact.error = null
        }

        return isValid
    }

    // ──────────────────────────────────────────────
    //  Save via ViewModel
    // ──────────────────────────────────────────────

    /**
     * Collects all validated field values into a [UserProfile]
     * and delegates saving to the ViewModel.
     */
    private fun saveProfile() {
        val profile = UserProfile(
            name = binding.etFullName.text.toString().trim(),
            age = binding.etUserAge.text.toString().trim().toInt(),
            gender = binding.dropdownGender.text.toString().trim(),
            bloodGroup = binding.dropdownBloodGroup.text.toString().trim(),
            allergies = binding.etAllergies.text?.toString()?.trim().orEmpty(),
            emergencyContact = binding.etEmergencyContact.text.toString().trim()
        )

        profileViewModel.saveUserProfile(profile)
    }

    // ──────────────────────────────────────────────
    //  Logout Action
    // ──────────────────────────────────────────────

    private fun logoutUser() {
        // Sign out from Firebase
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

        // Sign out from Google (to ensure account picker shows next time)
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()

        // Clear SharedPreferences
        val sharedPrefs = getSharedPreferences(packageName + "_preferences", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()

        // Redirect to Login
        val intent = android.content.Intent(this, LoginActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
