package com.example.swasthyasetu.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import android.widget.Toast
import com.example.swasthyasetu.databinding.ActivityHospitalDetailBinding
import com.example.swasthyasetu.model.Hospital
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.parcel.Parcelize
import kotlin.jvm.java

class HospitalDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityHospitalDetailBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHospitalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup immersive back stack navigator
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Secure Parcelable extraction across Android API limits seamlessly
        val hospital = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_HOSPITAL", Hospital::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("EXTRA_HOSPITAL")
        }

        if (hospital == null) {
            Toast.makeText(this, "Hospital data unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindData(hospital)
        setupActions(hospital)
    }

    private fun bindData(hospital: Hospital) {
        binding.tvHospitalName.text = hospital.name
        binding.tvAddress.text = hospital.address

        // specialty won't be available from Places API — hide or show N/A
        binding.tvSpecialty.text = if (!hospital.specialty.isNullOrBlank())
            hospital.specialty else "General / Multi-Specialty"

        binding.tvRating.text = if (hospital.rating > 0.0) "⭐ ${hospital.rating}" else "⭐ N/A"
    }
    @Parcelize
    data class Hospital(
        val name: String,
        val address: String,
        val rating: Double,
        val lat: Double,
        val lng: Double,
        val specialty: String? = null,      // nullable — Places API doesn't provide this
        val phoneNumber: String? = null     // nullable — Places API doesn't provide this
    ) : Parcelable

    private fun setupActions(hospital: Hospital) {
        binding.fabBook.setOnClickListener {
            // Guard against BookingBottomSheet crash if not implemented
            try {
                val bottomSheet = BookingBottomSheet.newInstance(hospital.name)
                bottomSheet.show(supportFragmentManager, "BookingBottomSheet")
            } catch (e: Exception) {
                Toast.makeText(this, "Booking coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabCall.setOnClickListener {
            val phone = hospital.phoneNumber
            if (!phone.isNullOrBlank()) {
                makePhoneCall(phone)
            } else {
                Snackbar.make(binding.root, "Phone number not available for this hospital.", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.fabSave.setOnClickListener {
            val phone = hospital.phoneNumber ?: ""
            saveToContacts(hospital.name, phone)
        }
    }

    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        try {
            startActivity(intent)
            Snackbar.make(binding.root, "Dialing $phoneNumber...", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Unable to launch dialer.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun saveToContacts(name: String, phone: String) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, name)
            putExtra(ContactsContract.Intents.Insert.PHONE, phone)
        }
        try {
            startActivity(intent)
            Snackbar.make(binding.root, "Opening Contacts App...", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Unable to launch Contacts App.", Snackbar.LENGTH_SHORT).show()
        }
    }
}
