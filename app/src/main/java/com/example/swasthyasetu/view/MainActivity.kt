package com.example.swasthyasetu.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.content.Context
import com.example.swasthyasetu.R
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.swasthyasetu.databinding.ActivityMainBinding
import com.example.swasthyasetu.service.MyFirebaseMessagingService
import com.example.swasthyasetu.util.LocaleHelper
import com.example.swasthyasetu.viewmodel.EmergencyViewModel
import com.example.swasthyasetu.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.jvm.java

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val emergencyViewModel: EmergencyViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val emergencyAlertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(MyFirebaseMessagingService.EXTRA_ALERT_MESSAGE)
            val type =
                intent?.getStringExtra(MyFirebaseMessagingService.EXTRA_ALERT_TYPE) ?: "warning"
            if (!message.isNullOrBlank()) {
                showEmergencyAlert(message, type)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) android.util.Log.w("MainActivity", "Notification permission denied")
    }

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // SMS permission now granted — retry SOS flow
            showSosConfirmationDialog()
        } else {
            showToast(getString(R.string.sos_sms_permission_needed))
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchLocationAndTriggerSos()
        } else {
            showToast(getString(R.string.sos_location_needed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Subscribe to FCM topic based on geographic state (if location is already granted)
        if (hasLocationPermission()) {
            subscribeToLocalFirebaseTopic()
        }

        // Pre-fetch emergency contact from Firestore on launch
        emergencyViewModel.fetchEmergencyContact()

        // Fetch daily health tip from Firestore
        mainViewModel.fetchDailyTip()

        // Fetch user profile for Medical Mini-Card
        mainViewModel.fetchUserProfile()

        setupMiniCard()
        setupLanguageButton()

        // Initial greeting
        binding.tvGreeting.text = getString(R.string.greeting_text)

        setupHealthTip()
        setupDashboardCards()
        setupSosButton()
        observeViewModel()
        observeHealthTip()
        observeMiniCard()
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            emergencyAlertReceiver,
            IntentFilter(MyFirebaseMessagingService.ACTION_EMERGENCY_ALERT)
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(emergencyAlertReceiver)
    }

    private fun setupMiniCard() {
        binding.cardMedicalMini.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun observeMiniCard() {
        mainViewModel.userProfile.observe(this) { profile ->
            if (profile != null && profile.name.isNotBlank()) {
                // ─── Filled state ───
                binding.layoutMiniProfileFilled.visibility = View.VISIBLE
                binding.layoutMiniProfileEmpty.visibility = View.GONE

                binding.tvMiniName.text = profile.name
                binding.tvMiniBlood.text = profile.bloodGroup.ifBlank { "--" }

                // Update header greeting with user name
                binding.tvGreeting.text = getString(R.string.greeting_format, profile.name)
            } else {
                // ─── Empty state ───
                binding.layoutMiniProfileFilled.visibility = View.GONE
                binding.layoutMiniProfileEmpty.visibility = View.VISIBLE

                // Fallback greeting if no profile/name
                binding.tvGreeting.text = getString(R.string.greeting_text)
            }
        }
    }

    private fun setupHealthTip() {
        // Start shimmer animation
        startShimmer()

        binding.btnShareTip.setOnClickListener {
            val tipText = getString(R.string.health_tip_share_prefix) +
                    binding.tvHealthTipContent.text.toString()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, tipText)
                setPackage("com.whatsapp") // target WhatsApp directly
            }
            try {
                startActivity(shareIntent)
            } catch (e: Exception) {
                // WhatsApp not installed — fall back to generic share
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, tipText)
                }
                startActivity(Intent.createChooser(fallback, "Share Health Tip"))
            }
        }
    }

    private fun observeHealthTip() {
        // Observe loading state for shimmer
        mainViewModel.isLoadingTip.observe(this) { isLoading ->
            if (isLoading) {
                binding.shimmerPlaceholder.visibility = View.VISIBLE
                binding.tvHealthTipContent.visibility = View.INVISIBLE
            } else {
                stopShimmer()
                binding.shimmerPlaceholder.visibility = View.GONE
                binding.tvHealthTipContent.visibility = View.VISIBLE
            }
        }

        // Observe tip data
        mainViewModel.healthTip.observe(this) { tip ->
            if (tip != null) {
                binding.tvHealthTipTitle.text = tip.title.ifBlank {
                    getString(R.string.health_tip_title)
                }
                binding.tvHealthTipContent.text = tip.content.ifBlank {
                    getString(R.string.health_tip_default_content)
                }
                // Apply alert or normal styling based on isAlert flag
                applyAlertStyling(tip.isAlert)
            } else {
                // Default fallback text
                binding.tvHealthTipTitle.text = getString(R.string.health_tip_default)
                binding.tvHealthTipContent.text = getString(R.string.health_tip_default_content)
                applyAlertStyling(false)
            }
        }

        // Observe errors (optional toast)
        mainViewModel.tipError.observe(this) { error ->
            error?.let {
                // Silently fall back — default text is already shown
                android.util.Log.w("MainActivity", it)
            }
        }
    }

    private fun applyAlertStyling(isAlert: Boolean) {
        val card = binding.cardHealthTip

        if (isAlert) {
            // Emergency red stroke
            card.strokeWidth = resources.getDimensionPixelSize(R.dimen.alert_stroke_width)
            card.strokeColor = ContextCompat.getColor(this, R.color.accent_emergency)
            card.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.alert_tip_bg)
            )

            // Swap icon to warning triangle
            binding.ivTipIcon.setImageResource(R.drawable.ic_warning)
            binding.ivTipIcon.imageTintList =
                ContextCompat.getColorStateList(this, R.color.accent_emergency)
        } else {
            // Standard teal theme — no stroke
            card.strokeWidth = 0
            card.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.card_bg_health_tip)
            )

            // Lightbulb icon with primary tint
            binding.ivTipIcon.setImageResource(R.drawable.ic_lightbulb)
            binding.ivTipIcon.imageTintList =
                ContextCompat.getColorStateList(this, R.color.primary)
        }
    }

    private fun startShimmer() {
        binding.shimmerPlaceholder.visibility = View.VISIBLE
        // Start the animation-list drawables on each shimmer bar
        for (i in 0 until binding.shimmerPlaceholder.childCount) {
            val child = binding.shimmerPlaceholder.getChildAt(i)
            val bg = child.background
            if (bg is AnimationDrawable) bg.start()
        }
    }

    private fun stopShimmer() {
        for (i in 0 until binding.shimmerPlaceholder.childCount) {
            val child = binding.shimmerPlaceholder.getChildAt(i)
            val bg = child.background
            if (bg is AnimationDrawable) bg.stop()
        }
    }

    private fun setupDashboardCards() {
        binding.cardAiChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        binding.cardFindHospitals.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.cardVaccination.setOnClickListener {
            startActivity(Intent(this, VaccinationActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.cardFirstAid.setOnClickListener {
            startActivity(Intent(this, FirstAidActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.cardSymptomChecker.setOnClickListener {
            startActivity(Intent(this, SymptomActivity::class.java))
        }

        binding.cardReminders.setOnClickListener {
            startActivity(Intent(this, ReminderListActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.cardScanner.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    // ──────────────────────────────────────────────
    //  SOS Emergency Button — Long-Press to Trigger
    // ──────────────────────────────────────────────

    private fun setupSosButton() {
        // Short tap → hint that long-press is required
        binding.btnSosEmergency.setOnClickListener {
            showToast(getString(R.string.sos_long_press_hint))
        }

        // Long-press → show confirmation dialog → send SOS
        binding.btnSosEmergency.setOnLongClickListener {
            showSosConfirmationDialog()
            true // consume the event
        }
    }

    private fun showSosConfirmationDialog() {
        AlertDialog.Builder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents
        )
            .setTitle(getString(R.string.sos_dialog_title))
            .setMessage(getString(R.string.sos_dialog_message))
            .setPositiveButton(getString(R.string.btn_yes)) { _, _ ->
                triggerEmergencySos()
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun triggerEmergencySos() {
        // Step 1: Ensure SMS permission
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            return
        }

        // Step 2: Ensure location permission
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        // Step 3: Get GPS and send SOS
        fetchLocationAndTriggerSos()
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndTriggerSos() {
        showToast(getString(R.string.sos_getting_location))

        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                emergencyViewModel.triggerSos(location.latitude, location.longitude)
            } else {
                showToast(getString(R.string.map_location_unavailable))
            }
        }.addOnFailureListener {
            showToast(getString(R.string.map_location_error))
        }
    }

    private fun observeViewModel() {
        emergencyViewModel.sosResult.observe(this) { result ->
            when (result) {
                is EmergencyViewModel.SosResult.Success -> {
                    showToast(getString(R.string.sos_sms_sent, result.contactName))
                }

                is EmergencyViewModel.SosResult.Error -> {
                    showToast(getString(R.string.sos_sms_failed, result.message))
                }

                is EmergencyViewModel.SosResult.NoContactFound -> {
                    showToast(getString(R.string.sos_no_contact))
                }

                is EmergencyViewModel.SosResult.PermissionRequired -> {
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }

                null -> { /* consumed */
                }
            }
            // Clear after handling so it doesn't re-fire on config changes
            result?.let { emergencyViewModel.clearSosResult() }
        }
    }

    private fun setupLanguageButton() {
        binding.btnLanguage.setOnClickListener { showLanguageDialog() }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun showLanguageDialog() {
        val currentLang = LocaleHelper.getSavedLanguage(this)
        val languages = arrayOf(
            getString(R.string.language_english),
            getString(R.string.language_hindi)
        )
        val langCodes = LocaleHelper.SUPPORTED_LANGUAGES
        val checkedItem = langCodes.indexOf(currentLang)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.language_dialog_title))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedCode = langCodes[which]
                if (selectedCode != currentLang) {
                    val needsRecreate = LocaleHelper.setLocale(this, selectedCode)
                    dialog.dismiss()
                    if (needsRecreate) recreate()
                } else {
                    dialog.dismiss()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun subscribeToLocalFirebaseTopic() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Geocoding happens in background
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = android.location.Geocoder(
                            this@MainActivity,
                            java.util.Locale.getDefault()
                        )
                        val addresses =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val stateName = addresses[0].adminArea // e.g., "Punjab"
                            if (!stateName.isNullOrBlank()) {
                                // Format safely for FCM Topics (lowercase, alphanumeric, underscores only)
                                val sanitizedTopic = stateName.lowercase().replace(" ", "_")
                                    .replace(Regex("[^a-z0-9_-]"), "") + "_alerts"

                                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                                    .subscribeToTopic(sanitizedTopic)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            android.util.Log.d(
                                                "FCM",
                                                "Subscribed to local topic: $sanitizedTopic"
                                            )
                                        }
                                    }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Geocoder failed: ${e.message}")
                    }
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showEmergencyAlert(message: String, type: String) {
        binding.alertSection.visibility = View.VISIBLE
        binding.tvAlertTicker.text = message
        binding.tvAlertTicker.isSelected = true // Starts the marquee scrolling

        // Set icon based on alert type
        val iconResId = when (type.lowercase()) {
            "heatwave" -> R.drawable.ic_warning // You can replace with a specific ic_heatwave if available
            "outbreak" -> R.drawable.ic_warning // Replace with ic_virus or similar
            "flood" -> R.drawable.ic_warning    // Replace with ic_flood or similar
            else -> R.drawable.ic_warning
        }
        binding.ivAlertIcon.setImageResource(iconResId)

        // Pulsing animation for the warning card
        val pulseAnimation = android.animation.ObjectAnimator.ofFloat(
            binding.warningCard, "alpha", 1f, 0.6f, 1f
        )
        pulseAnimation.duration = 1200
        pulseAnimation.repeatCount = android.animation.ObjectAnimator.INFINITE
        pulseAnimation.start()

        // Set up click listener to launch detail page
        binding.warningCard.setOnClickListener {
            val intent = android.content.Intent(this, AlertDetailActivity::class.java).apply {
                putExtra(AlertDetailActivity.EXTRA_ALERT_TITLE, message)
                putExtra(AlertDetailActivity.EXTRA_ALERT_TYPE, type)
            }
            startActivity(intent)
        }
    }
}
