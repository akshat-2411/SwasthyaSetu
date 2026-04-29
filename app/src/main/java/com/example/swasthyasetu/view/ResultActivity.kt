package com.example.swasthyasetu.view

import com.example.swasthyasetu.model.DiagnosisResult
import com.example.swasthyasetu.R
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swasthyasetu.databinding.ActivityResultBinding

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding

    companion object {
        const val EXTRA_DIAGNOSIS_RESULT = "EXTRA_DIAGNOSIS_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        val result = intent.getSerializableExtra(EXTRA_DIAGNOSIS_RESULT) as? DiagnosisResult

        if (result == null) {
            finish()
            return
        }

        displayResult(result)
        setupClickListeners(result)
    }

    private fun displayResult(result: DiagnosisResult) {
        binding.tvIllnessName.text = result.illnessName

        val (emoji, label, bgColor, cardTint) = when (result.urgencyLevel) {
            "Red" -> UrgencyStyle(
                emoji = "🔴",
                label = "HIGH URGENCY — See a Doctor",
                bgColor = ContextCompat.getColor(this, R.color.accent_emergency),
                cardTint = Color.parseColor("#FFF3E0")
            )
            "Yellow" -> UrgencyStyle(
                emoji = "🟡",
                label = "MODERATE URGENCY — Monitor Closely",
                bgColor = Color.parseColor("#FFA000"),
                cardTint = Color.parseColor("#FFFDE7")
            )
            else -> UrgencyStyle(
                emoji = "🟢",
                label = "LOW URGENCY — Home Care Advised",
                bgColor = ContextCompat.getColor(this, R.color.success_green),
                cardTint = Color.parseColor("#E8F5E9")
            )
        }

        binding.tvUrgencyIcon.text = emoji
        binding.tvUrgencyLabel.text = label

        // Tint the pill background
        val pillBg = binding.tvUrgencyLabel.background
        if (pillBg is GradientDrawable) {
            pillBg.setColor(bgColor)
        } else {
            binding.tvUrgencyLabel.backgroundTintList = ColorStateList.valueOf(bgColor)
        }

        // Tint the card
        binding.cardUrgency.setCardBackgroundColor(cardTint)

        binding.rvHomeCare.layoutManager = LinearLayoutManager(this)
        binding.rvHomeCare.adapter = HomeCareTipAdapter(result.homeCareTips)

        binding.tvDisclaimer.text = getString(R.string.result_disclaimer)

        if (result.isAiGenerated) {
            binding.tvAiBadge.visibility = android.view.View.VISIBLE
        } else {
            binding.tvAiBadge.visibility = android.view.View.GONE
        }
    }

    private fun setupClickListeners(result: DiagnosisResult) {
        binding.btnResultBack.setOnClickListener { finish() }

        // Primary CTA — navigate to hospital map
        binding.btnFindDoctor.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.btnGoHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        binding.btnConsultAI.setOnClickListener {
            val symptomQuery = "I was diagnosed with possible ${result.illnessName}. " +
                    "Urgency level: ${result.urgencyLevel}. " +
                    "Please advise me on what I should do next."
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("SYMPTOM_QUERY", symptomQuery)
            }
            startActivity(intent)
        }
    }

    private data class UrgencyStyle(
        val emoji: String,
        val label: String,
        val bgColor: Int,
        val cardTint: Int
    )
}
