package com.example.swasthyasetu.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.swasthyasetu.databinding.ActivityScannerBinding
import com.example.swasthyasetu.R
import com.example.swasthyasetu.util.MedicineInfoUtil
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Capture / pick image
        binding.btnCapture.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return

            val image = InputImage.fromFilePath(this, uri)
            processImage(image)
        }
    }

    private fun processImage(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text

                val medicineName = extractMedicineName(extractedText)
                val info = MedicineInfoUtil.getMedicineInfo(medicineName)

                showResult(medicineName, info)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun extractMedicineName(text: String): String {
        val words = text.split("\n", " ")

        return words.firstOrNull { it.length > 4 } ?: "Unknown Medicine"
    }

    private fun showResult(name: String, info: String) {
        binding.layoutResult.removeAllViews()

        val view = layoutInflater.inflate(R.layout.item_scanner_result, null)

        val tvName = view.findViewById<android.widget.TextView>(R.id.tvMedicineName)
        val tvInfo = view.findViewById<android.widget.TextView>(R.id.tvMedicineInfo)
        val btnReminder = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddReminder)
        val btnAI = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAskAI)

        tvName.text = name
        tvInfo.text = info

        btnReminder.setOnClickListener {
            val intent = Intent(this, AddReminderActivity::class.java)
            intent.putExtra("medicine_name", name)
            startActivity(intent)
        }

        btnAI.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("SYMPTOM_QUERY", "Tell me about $name medicine")
            startActivity(intent)
        }

        binding.layoutResult.addView(view)
    }
}