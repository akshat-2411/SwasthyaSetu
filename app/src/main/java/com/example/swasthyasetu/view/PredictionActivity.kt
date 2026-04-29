package com.example.swasthyasetu.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import com.example.swasthyasetu.R
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swasthyasetu.databinding.ActivityPredictionBinding
import com.example.swasthyasetu.util.DiseaseRisk
import com.example.swasthyasetu.util.RiskLevel
import com.example.swasthyasetu.viewmodel.SymptomViewModel

class PredictionActivity : BaseActivity() {

    private lateinit var binding: ActivityPredictionBinding
    private lateinit var adapter: PredictionAdapter
    private val viewModel: SymptomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPredictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val symptoms = intent.getStringArrayListExtra("SELECTED_SYMPTOMS") ?: arrayListOf()

        setupObservers()

        viewModel.computePredictions(symptoms)

        binding.tvActionMessage.visibility = View.GONE
        binding.btnPrimaryAction.visibility = View.GONE
    }

    private fun setupObservers() {
        viewModel.predictedRisks.observe(this) { risks ->
            if (risks.isNullOrEmpty()) {
                binding.cardTopRisk.visibility = View.GONE
                binding.tvSecondaryRisksLabel.visibility = View.GONE
                binding.rvSecondaryRisks.visibility = View.GONE
                binding.cardSafe.visibility = View.VISIBLE
            } else {
                binding.cardSafe.visibility = View.GONE

                val primaryRisk = risks[0]
                binding.cardTopRisk.visibility = View.VISIBLE
                binding.tvTopRiskName.text = "${primaryRisk.name.uppercase()} RISK"

                animateGauge(primaryRisk.probability)
                updateUiBasedOnRisk(primaryRisk)

                val secondaryRisks = if (risks.size > 1) risks.subList(1, risks.size) else emptyList()
                if (secondaryRisks.isNotEmpty()) {
                    binding.tvSecondaryRisksLabel.visibility = View.VISIBLE
                    binding.rvSecondaryRisks.visibility = View.VISIBLE
                    adapter = PredictionAdapter(secondaryRisks)
                    binding.rvSecondaryRisks.layoutManager = LinearLayoutManager(this)
                    binding.rvSecondaryRisks.adapter = adapter
                } else {
                    binding.tvSecondaryRisksLabel.visibility = View.GONE
                    binding.rvSecondaryRisks.visibility = View.GONE
                }
            }
        }

        viewModel.isLoadingAiContext.observe(this) { isLoading ->
            if (isLoading) {
                binding.cardAiContext.visibility = View.VISIBLE
                binding.shimmerAiLoading.visibility = View.VISIBLE
                binding.tvAiContextContent.visibility = View.GONE
            }
        }

        viewModel.aiPredictionContext.observe(this) { contextText ->
            binding.shimmerAiLoading.visibility = View.GONE
            binding.tvAiContextContent.visibility = View.VISIBLE
            binding.tvAiContextContent.text = contextText
        }
    }

    private fun animateGauge(targetScore: Int) {
        val animator = ValueAnimator.ofInt(0, targetScore)
        animator.duration = 1200
        animator.addUpdateListener { animation ->
            val currentValue = animation.animatedValue as Int
            binding.progressTopRisk.progress = currentValue
            binding.tvTopRiskScore.text = "$currentValue%"
        }
        animator.start()
    }

    private fun updateUiBasedOnRisk(highestRisk: DiseaseRisk) {
        binding.chipRiskLevel.text = highestRisk.riskLevel.name
        binding.tvActionMessage.visibility = View.VISIBLE
        binding.btnPrimaryAction.visibility = View.VISIBLE

        when (highestRisk.riskLevel) {
            RiskLevel.HIGH -> {
                binding.progressTopRisk.setIndicatorColor(Color.parseColor("#D32F2F"))
                binding.chipRiskLevel.setChipBackgroundColorResource(R.color.accent_emergency)

                binding.tvActionMessage.text = "This risk indicates you may require urgent attention."
                binding.tvActionMessage.setTextColor(Color.parseColor("#D32F2F"))

                binding.btnPrimaryAction.text = "EMERGENCY: See Doctor"
                binding.btnPrimaryAction.setBackgroundColor(Color.parseColor("#D32F2F"))
                binding.btnPrimaryAction.setOnClickListener {
                    startActivity(Intent(this, MapActivity::class.java))
                }
                triggerSubtleVibration()
            }
            RiskLevel.MEDIUM -> {
                binding.progressTopRisk.setIndicatorColor(Color.parseColor("#FF9800"))
                binding.chipRiskLevel.setChipBackgroundColorResource(R.color.warning_amber)

                binding.tvActionMessage.text = "Monitor your symptoms closely over the next 24 hours."
                binding.tvActionMessage.setTextColor(Color.parseColor("#FF9800"))

                binding.btnPrimaryAction.text = "Symptom Guidelines"
                binding.btnPrimaryAction.setBackgroundColor(Color.parseColor("#FF9800"))
                binding.btnPrimaryAction.setOnClickListener {
                    finish()
                }
            }
            RiskLevel.LOW -> {
                binding.progressTopRisk.setIndicatorColor(Color.parseColor("#388E3C"))
                binding.chipRiskLevel.setChipBackgroundColorResource(R.color.success_green)

                binding.tvActionMessage.text = "Don't panic! Based on AI analysis, this is likely manageable at home."
                binding.tvActionMessage.setTextColor(Color.parseColor("#388E3C"))

                binding.btnPrimaryAction.text = "Home Care Tips"
                binding.btnPrimaryAction.setBackgroundColor(Color.parseColor("#388E3C"))
                binding.btnPrimaryAction.setOnClickListener {
                    startActivity(Intent(this, ResultActivity::class.java))
                }
            }
        }
    }

    private fun triggerSubtleVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        }
    }
}
